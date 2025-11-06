# /DynamicAnalyzer/analyzer.py

import subprocess
import time
from celery import Celery
from typing import Dict
from config import CELERY_BROKER_URL, ADB_PATH, EMULATOR_PORT
from sandbox_manager import SandboxManager
from behavior_monitor import BehaviorMonitor
from interaction_simulator import InteractionSimulator

app = Celery('DynamicAnalyzer', broker=CELERY_BROKER_URL)

@app.task(name='tasks.analyze_dynamic')
def analyze_dynamic(job_id: str, apk_path: str, package_name: str) -> Dict:
    """
    Tarea principal de análisis dinámico.
    RNF-11: Solo el sistema de orquestación puede invocar esta tarea.
    """
    print(f"[{job_id}] Iniciando análisis dinámico para {package_name}")
    
    try:
        # RF-14: Configurar sandbox aislada
        with SandboxManager(job_id) as sandbox:
            
            # Instalar APK en el emulador
            if not _install_apk(job_id, apk_path):
                return {"job_id": job_id, "status": "error", "message": "Error instalando APK"}
            
            # Iniciar monitoreo
            monitor = BehaviorMonitor(job_id)
            simulator = InteractionSimulator(job_id, package_name)
            
            # RF-18: Simular interacción
            simulator.run_interaction_sequence()
            
            # Esperar y capturar comportamiento
            time.sleep(5)
            
            # RNF-5: Verificar timeout
            if sandbox.check_timeout():
                print(f"[{job_id}] Timeout alcanzado, abortando análisis")
                return {"job_id": job_id, "status": "timeout", "message": "Análisis excedió 90s"}
            
            # RF-15, RF-16, RF-17: Capturar comportamientos
            dynamic_features = monitor.collect_all()
            
            # RF-19: Abortar sandbox de forma segura
            # (el context manager se encarga automáticamente)
            
            print(f"[{job_id}] Análisis dinámico completado")
            return {
                "job_id": job_id,
                "status": "success",
                "features": dynamic_features
            }
            
    except Exception as e:
        print(f"[{job_id}] Error en análisis dinámico: {e}")
        return {"job_id": job_id, "status": "error", "message": str(e)}

def _install_apk(job_id: str, apk_path: str) -> bool:
    """Instala el APK en el emulador"""
    try:
        result = subprocess.run(
            [ADB_PATH, "-s", f"emulator-{EMULATOR_PORT}", "install", "-r", apk_path],
            capture_output=True,
            text=True,
            timeout=30
        )
        return "Success" in result.stdout
    except Exception as e:
        print(f"[{job_id}] Error instalando APK: {e}")
        return False
