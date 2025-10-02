# /StaticAnalyzer/main.py

import uuid
import shutil
import os
from fastapi import FastAPI, UploadFile, File, HTTPException

# Importamos la tarea que crearemos en el siguiente paso.
# Es normal que tu editor de código marque un error aquí por ahora.
from analyzer import analyze_static 

# --- Configuración Inicial y Simulación ---

# 1. Creamos la aplicación FastAPI. El título aparecerá en la documentación automática.
app = FastAPI(title="StaticAnalyzer Service", description="API para el análisis estático de APKs.")

# 2. Simulación de un ObjectStore: usaremos un directorio local.
# En un sistema de producción, esto sería un bucket de S3, Google Cloud Storage, etc.
OBJECT_STORE_PATH = "/tmp/apk_storage/"
os.makedirs(OBJECT_STORE_PATH, exist_ok=True) # Nos aseguramos que el directorio exista

# 3. Importamos nuestra base de datos simulada desde el módulo de estado compartido.
from shared_state import JOBS_DB

# --- Endpoints de la API ---

@app.post("/analyze", status_code=202)
async def create_analysis_job(file: UploadFile = File(...)):
    """
    Recibe un archivo APK, lo guarda, encola una tarea de análisis y responde
    inmediatamente con un ID de trabajo.

    - **status_code=202 (Accepted):** Le decimos al cliente "Hemos aceptado tu
      petición, pero el procesamiento aún no ha terminado". Es el código HTTP
      correcto para operaciones asíncronas.
    """
    # 4. Validación básica del archivo.
    if not file.filename.endswith(".apk"):
        # Si no es un APK, respondemos con un error 422 (Unprocessable Entity).
        raise HTTPException(
            status_code=422, 
            detail="Tipo de archivo inválido. Solo se aceptan archivos .apk"
        )

    # 5. Generamos un ID de trabajo único.
    job_id = str(uuid.uuid4())
    
    # 6. Definimos la ruta donde se guardará el APK en nuestro ObjectStore simulado.
    apk_path = os.path.join(OBJECT_STORE_PATH, f"{job_id}.apk")

    # 7. Guardamos el archivo subido en el disco.
    try:
        with open(apk_path, "wb") as buffer:
            # shutil.copyfileobj es una forma eficiente de copiar el contenido del archivo.
            shutil.copyfileobj(file.file, buffer)
    finally:
        # Es una buena práctica cerrar siempre el archivo.
        file.file.close()

    # 8. Guardamos el estado inicial del trabajo en nuestra DB simulada.
    JOBS_DB[job_id] = {"status": "pending", "filename": file.filename, "path": apk_path}

    # 9. ¡La parte clave! Encolamos la tarea en Celery para que un worker la procese.
    # .delay() es la forma de llamar a una tarea de Celery de forma asíncrona.
    # Le pasamos los datos que necesita el worker para realizar el trabajo.
    analyze_static.delay(job_id=job_id, object_path=apk_path)

    print(f"Trabajo '{job_id}' creado para el archivo '{file.filename}'. Tarea encolada.")
    
    # 10. Respondemos al cliente inmediatamente.
    return {"job_id": job_id, "message": "El trabajo de análisis ha sido aceptado."}


@app.get("/status/{job_id}")
async def get_job_status(job_id: str):
    """
    Endpoint de utilidad para consultar el estado de un trabajo.
    """
    job = JOBS_DB.get(job_id)
    if not job:
        raise HTTPException(status_code=404, detail="Trabajo no encontrado.")
    
    # Podríamos añadir más detalles, como el resultado si ya está completado.
    # Por ahora, solo devolvemos la información que tenemos.
    return job
