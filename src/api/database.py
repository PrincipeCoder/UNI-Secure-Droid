import json
import os
from datetime import datetime

class SimpleDatabase:
    """Base de datos simple en memoria con persistencia en JSON"""
    
    def __init__(self, db_file="jobs_db.json"):
        self.db_file = db_file
        self.jobs = {}
        self.load()
    
    def load(self):
        if os.path.exists(self.db_file):
            with open(self.db_file, 'r') as f:
                self.jobs = json.load(f)
    
    def save(self):
        with open(self.db_file, 'w') as f:
            json.dump(self.jobs, f, indent=2)
    
    def create_job(self, job_id, apk_path, sha256):
        self.jobs[job_id] = {
            "job_id": job_id,
            "apk_path": apk_path,
            "sha256": sha256,
            "status": "queued",
            "created_at": datetime.now().isoformat(),
            "result": None
        }
        self.save()
    
    def update_status(self, job_id, status):
        if job_id in self.jobs:
            self.jobs[job_id]["status"] = status
            self.save()
    
    def save_result(self, job_id, result):
        if job_id in self.jobs:
            self.jobs[job_id]["result"] = result
            self.jobs[job_id]["status"] = "completed"
            self.save()
    
    def get_job(self, job_id):
        return self.jobs.get(job_id)
