# /StaticAnalyzer/tests/test_integration.py

import pytest
import time
from fastapi.testclient import TestClient

# Importamos la app de FastAPI y la base de datos simulada
from main import app, JOBS_DB
# Importamos la app de Celery para configurar el modo de prueba
from analyzer import app as celery_app

# --- Configuración ---

# Creamos un cliente de prueba para nuestra API
client = TestClient(app)

@pytest.fixture(scope="session", autouse=True)
def setup_celery():
    """Configura Celery en modo síncrono para las pruebas de integración."""
    celery_app.conf.update(task_always_eager=True)
    
@pytest.fixture(autouse=True)
def clear_jobs_db():
    """Limpia la base de datos simulada antes de cada prueba de integración."""
    JOBS_DB.clear()

# --- Pruebas de Integración ---

def test_full_analysis_flow_success():
    """
    Prueba el flujo completo: subir un APK válido y verificar que se procesa.
    """
    apk_path = 'tests/test_app.apk'
    
    # 1. Simular la subida de un archivo al endpoint /analyze
    with open(apk_path, 'rb') as apk_file:
        response = client.post("/analyze", files={"file": ("test_app.apk", apk_file, "application/vnd.android.package-archive")})

    # 2. Verificar la respuesta inicial de la API
    assert response.status_code == 202 # 202 Accepted
    response_json = response.json()
    assert "job_id" in response_json
    job_id = response_json['job_id']
    
    # 3. Verificar el estado final del trabajo
    # Como Celery está en modo 'eager', la tarea ya se ejecutó.
    # Usamos el endpoint /status para verificar el resultado.
    response_status = client.get(f"/status/{job_id}")

    assert response_status.status_code == 200
    job_details = response_status.json()
    
    assert job_details['status'] == 'completed'
    assert 'features' in job_details
    assert 'package_name' in job_details['features']

def test_upload_invalid_file_type():
    """
    Prueba que la API rechaza archivos que no son .apk.
    """
    # Creamos un archivo de texto falso
    with open('tests/not_an_apk.txt', 'w') as f:
        f.write("hello")

    with open('tests/not_an_apk.txt', 'rb') as fake_file:
        response = client.post("/analyze", files={"file": ("not_an_apk.txt", fake_file, "text/plain")})

    # Verificamos que la API responde con un error de validación
    assert response.status_code == 422 # 422 Unprocessable Entity
    assert "Tipo de archivo inválido" in response.json()['detail']
