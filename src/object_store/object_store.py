
"""
ObjectStore - Local backend 
Endpoints:
  POST /upload       -> multipart/form-data: job_id, file
  POST /upload_json  -> JSON: {"job_id":"abc123","file_base64":"...","filename":"optional.apk"}

Returns:
  200 -> {"object_path": "object://<bucket>/<prefix>/<filename>"}
  4xx/5xx -> {"error": "..."}
"""

import os
import io
import base64
import time
import logging
import hashlib
from typing import Optional, Dict
from flask import Flask, request, jsonify
from werkzeug.utils import secure_filename

# Try optional import for AES-GCM encryption
try:
    from cryptography.hazmat.primitives.ciphers.aead import AESGCM
except Exception:
    AESGCM = None

# ------- Configuration (env or defaults) -------
OBJECTSTORE_BUCKET = os.environ.get("OBJECTSTORE_BUCKET", "local-bucket")   # logical namespace
OBJECTSTORE_PREFIX = os.environ.get("OBJECTSTORE_PREFIX", "apk")
STORAGE_DIR = os.environ.get("STORAGE_DIR", "./objectstore")
MAX_UPLOAD_RETRIES = int(os.environ.get("MAX_UPLOAD_RETRIES", "2"))  # number of retries on failure
ENCRYPTION_KEY_B64 = os.environ.get("ENCRYPTION_KEY_BASE64", None)   # optional base64 key (16/24/32 bytes)
PORT = int(os.environ.get("PORT", "5002"))

# ------- Logging -------
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("objectstore_local_oop")

# ------- Utilities -------
def ensure_dir(path: str):
    os.makedirs(path, exist_ok=True)

def sha256_bytes(b: bytes) -> str:
    return hashlib.sha256(b).hexdigest()

# ------- Encryption helper class -------
class Encryptor:
    def __init__(self, key_b64: Optional[str]):
        if key_b64:
            if AESGCM is None:
                raise RuntimeError("cryptography package required for encryption but it's not installed.")
            key = base64.b64decode(key_b64)
            if len(key) not in (16, 24, 32):
                raise ValueError("ENCRYPTION_KEY_BASE64 must decode to 16, 24 or 32 bytes.")
            self._aesgcm = AESGCM(key)
            logger.info("Encryptor: AES-GCM enabled.")
        else:
            self._aesgcm = None
            logger.info("Encryptor: disabled (no key provided).")

    def encrypt(self, plain: bytes) -> bytes:
        if not self._aesgcm:
            return plain
        nonce = os.urandom(12)
        ct = self._aesgcm.encrypt(nonce, plain, None)
        return nonce + ct

    def decrypt(self, data: bytes) -> bytes:
        if not self._aesgcm:
            return data
        nonce = data[:12]
        ct = data[12:]
        return self._aesgcm.decrypt(nonce, ct, None)

# ------- Backend: Local filesystem -------
class LocalBackend:
    def __init__(self, storage_dir: str):
        self.storage_dir = storage_dir
        ensure_dir(self.storage_dir)
        logger.info("LocalBackend: storage_dir=%s", self.storage_dir)

    def _path_for(self, bucket: str, prefix: str, filename: str) -> str:
        # normalized path: <storage_dir>/<bucket>/<prefix>/<filename>
        return os.path.join(self.storage_dir, bucket, prefix, filename)

    def save(self, bucket: str, prefix: str, filename: str, data: bytes) -> str:
        dest_dir = os.path.dirname(self._path_for(bucket, prefix, filename))
        ensure_dir(dest_dir)
        dest_path = self._path_for(bucket, prefix, filename)
        # write atomically: write to temp file then rename
        tmp_path = dest_path + ".tmp"
        with open(tmp_path, "wb") as f:
            f.write(data)
            f.flush()
            os.fsync(f.fileno())
        os.replace(tmp_path, dest_path)
        logger.info("LocalBackend: saved %s", dest_path)
        return dest_path

# ------- ObjectStore  -------
class ObjectStore:
    def __init__(self, backend: LocalBackend, encryptor: Encryptor, bucket: str, prefix: str, max_retries: int = 2):
        self.backend = backend
        self.encryptor = encryptor
        self.bucket = bucket
        self.prefix = prefix
        self.max_retries = max_retries

    def build_object_path(self, filename: str) -> str:
        return f"object://{self.bucket}/{self.prefix}/{filename}"

    def upload(self, job_id: str, filename: str, content: bytes) -> Dict[str, str]:
        """
        Attempts to store content; returns dict with object_path and local_path.
        Retries on exception up to self.max_retries.
        """
        # sanitize filename and compose final filename using job_id to ensure uniqueness
        filename = secure_filename(filename)
        if "." not in filename:
            _, ext = os.path.splitext(filename)
            if not ext:
                filename = f"{job_id}.apk"
        # prefer job_id-based filename (preserve extension)
        _, ext = os.path.splitext(filename)
        final_filename = f"{job_id}{ext or '.apk'}"

        payload = self.encryptor.encrypt(content) if self.encryptor else content

        attempt = 0
        last_exc = None
        while attempt <= self.max_retries:
            try:
                local_path = self.backend.save(self.bucket, self.prefix, final_filename, payload)
                object_path = self.build_object_path(final_filename)
                file_hash = sha256_bytes(content)  # return hash of original (plaintext) bytes
                return {"object_path": object_path, "local_path": local_path, "sha256": file_hash}
            except Exception as e:
                logger.warning("Upload attempt %d failed: %s", attempt + 1, str(e))
                last_exc = e
                attempt += 1
                time.sleep(0.1 * attempt)
        logger.error("Upload failed after %d attempts", attempt)
        raise last_exc

# ------- Flask App wiring -------
app = Flask(__name__)

# instantiate components
_encryptor = None
try:
    _encryptor = Encryptor(ENCRYPTION_KEY_B64)
except Exception as ex:
    logger.error("Failed to init encryptor: %s. Encryption disabled.", ex)
    _encryptor = Encryptor(None)  # fallback: no encryption

_backend = LocalBackend(STORAGE_DIR)
_store = ObjectStore(_backend, _encryptor, OBJECTSTORE_BUCKET, OBJECTSTORE_PREFIX, max_retries=MAX_UPLOAD_RETRIES)

# Routes
@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok", "backend": "local", "storage_dir": STORAGE_DIR}), 200

@app.route("/upload", methods=["POST"])
def upload_multipart():
    job_id = request.form.get("job_id") or request.args.get("job_id")
    if not job_id:
        return jsonify({"error": "missing job_id"}), 400
    if "file" not in request.files:
        return jsonify({"error": "no file part"}), 400

    file = request.files["file"]
    if file.filename == "":
        return jsonify({"error": "empty filename"}), 400

    filename = secure_filename(file.filename)
    content = file.read()
    try:
        info = _store.upload(job_id, filename, content)
        # Return DTO as requested
        return jsonify({"object_path": info["object_path"]}), 200
    except Exception as e:
        logger.exception("Upload failed")
        return jsonify({"error": "upload failed", "detail": str(e)}), 500

@app.route("/upload_json", methods=["POST"])
def upload_json_base64():
    payload = request.get_json(silent=True)
    if not payload:
        return jsonify({"error": "invalid json"}), 400
    job_id = payload.get("job_id")
    b64 = payload.get("file_base64")
    filename = payload.get("filename", f"{job_id}.apk")
    if not job_id or not b64:
        return jsonify({"error": "job_id and file_base64 required"}), 400
    try:
        content = base64.b64decode(b64)
    except Exception:
        return jsonify({"error": "file_base64 not valid base64"}), 400

    try:
        info = _store.upload(job_id, filename, content)
        return jsonify({"object_path": info["object_path"]}), 200
    except Exception as e:
        logger.exception("Upload (json) failed")
        return jsonify({"error": "upload failed", "detail": str(e)}), 500

# Run
if __name__ == "__main__":
    ensure_dir(STORAGE_DIR)
    logger.info("Starting ObjectStore (local, POO) on port %d", PORT)
    app.run(host="0.0.0.0", port=PORT, debug=False)
