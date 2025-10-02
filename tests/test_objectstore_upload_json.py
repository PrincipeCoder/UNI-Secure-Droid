import os, sys, base64, requests, tempfile, time
from urllib.parse import urlparse

BASE_URL = os.environ.get("OBJECTSTORE_URL", "http://localhost:5002")
ENDPOINT = f"{BASE_URL.rstrip('/')}/upload_json"

def main():
    job_id = "testobj_json_" + str(int(time.time()))
    # make small content
    content = b"apk-bytes-for-json-test-" + job_id.encode()
    b64 = base64.b64encode(content).decode("ascii")
    payload = {"job_id": job_id, "file_base64": b64, "filename": f"{job_id}.apk"}
    print("Posting JSON to", ENDPOINT)
    r = requests.post(ENDPOINT, json=payload, timeout=10)
    if r.status_code != 200:
        print("FAILED: status", r.status_code, r.text)
        sys.exit(2)
    body = r.json()
    if "object_path" not in body:
        print("FAILED: no object_path in response:", body)
        sys.exit(3)
    print("OK: object_path:", body["object_path"])
    # basic local check (optional)
    storage_dir = os.environ.get("STORAGE_DIR", "./objectstore")
    # parse object path
    obj = body["object_path"]
    if obj.startswith("object://"):
        rest = obj[len("object://"):]
        parts = rest.split("/", 2)
        bucket = parts[0]
        prefix = parts[1] if len(parts) > 2 else ""
        filename = "/".join(parts[2:]) if len(parts) > 2 else (parts[1] if len(parts)>1 else "")
        candidate = os.path.join(storage_dir, bucket, prefix, filename)
        time.sleep(0.15)
        if os.path.exists(candidate):
            print("Local file present:", candidate)
        else:
            print("Local file not present at expected path; server may store elsewhere.")
    else:
        print("object_path has unexpected format.")
    print("Test complete (upload_json).")
    sys.exit(0)

if __name__ == "__main__":
    main()
