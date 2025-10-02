# /StaticAnalyzer/analyzer.py

import logging
from typing import List, Set, Tuple
from celery import Celery
from androguard.core.androconf import show_logging as androguard_logging
from androguard.core.bytecodes.apk import APK
from androguard.core.bytecodes.dvm import DalvikVMFormat
from androguard.core.analysis.analysis import Analysis

# Importamos nuestras configuraciones y utilidades
from config import CELERY_BROKER_URL, ANALYSIS_TIMEOUT_SECONDS
from utils.timeout import Timeout, TimeoutException
from utils.error_handling import InvalidAPKError

# Importamos la base de datos simulada desde el módulo de estado compartido
# para actualizar el estado de los trabajos.
from shared_state import JOBS_DB

# --- Configuración Inicial ---

# Silenciamos el logging de Androguard para que no sea tan verboso
androguard_logging(level=logging.ERROR)

# 1. Creamos la instancia de la aplicación Celery.
# El primer argumento es el nombre del módulo actual.
# El argumento 'broker' le dice a Celery dónde está nuestro RabbitMQ.
app = Celery('StaticAnalyzer', broker=CELERY_BROKER_URL)

# --- Tarea Principal de Celery ---

@app.task(name='tasks.analyze_static')
def analyze_static(job_id: str, object_path: str):
    """
    Tarea de Celery que realiza el análisis estático de un APK.
    Esta función se ejecuta en un proceso 'worker' separado de la API.
    """
    print(f"WORKER: [{job_id}] Recibido. Analizando archivo en: {object_path}")
    
    # Actualizamos el estado en nuestra DB simulada
    JOBS_DB[job_id]["status"] = "processing"

    try:
        # 2. Envolvemos toda la lógica de análisis en nuestro gestor de Timeout.
        with Timeout(seconds=ANALYSIS_TIMEOUT_SECONDS):
            
            # 3. Usamos Androguard para analizar el archivo.
            a, d, dx = _perform_analysis(object_path)
            
            # 4. Extraemos las características que nos interesan.
            static_features = {
                "package_name": a.get_package(),
                "main_activity": a.get_main_activity(),
                "permissions": sorted(a.get_permissions()),
                "api_calls": _extract_api_calls(dx),
                "urls": _extract_urls(dx),
            }

        # Si todo va bien, el trabajo está completo.
        print(f"WORKER: [{job_id}] Análisis completado exitosamente.")
        JOBS_DB[job_id]["status"] = "completed"
        JOBS_DB[job_id]["features"] = static_features
        
        # 5. Entregamos el resultado al siguiente componente (FeatureBuilder).
        # Por ahora, simplemente lo imprimimos en la consola del worker.
        print(f"WORKER: [{job_id}] Enviando al FeatureBuilder: {static_features}")
        
        return {"job_id": job_id, "status": "success", "features": static_features}

    except TimeoutException:
        error_msg = f"Timeout > {ANALYSIS_TIMEOUT_SECONDS}s. Análisis abortado."
        print(f"WORKER: ERROR [{job_id}] - {error_msg}")
        JOBS_DB[job_id]["status"] = "error"
        JOBS_DB[job_id]["error_details"] = error_msg
        return {"job_id": job_id, "status": "error", "message": error_msg}
    
    except InvalidAPKError as e:
        error_msg = f"Archivo APK corrupto o inválido: {e}"
        print(f"WORKER: ERROR [{job_id}] - {error_msg}")
        JOBS_DB[job_id]["status"] = "error"
        JOBS_DB[job_id]["error_details"] = error_msg
        return {"job_id": job_id, "status": "error", "message": error_msg}

    except Exception as e:
        # Capturamos cualquier otro error inesperado (fallo en descompilación, etc.)
        error_msg = f"Fallo inesperado durante el análisis: {e}"
        print(f"WORKER: ERROR [{job_id}] - {error_msg}")
        JOBS_DB[job_id]["status"] = "error"
        JOBS_DB[job_id]["error_details"] = error_msg
        return {"job_id": job_id, "status": "error", "message": error_msg}

# --- Funciones Auxiliares para el Análisis ---

def _perform_analysis(apk_path: str) -> tuple[APK, list[DalvikVMFormat], Analysis]:
    """Abre y analiza el APK con Androguard, devolviendo los objetos de análisis."""
    try:
        a = APK(apk_path)
        d = [DalvikVMFormat(dex) for dex in a.get_all_dex()]
        dx = Analysis(d)
        return a, d, dx
    except Exception as e:
        # Si Androguard no puede procesar el archivo, lanzamos nuestro error personalizado.
        raise InvalidAPKError(str(e))

def _extract_api_calls(dx: Analysis, limit: int = 200) -> List[str]:
    """Extrae llamadas a APIs externas (ej. Landroid/telephony/TelephonyManager;->getLine1Number)."""
    api_calls = {
        f"{method.get_class_name()}->{method.get_name()}"
        for method in dx.get_methods() if method.is_external()
    }
    return sorted(list(api_calls))[:limit]

def _extract_urls(dx: Analysis, limit: int = 100) -> List[str]:
    """Encuentra cadenas que parecen URLs dentro del código de la aplicación."""
    urls = {
        url.get_value().decode('utf-8', 'ignore')
        for string_analysis in dx.get_strings_analysis().values()
        for url in string_analysis.get_urls()
    }
    return sorted(list(urls))[:limit]
