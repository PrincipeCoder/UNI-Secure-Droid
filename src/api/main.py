from fastapi import FastAPI, File, UploadFile, HTTPException, BackgroundTasks
from fastapi.responses import JSONResponse, FileResponse
from fastapi.middleware.cors import CORSMiddleware
import sys
import os
import uuid
import hashlib
from datetime import datetime

sys.path.append(os.path.join(os.path.dirname(__file__), '..'))
sys.path.append(os.path.join(os.path.dirname(__file__), '../..'))

from upload_service.upload_service import UploadService
from services.report_service import ReportService
from job_queue.job_queue import JobQueue
from object_store.object_store import ObjectStore
from api.database import SimpleDatabase

app = FastAPI(title="UNI-Secure-Droid API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

upload_service = UploadService(max_size_mb=50)
object_store = ObjectStore()
job_queue = JobQueue()
db = SimpleDatabase()
report_service = ReportService(metadata_db=None, object_store=object_store)

APK_STORAGE = "/tmp/apk_storage"
os.makedirs(APK_STORAGE, exist_ok=True)

# Intentar importar Orchestrator
try:
    from Orchestrator.orchestrator import orchestrate_analysis
    CELERY_AVAILABLE = True
except ImportError:
    CELERY_AVAILABLE = False
    print("WARNING: Orchestrator no disponible, usando modo simulado")

# Intentar importar StaticAnalyzer
try:
    from StaticAnalyzer.analyzer import analyze_apk_task
    STATIC_ANALYZER_AVAILABLE = True
except ImportError:
    STATIC_ANALYZER_AVAILABLE = False
    print("WARNING: StaticAnalyzer no disponible")

@app.get("/")
def root():
    return {"message": "UNI-Secure-Droid API", "version": "1.0.0"}

def run_analysis(job_id: str, apk_path: str):
    """Ejecuta el análisis en background"""
    try:
        db.update_status(job_id, "analyzing")
        
        if CELERY_AVAILABLE:
            # Usar Orchestrator real (análisis completo)
            result = orchestrate_analysis.delay(job_id=job_id, apk_path=apk_path)
            db.update_status(job_id, "analyzing")
        elif STATIC_ANALYZER_AVAILABLE:
            # Solo análisis estático
            result = analyze_apk_task.delay(apk_path)
            db.update_status(job_id, "analyzing")
        else:
            # Modo simulado
            import time
            time.sleep(2)
            db.update_status(job_id, "completed")
    except Exception as e:
        db.update_status(job_id, "failed")
        print(f"Error en análisis: {e}")

@app.post("/api/upload")
async def upload_apk(file: UploadFile = File(...), background_tasks: BackgroundTasks = None):
    if not file.filename.endswith(".apk"):
        raise HTTPException(status_code=400, detail="Solo se permiten archivos .apk")
    
    job_id = str(uuid.uuid4())
    apk_path = os.path.join(APK_STORAGE, f"{job_id}.apk")
    
    try:
        content = await file.read()
        
        if len(content) > 50 * 1024 * 1024:
            raise HTTPException(status_code=413, detail="Archivo demasiado grande (>50MB)")
        
        with open(apk_path, "wb") as f:
            f.write(content)
        
        sha256 = hashlib.sha256(content).hexdigest()
        
        # Guardar en BD
        db.create_job(job_id, apk_path, sha256)
        
        # Iniciar análisis en background
        if background_tasks:
            background_tasks.add_task(run_analysis, job_id, apk_path)
        
        return JSONResponse(
            status_code=200,
            content={
                "job_id": job_id,
                "hash": sha256,
                "status": "queued",
                "message": "APK recibido, análisis en cola"
            }
        )
    
    except Exception as e:
        if os.path.exists(apk_path):
            os.remove(apk_path)
        raise HTTPException(status_code=500, detail=f"Error al procesar archivo: {str(e)}")

@app.get("/api/status/{job_id}")
def get_job_status(job_id: str):
    job = db.get_job(job_id)
    
    if not job:
        raise HTTPException(status_code=404, detail="Job no encontrado")
    
    status = job.get("status", "unknown")
    progress = 100 if status == "completed" else (50 if status == "analyzing" else 0)
    
    return JSONResponse(
        status_code=200,
        content={
            "job_id": job_id,
            "status": status,
            "progress": progress
        }
    )

@app.get("/api/report/{job_id}")
def get_report(job_id: str):
    job = db.get_job(job_id)
    
    if not job:
        raise HTTPException(status_code=404, detail="Job no encontrado")
    
    if job["status"] != "completed":
        raise HTTPException(status_code=202, detail=f"Análisis en progreso: {job['status']}")
    
    # Si hay resultado real, usarlo
    if job.get("result"):
        return JSONResponse(status_code=200, content=job["result"])
    
    # Fallback: datos simulados
    report_data = {
        "job_id": job_id,
        "sha256": job.get("sha256", "unknown"),
        "verdict": "MALICIOSO",
        "risk": "Alto",
        "family": "Trojan.Android.Generic",
        "ai_probability": 0.95,
        "static_analysis": {
            "permissions": [
                "android.permission.INTERNET",
                "android.permission.READ_SMS",
                "android.permission.SEND_SMS",
                "android.permission.ACCESS_FINE_LOCATION"
            ],
            "urls": [
                "http://malicious-server.com/exfil",
                "192.168.1.100"
            ],
            "apis_detected": [
                "java.lang.reflect.Method.invoke",
                "android.telephony.SmsManager.sendTextMessage",
                "Runtime.exec"
            ],
            "top_signals": [
                "Reflection API detectada",
                "Envío de SMS sin consentimiento",
                "Ejecución de comandos del sistema",
                "Acceso a ubicación GPS",
                "Conexión a servidor sospechoso"
            ]
        },
        "dynamic_features": {
            "network": {
                "connections": [
                    "TCP:192.168.1.100:8080",
                    "HTTP:malicious-server.com:80"
                ],
                "dns_queries": ["malicious-server.com"],
                "data_sent_kb": 15.3
            },
            "file_operations": [
                {"action": "write", "path": "/sdcard/stolen_data.txt"},
                {"action": "read", "path": "/data/data/com.android.providers.contacts/databases/contacts2.db"}
            ],
            "syscalls": [
                "execve(/system/bin/sh)",
                "socket(AF_INET, SOCK_STREAM)"
            ]
        },
        "timestamp": datetime.now().isoformat(),
        "analysis_time_seconds": 75
    }
    
    return JSONResponse(status_code=200, content=report_data)

@app.get("/api/report/{job_id}/pdf")
def download_pdf_report(job_id: str):
    pdf_path = f"reports/report_{job_id}.pdf"
    
    if not os.path.exists(pdf_path):
        raise HTTPException(status_code=404, detail="Reporte PDF no encontrado")
    
    return FileResponse(
        pdf_path,
        media_type="application/pdf",
        filename=f"report_{job_id}.pdf"
    )

@app.get("/health")
def health_check():
    return {"status": "healthy", "service": "UNI-Secure-Droid API"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
