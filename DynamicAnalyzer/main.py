# /DynamicAnalyzer/main.py

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from analyzer import analyze_dynamic

app = FastAPI(title="DynamicAnalyzer Service", description="API para análisis dinámico en sandbox")

# Simulación de DB compartida
JOBS_DB = {}

class AnalysisRequest(BaseModel):
    job_id: str
    apk_path: str
    package_name: str

@app.post("/analyze", status_code=202)
async def create_dynamic_analysis(request: AnalysisRequest):
    """
    RNF-11: Endpoint protegido - solo accesible por el orquestador.
    Encola una tarea de análisis dinámico.
    """
    job_id = request.job_id
    
    # Validación
    if not request.package_name:
        raise HTTPException(status_code=422, detail="package_name requerido")
    
    # Registrar trabajo
    JOBS_DB[job_id] = {"status": "pending", "type": "dynamic"}
    
    # Encolar tarea
    analyze_dynamic.delay(
        job_id=job_id,
        apk_path=request.apk_path,
        package_name=request.package_name
    )
    
    return {"job_id": job_id, "message": "Análisis dinámico aceptado"}

@app.get("/status/{job_id}")
async def get_status(job_id: str):
    """Consulta el estado de un análisis dinámico"""
    job = JOBS_DB.get(job_id)
    if not job:
        raise HTTPException(status_code=404, detail="Job no encontrado")
    return job
