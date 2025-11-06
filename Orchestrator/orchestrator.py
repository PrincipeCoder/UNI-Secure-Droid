# /Orchestrator/orchestrator.py

from celery import Celery, chain
from typing import Dict
import os

CELERY_BROKER_URL = os.getenv("CELERY_BROKER_URL", "amqp://guest@localhost//")
app = Celery('Orchestrator', broker=CELERY_BROKER_URL)

@app.task(name='tasks.orchestrate_analysis')
def orchestrate_analysis(job_id: str, apk_path: str) -> Dict:
    """
    RNF-11: Orquestador principal - único punto de entrada autorizado.
    Coordina análisis estático y dinámico en secuencia.
    """
    print(f"[{job_id}] Iniciando orquestación de análisis completo")
    
    # Paso 1: Análisis estático
    from StaticAnalyzer.analyzer import analyze_static
    static_result = analyze_static.apply_async(
        args=[job_id, apk_path],
        queue='static_analysis'
    ).get(timeout=120)
    
    if static_result.get("status") != "success":
        return {"job_id": job_id, "status": "error", "message": "Análisis estático falló"}
    
    # Extraer package_name del análisis estático
    package_name = static_result.get("features", {}).get("package_name")
    
    if not package_name:
        return {"job_id": job_id, "status": "error", "message": "No se pudo extraer package_name"}
    
    # Paso 2: Análisis dinámico (RNF-11: solo el orquestador puede invocarlo)
    from DynamicAnalyzer.analyzer import analyze_dynamic
    dynamic_result = analyze_dynamic.apply_async(
        args=[job_id, apk_path, package_name],
        queue='dynamic_analysis'
    ).get(timeout=120)
    
    # Combinar resultados
    combined_features = {
        "static_features": static_result.get("features", {}),
        "dynamic_features": dynamic_result.get("features", {})
    }
    
    print(f"[{job_id}] Orquestación completada")
    return {
        "job_id": job_id,
        "status": "success",
        "features": combined_features
    }
