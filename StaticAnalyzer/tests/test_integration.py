import pytest
import time
import os
from fastapi.testclient import TestClient
from unittest.mock import MagicMock

# Importamos la app de FastAPI y la base de datos simulada
from main import app
from shared_state import JOBS_DB
# Importamos la app de Celery para configurar el modo de prueba
from analyzer import app as celery_app

# --- Configuración ---

# Creamos un cliente de prueba para nuestra API
client = TestClient(app)

# Construimos la ruta a los archivos de prueba de forma robusta
TESTS_DIR = os.path.dirname(os.path.abspath(__file__))
DUMMY_FILE_PATH = os.path.join(TESTS_DIR, 'dummy_file.txt')


@pytest.fixture(scope="session", autouse=True)
def setup_celery():
    """Configura Celery en modo síncrono para las pruebas de integración."""
    celery_app.conf.update(task_always_eager=True)
    
@pytest.fixture(autouse=True)
def clear_jobs_db():
    """Limpia la base de datos simulada antes de cada prueba de integración."""
    JOBS_DB.clear()

# --- Pruebas de Integración ---

def test_full_analysis_flow_success(mocker, mock_androguard_success):
    """
    Prueba el flujo completo: subir un APK simulado y verificar que se procesa.
    """
    # Mockeamos la función de análisis para que no dependa de un APK real
    mocker.patch('analyzer._perform_analysis', return_value=mock_androguard_success)
    # Mockeamos el Timeout para que no use signals, que fallan en threads
    mocker.patch('utils.timeout.Timeout.__enter__', return_value=None)
    mocker.patch('utils.timeout.Timeout.__exit__', return_value=None)
    
    # 1. Simular la subida de un archivo al endpoint /analyze
    # No necesitamos un APK real, solo un stream de bytes para el upload
    with open(DUMMY_FILE_PATH, 'w') as f:
        f.write("dummy apk content")

    with open(DUMMY_FILE_PATH, 'rb') as apk_file:
        response = client.post(
            "/analyze",
            files={"file": ("test_app.apk", apk_file, "application/vnd.android.package-archive")}
        )

    # 2. Verificar la respuesta inicial de la API
    assert response.status_code == 202 # 202 Accepted
    response_json = response.json()
    assert "job_id" in response_json
    job_id = response_json['job_id']
    
    # 3. Verificar el estado final del trabajo
    response_status = client.get(f"/status/{job_id}")
    assert response_status.status_code == 200
    job_details = response_status.json()
    
    assert job_details['status'] == 'completed'
    assert 'features' in job_details
    assert job_details['features']['package_name'] == 'com.test.app'

def test_upload_invalid_file_type():
    """
    Prueba que la API rechaza archivos que no son .apk.
    """
    with open(DUMMY_FILE_PATH, 'w') as f:
        f.write("hello")

    with open(DUMMY_FILE_PATH, 'rb') as fake_file:
        response = client.post("/analyze", files={"file": ("not_an_apk.txt", fake_file, "text/plain")})

    assert response.status_code == 422 # 422 Unprocessable Entity
    assert "Tipo de archivo inválido" in response.json()['detail']