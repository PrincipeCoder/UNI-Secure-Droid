import os, sys, time, requests

BASE_URL = os.environ.get("JOBQUEUE_URL", "http://localhost:5003")
ENQUEUE_URL = f"{BASE_URL.rstrip('/')}/enqueue"
METRICS_URL = f"{BASE_URL.rstrip('/')}/metrics"
HEALTH_URL = f"{BASE_URL.rstrip('/')}/health"

def get_metrics():
    try:
        r = requests.get(METRICS_URL, timeout=3)
        if r.status_code == 200:
            return r.json()
    except Exception as e:
        print("Error fetching metrics:", e)
    return None

def main():
    # check health
    try:
        r = requests.get(HEALTH_URL, timeout=2)
        if r.status_code != 200:
            print("JobQueue health endpoint not OK:", r.status_code, r.text)
    except Exception as e:
        print("Cannot contact JobQueue service at", BASE_URL, "-", e)
        sys.exit(2)

    # fetch baseline metrics
    base = get_metrics() or {}
    base_processed = int(base.get("jobs_processed", 0))
    base_errors = int(base.get("jobs_error", 0))
    print("Baseline jobs_processed:", base_processed, "jobs_error:", base_errors)

    job_id = "testjob_" + str(int(time.time()))
    payload = {"forward_to_dynamic": True, "score": 0.55, "simulated_latency": 0.5}
    data = {"job_id": job_id, "phase": "static", "payload": payload}
    print("Enqueuing job:", data)
    r = requests.post(ENQUEUE_URL, json=data, timeout=5)
    if r.status_code == 429:
        print("Queue saturated (429). Test cannot proceed.")
        sys.exit(3)
    if r.status_code != 200:
        print("Enqueue failed:", r.status_code, r.text)
        sys.exit(4)
    print("Enqueued OK:", r.json())

    # wait/poll for processing: up to 15s total
    deadline = time.time() + 15
    while time.time() < deadline:
        m = get_metrics()
        if m:
            processed = int(m.get("jobs_processed", 0))
            errors = int(m.get("jobs_error", 0))
            print("poll metrics -> processed:", processed, "errors:", errors)
            if processed >= base_processed + 1 and errors == base_errors:
                print("OK: job processed by worker(s).")
                sys.exit(0)
            if errors > base_errors:
                print("Worker(s) reported error. Test FAILED.")
                sys.exit(5)
        time.sleep(0.5)
    print("Timeout waiting for job to be processed. Test inconclusive/failed.")
    sys.exit(6)

if __name__ == "__main__":
    main()
