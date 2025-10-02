
"""
JobQueue service 
- Orquesta trabajos de análisis en segundo plano.
- Exposes POST /enqueue to accept jobs:
    Input DTO: {"job_id":"abc123","phase":"static"|"dynamic","payload": {...}}
    Output DTO: {"ack": true}
- Notifica estados a MetadataDB via stored function upsert_job_result(job_id, status, score,...)
- Retries to workers with backoff. Marks job error on repeated failure.
- If queue is saturated -> returns 429 Too Many Requests.

"""

import os
import time
import json
import logging
import threading
import queue
from dataclasses import dataclass, field
from typing import Any, Dict, Optional, Callable
from flask import Flask, request, jsonify
import requests

# Optional DB client import
try:
    import psycopg2
    import psycopg2.extras
except Exception:
    psycopg2 = None

# ---------- Configuration ----------
STATIC_QUEUE_MAXSIZE = int(os.environ.get("STATIC_QUEUE_MAXSIZE", "100"))
DYNAMIC_QUEUE_MAXSIZE = int(os.environ.get("DYNAMIC_QUEUE_MAXSIZE", "100"))
STATIC_WORKERS = int(os.environ.get("STATIC_WORKERS", "2"))
DYNAMIC_WORKERS = int(os.environ.get("DYNAMIC_WORKERS", "2"))
WORKER_HTTP_MODE = os.environ.get("WORKER_HTTP_MODE", "false").lower() in ("1","true","yes")
STATIC_WORKER_URL = os.environ.get("STATIC_WORKER_URL", "")
DYNAMIC_WORKER_URL = os.environ.get("DYNAMIC_WORKER_URL", "")
MAX_WORKER_RETRIES = int(os.environ.get("MAX_WORKER_RETRIES", "3"))
QUEUE_ENQUEUE_TIMEOUT_SEC = float(os.environ.get("QUEUE_ENQUEUE_TIMEOUT_SEC", "0.1"))
WORKER_HTTP_TIMEOUT = float(os.environ.get("WORKER_HTTP_TIMEOUT", "10.0"))
PORT = int(os.environ.get("PORT", "5003"))

# DB config
DB_HOST = os.environ.get("DB_HOST", "")
DB_PORT = int(os.environ.get("DB_PORT", "5432") or 5432)
DB_NAME = os.environ.get("DB_NAME", "")
DB_USER = os.environ.get("DB_USER", "")
DB_PASSWORD = os.environ.get("DB_PASSWORD", "")

# Logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("jobqueue")

# ---------- DTOs & Data classes ----------
@dataclass
class Job:
    job_id: str
    phase: str  # 'static' or 'dynamic'
    payload: Dict[str, Any] = field(default_factory=dict)
    enqueued_at: float = field(default_factory=time.time)

# ---------- Metrics collector (simple) ----------
class Metrics:
    def __init__(self):
        self.lock = threading.Lock()
        self.metrics = {
            "enqueue_count": 0,
            "enqueue_fail_queue_full": 0,
            "worker_timeouts": 0,
            "worker_retries": 0,
            "jobs_processed": 0,
            "jobs_error": 0,
        }

    def inc(self, key, n=1):
        with self.lock:
            self.metrics.setdefault(key, 0)
            self.metrics[key] += n

    def get_all(self):
        with self.lock:
            return dict(self.metrics)

metrics = Metrics()

# ---------- MetadataDB client ----------
class MetadataDBClient:

    def __init__(self, host, port, dbname, user, password):
        self.enabled = bool(host and dbname and user and psycopg2)
        self.conn = None
        if self.enabled:
            try:
                self.conn = psycopg2.connect(
                    host=host, port=port, dbname=dbname, user=user, password=password,
                    cursor_factory=psycopg2.extras.RealDictCursor, connect_timeout=5
                )
                self.conn.autocommit = True
                logger.info("MetadataDBClient: connected to DB %s@%s:%s", dbname, host, port)
            except Exception as e:
                logger.exception("MetadataDBClient: failed to connect to DB: %s", e)
                self.enabled = False

    def notify_status(self, job_id: str, status: str, score: Optional[float]=None,
                      processing_time_ms: Optional[int]=None, changed_by: str="JobQueue"):

        if not self.enabled:
            logger.info("MetadataDBClient (noop): job=%s status=%s score=%s", job_id, status, score)
            return True
        try:
            with self.conn.cursor() as cur:
                # use a safe call: SELECT upsert_job_result(%s,%s,%s,%s,%s)
                cur.execute("SELECT upsert_job_result(%s, %s::job_status, %s, %s, %s);",
                            (job_id, status, score, processing_time_ms, changed_by))
            logger.info("MetadataDBClient: notified job=%s status=%s", job_id, status)
            return True
        except Exception as e:
            logger.exception("MetadataDBClient: notify failed for job=%s status=%s: %s", job_id, status, e)
            return False

    def mark_job_error(self, job_id: str, err_msg: str, changed_by: str="JobQueue"):
        if not self.enabled:
            logger.info("MetadataDBClient (noop): mark error job=%s msg=%s", job_id, err_msg)
            return True
        try:
            with self.conn.cursor() as cur:
                cur.execute("SELECT mark_job_error(%s, %s, %s);", (job_id, err_msg, changed_by))
            logger.info("MetadataDBClient: marked job error %s", job_id)
            return True
        except Exception as e:
            logger.exception("MetadataDBClient: mark_job_error failed for job=%s: %s", job_id, e)
            return False

# ---------- Worker clients ----------
class HttpWorkerClient:

    def __init__(self, url: str, timeout: float = WORKER_HTTP_TIMEOUT):
        self.url = url
        self.timeout = timeout

    def process(self, job: Job) -> Dict[str, Any]:
        try:
            resp = requests.post(self.url, json={"job_id": job.job_id, "payload": job.payload}, timeout=self.timeout)
            resp.raise_for_status()
            return resp.json()
        except requests.Timeout:
            metrics.inc("worker_timeouts")
            logger.warning("HttpWorkerClient: timeout calling worker %s for job %s", self.url, job.job_id)
            raise
        except Exception as e:
            logger.exception("HttpWorkerClient: error calling worker %s: %s", self.url, e)
            raise

class LocalWorkerSimulator:

    def __init__(self, simulated_latency: float = 1.0):
        self.simulated_latency = simulated_latency

    def process(self, job: Job) -> Dict[str, Any]:
        # Simulate processing time
        t0 = time.time()
        # Payload can include 'simulate_fail': True to simulate a failure
        simulate_fail = bool(job.payload.get("simulate_fail"))
        simulate_timeout = bool(job.payload.get("simulate_timeout"))
        latency = float(job.payload.get("simulated_latency", self.simulated_latency))
        # emulate blocking work
        time.sleep(latency)
        processing_ms = int((time.time() - t0) * 1000)
        if simulate_timeout:
            metrics.inc("worker_timeouts")
            raise TimeoutError("Simulated worker timeout")
        if simulate_fail:
            # simulate an error response
            return {"status": "error", "score": None, "forward": False, "processing_time_ms": processing_ms}
        # success
        forward = bool(job.payload.get("forward_to_dynamic", False))
        score = float(job.payload.get("score", 0.0))
        return {"status": "done", "score": score, "forward": forward, "processing_time_ms": processing_ms}

# ---------- Worker thread ----------
class WorkerThread(threading.Thread):
    def __init__(self, name: str, job_queue: "JobQueue", phase: str, worker_client, max_retries: int = MAX_WORKER_RETRIES):
        super().__init__(daemon=True)
        self.name = name
        self.job_queue = job_queue
        self.phase = phase  # 'static' or 'dynamic'
        self.worker_client = worker_client
        self.max_retries = max_retries
        self._running = True

    def run(self):
        logger.info("WorkerThread %s started for phase=%s", self.name, self.phase)
        while self._running:
            try:
                job = self.job_queue.dequeue_for_worker(self.phase, timeout=1.0)
                if job is None:
                    continue
                logger.info("[%s] picked job=%s", self.name, job.job_id)
                # notify metadata DB that now processing this phase
                self.job_queue.db_client.notify_status(job.job_id, self.phase, changed_by="JobQueue")
                # process with retry/backoff
                attempt = 0
                while attempt <= self.max_retries:
                    try:
                        result = self.worker_client.process(job)
                        # result expected to be dict
                        status = result.get("status", "error")
                        score = result.get("score", None)
                        proc_ms = result.get("processing_time_ms", None)
                        if status == "done":
                            logger.info("[%s] job %s done (score=%s)", self.name, job.job_id, score)
                            # notify DB done for this phase (but final 'done' should be marked by pipeline owner)
                            # We notify the current phase as done first; if forwarded, we'll enqueue the next phase.
                            self.job_queue.db_client.notify_status(job.job_id, "done" if not result.get("forward", False) else self.phase, score=score, processing_time_ms=proc_ms, changed_by=self.name)
                            metrics.inc("jobs_processed")
                            # forward to dynamic if requested and we are in static
                            if self.phase == "static" and result.get("forward", False):
                                forwarded_job = Job(job.job_id, "dynamic", payload=job.payload)
                                enqueued = self.job_queue.enqueue(forwarded_job)
                                if not enqueued:
                                    # cannot enqueue forwarded job -> log + mark error
                                    logger.warning("[%s] failed to forward job %s to dynamic queue (saturated)", self.name, job.job_id)
                                    self.job_queue.db_client.mark_job_error(job.job_id, "failed to forward to dynamic: queue full", changed_by=self.name)
                            break  # success -> exit retry loop
                        else:
                            # worker returned explicit error
                            logger.warning("[%s] worker returned error for job %s", self.name, job.job_id)
                            attempt += 1
                            metrics.inc("worker_retries")
                            time.sleep(0.5 * (2 ** attempt))
                            continue
                    except requests.Timeout:
                        # HTTP timeout counts as a retry but record metric
                        metrics.inc("worker_retries")
                        logger.warning("[%s] worker timeout for job %s attempt %d", self.name, job.job_id, attempt+1)
                        attempt += 1
                        time.sleep(0.5 * (2 ** attempt))
                        continue
                    except Exception as e:
                        # other errors -> retry up to max_retries
                        metrics.inc("worker_retries")
                        logger.exception("[%s] error processing job %s (attempt %d): %s", self.name, job.job_id, attempt+1, e)
                        attempt += 1
                        time.sleep(0.5 * (2 ** attempt))
                        continue
                else:
                    # if we reach here, retries exhausted
                    logger.error("[%s] exhausted retries for job %s -> marking error", self.name, job.job_id)
                    metrics.inc("jobs_error")
                    self.job_queue.db_client.mark_job_error(job.job_id, f"worker failed after {self.max_retries} retries", changed_by=self.name)

            except Exception as e:
                logger.exception("WorkerThread %s unexpected error: %s", self.name, e)
                time.sleep(1.0)

    def stop(self):
        self._running = False

# ---------- JobQueue main class ----------
class JobQueue:
    def __init__(self,
                 static_maxsize=STATIC_QUEUE_MAXSIZE,
                 dynamic_maxsize=DYNAMIC_QUEUE_MAXSIZE,
                 static_workers=STATIC_WORKERS,
                 dynamic_workers=DYNAMIC_WORKERS,
                 worker_http_mode=WORKER_HTTP_MODE,
                 static_worker_url=STATIC_WORKER_URL,
                 dynamic_worker_url=DYNAMIC_WORKER_URL,
                 db_client: Optional[MetadataDBClient]=None):
        self.static_q = queue.Queue(maxsize=static_maxsize)
        self.dynamic_q = queue.Queue(maxsize=dynamic_maxsize)
        self.static_workers_count = static_workers
        self.dynamic_workers_count = dynamic_workers
        self.worker_http_mode = worker_http_mode
        self.static_worker_url = static_worker_url
        self.dynamic_worker_url = dynamic_worker_url
        self.db_client = db_client or MetadataDBClient(DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD)
        self._workers = []
        self._start_workers()

    def _start_workers(self):
        # create worker threads for static
        for i in range(self.static_workers_count):
            client = HttpWorkerClient(self.static_worker_url) if self.worker_http_mode and self.static_worker_url else LocalWorkerSimulator()
            w = WorkerThread(name=f"static-{i+1}", job_queue=self, phase="static", worker_client=client)
            w.start()
            self._workers.append(w)
        # dynamic
        for i in range(self.dynamic_workers_count):
            client = HttpWorkerClient(self.dynamic_worker_url) if self.worker_http_mode and self.dynamic_worker_url else LocalWorkerSimulator()
            w = WorkerThread(name=f"dynamic-{i+1}", job_queue=self, phase="dynamic", worker_client=client)
            w.start()
            self._workers.append(w)
        logger.info("JobQueue: started %d static and %d dynamic workers", self.static_workers_count, self.dynamic_workers_count)

    def enqueue(self, job: Job) -> bool:

        target_q = self.static_q if job.phase == "static" else self.dynamic_q
        try:
            # non-blocking with small timeout to allow quick 429 response if saturated
            target_q.put(job, timeout=QUEUE_ENQUEUE_TIMEOUT_SEC)
            metrics.inc("enqueue_count")
            # notify DB that job is queued
            self.db_client.notify_status(job.job_id, "queued", changed_by="JobQueue")
            logger.info("Enqueued job=%s phase=%s", job.job_id, job.phase)
            return True
        except queue.Full:
            metrics.inc("enqueue_fail_queue_full")
            logger.warning("Failed to enqueue job=%s phase=%s: queue full", job.job_id, job.phase)
            return False

    def dequeue_for_worker(self, phase: str, timeout: Optional[float]=None) -> Optional[Job]:
        try:
            if phase == "static":
                return self.static_q.get(timeout=timeout)
            else:
                return self.dynamic_q.get(timeout=timeout)
        except queue.Empty:
            return None

    def stop_all(self):
        for w in self._workers:
            w.stop()

# ---------- Flask app (API) ----------
app = Flask(__name__)
_jobqueue = JobQueue()

@app.route("/enqueue", methods=["POST"])
def api_enqueue():

    data = request.get_json(silent=True)
    if not data:
        return jsonify({"error": "invalid json"}), 400
    job_id = data.get("job_id")
    phase = data.get("phase")
    payload = data.get("payload", {})

    if not job_id or phase not in ("static", "dynamic"):
        return jsonify({"error": "job_id and phase ('static'|'dynamic') required"}), 400

    job = Job(job_id=job_id, phase=phase, payload=payload)
    ok = _jobqueue.enqueue(job)
    if not ok:
        # return 429 Too Many Requests
        return jsonify({"error": "queue saturated, try again later"}), 429
    return jsonify({"ack": True}), 200

@app.route("/metrics", methods=["GET"])
def api_metrics():
    return jsonify(metrics.get_all()), 200

@app.route("/health", methods=["GET"])
def api_health():
    return jsonify({"status": "ok", "static_workers": STATIC_WORKERS, "dynamic_workers": DYNAMIC_WORKERS}), 200

# Graceful shutdown helper
def shutdown(signum=None, frame=None):
    logger.info("Shutting down JobQueue service...")
    _jobqueue.stop_all()
    # allow threads to exit
    time.sleep(1.0)
    logger.info("Shutdown complete.")

# ---------- Run (CLI) ----------
if __name__ == "__main__":
    try:
        logger.info("Starting JobQueue service on port %d", PORT)
        app.run(host="0.0.0.0", port=PORT)
    except KeyboardInterrupt:
        shutdown()
