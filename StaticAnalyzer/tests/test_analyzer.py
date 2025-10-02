import pytest
import time
import os
from unittest.mock import MagicMock, patch

# Importamos la app de Celery y la tarea que queremos probar
from analyzer import app as celery_app, analyze_static, InvalidAPKError
from utils.timeout import TimeoutException
from shared_state import JOBS_DB

# --- Configuración de Pytest y Fixtures ---

DUMMY_APK_PATH = "/path/to/dummy.apk"

@pytest.fixture(scope="session", autouse=True)
def setup_celery_for_testing():
    """Configura Celery para que se ejecute en modo de prueba."""
    celery_app.conf.update(task_always_eager=True)

@pytest.fixture(autouse=True)
def clear_jobs_db():
    """Limpia la base de datos simulada antes de cada prueba."""
    JOBS_DB.clear()

# --- Pruebas de la Tarea Principal de Celery ---

def test_analyze_static_success(mocker, mock_androguard_success):
    """Prueba el flujo completo de la tarea con un análisis simulado exitoso."""
    mocker.patch('analyzer._perform_analysis', return_value=mock_androguard_success)

    job_id = 'test-success'
    JOBS_DB[job_id] = {"status": "pending"}
    result = analyze_static.delay(job_id=job_id, object_path=DUMMY_APK_PATH).get()
    
    assert result['status'] == 'success'
    assert result['job_id'] == job_id
    assert result['features']['package_name'] == "com.test.app"
    assert "android.permission.INTERNET" in result['features']['permissions']
    assert "Landroid/telephony/TelephonyManager;->getLine1Number" in result['features']['api_calls']
    assert "https://example.com" in result['features']['urls']

def test_analyze_static_apk_corrupto(mocker):
    """Prueba que la tarea maneja un InvalidAPKError de _perform_analysis."""
    mocker.patch('analyzer._perform_analysis', side_effect=InvalidAPKError("Test error"))
    
    job_id = 'test-corrupt'
    JOBS_DB[job_id] = {"status": "pending"}
    result = analyze_static.delay(job_id=job_id, object_path=DUMMY_APK_PATH).get()

    assert result['status'] == 'error'
    assert 'Archivo APK corrupto o inválido' in result['message']

def test_analyze_static_timeout(mocker):
    """Prueba que el timeout funciona simulando una TimeoutException."""
    # Mockeamos el gestor de contexto Timeout para que lance la excepción
    # directamente al entrar en el bloque 'with'.
    mocker.patch('analyzer.Timeout.__enter__', side_effect=TimeoutException)
    
    job_id = 'test-timeout'
    JOBS_DB[job_id] = {"status": "pending"}
    result = analyze_static.delay(job_id=job_id, object_path=DUMMY_APK_PATH).get()

    assert result['status'] == 'error'
    assert 'Timeout' in result['message']