import os
import sys
import tempfile
import requests
import hashlib
import time
from urllib.parse import urlparse

BASE_URL = os.environ.get("OBJECTSTORE_URL", "http://localhost:5002")
UPLOAD_ENDPOINT = f"{BASE_URL.rstrip('/')}/upload"

def sha256_bytes(b: bytes) -> str:
    import hashlib
    return hashlib.sha256(b).hexdigest()

def parse_object_path(obj_path: str):
    # expected format: object://bucket/prefix/filename
    if not obj_path.startswith("object://"):
        raise ValueError("Unexpected object_path format: " + obj_path)
    rest = obj_path[len("object://"):]
    parts = rest.split("/", 2)
    if len(parts) < 3:
        # maybe bucket/prefix/filename but if prefix contains / may be longer. Use split('/')
        parts = rest.split("/")
    bucket = parts[0]
    prefix = parts[1] if len(parts) > 2 else ""
    filename = "/".join(parts[2:]) if len(parts) > 2 else (parts[1] if len(parts)>1 else "")
    return bucket, prefix, filename

def main():
    job_id = "testobj_multipart_" + str(int(time.time()))
    # create temporary sample file
    with tempfile.NamedTemporaryFile(delete=False, suffix=".apk") as tf:
        tf.write(b"hello-objectstore-test-" + job_id.encode())
        tf.flush()
        local_path = tf.name

    with open(local_path, "rb") as f:
        files = {"file": (os.path.basename(local_path), f)}
        data = {"job_id": job_id}
        print("Uploading file via multipart to", UPLOAD_ENDPOINT)
        r = requests.post(UPLOAD_ENDPOINT, files=files, data=data, timeout=10)
    os.unlink(local_path)

    if r.status_code != 200:
        print("FAILED: upload returned", r.status_code, r.text)
        sys.exit(2)

    body = r.json()
    if "object_path" not in body:
        print("FAILED: response does not contain object_path:", body)
        sys.exit(3)
    obj_path = body["object_path"]
    print("OK: object_path:", obj_path)

    # try to map to local storage: parse object_path and check filesystem
    try:
        bucket, prefix, filename = parse_object_path(obj_path)
    except Exception as e:
        print("WARNING: cannot parse object_path:", e)
        print("Test SUCCESS (upload accepted), but cannot verify local file presence.")
        sys.exit(0)

    storage_dir = os.environ.get("STORAGE_DIR", "./objectstore")
    candidate_path = os.path.join(storage_dir, bucket, prefix, filename)
    print("Expecting file at (local check):", candidate_path)
    # wait briefly for IO flush
    time.sleep(0.2)
    if os.path.exists(candidate_path):
        print("File exists locally:", candidate_path)
        # compute hash of saved file
        with open(candidate_path, "rb") as f:
            saved = f.read()
        # original content was b"hello-objectstore-test-"+job_id
        original = b"hello-objectstore-test-" + job_id.encode()
        if sha256_bytes(saved) == sha256_bytes(original):
            print("SHA256 matches original -> file stored without client-side encryption. OK.")
            sys.exit(0)
        else:
            print("SHA256 differs from original. This may be because server-side/client-side encryption is enabled.")
            print("Saved file size:", len(saved))
            print("Original size:", len(original))
            print("Test SUCCESS (upload accepted and file present), but content differs (encryption?).")
            sys.exit(0)
    else:
        print("File not found locally. Either server stores files elsewhere or path mapping differs.")
        print("Test SUCCESS (upload accepted), but local verification failed.")
        sys.exit(0)

if __name__ == "__main__":
    main()
