# /StaticAnalyzer/tests/test_analyzer.py

import pytest
import time
from unittest.mock import MagicMock

# Importamos la app de Celery y la tarea que queremos probar
from analyzer import app as celery_app, analyze_static, _perform_analysis
from utils.timeout import TimeoutException

# --- Configuración de Pytest y Fixtures ---

APK_VALIDO_PATH = 'tests/test_app.apk'
APK_INVALIDO_PATH = 'tests/test_app.apk.invalid' # Un archivo que crearemos para la prueba

@pytest.fixture(scope="session", autouse=True)
def setup_celery_for_testing():
    """
    Esta fixture configura Celery para que se ejecute en modo de prueba.
    'autouse=True' hace que se ejecute automáticamente para todos los tests.
    """
    celery_app.conf.update(task_always_eager=True)

@pytest.fixture(scope="session")
def androguard_analysis():
    """
    Fixture que ejecuta el análisis de Androguard UNA SOLA VEZ y reutiliza
    el resultado en múltiples tests. Esto hace que las pruebas sean mucho más rápidas.
    """
    try:
        a, d, dx = _perform_analysis(APK_VALIDO_PATH)
        return a, d, dx
    except Exception as e:
        pytest.fail(f"No se pudo analizar el APK de prueba. Asegúrate de que '{APK_VALIDO_PATH}' existe y es válido. Error: {e}")

# --- Pruebas de las Funciones Auxiliares ---

def test_perform_analysis_success(androguard_analysis):
    """Prueba que el análisis de un APK válido produce los objetos correctos."""
    a, d, dx = androguard_analysis
    assert a is not None, "El objeto APK no debería ser nulo"
    assert a.get_package() is not None, "El APK debería tener un nombre de paquete"
    assert dx is not None, "El objeto de análisis (dx) no debería ser nulo"

def test_extract_permissions(androguard_analysis):
    """Prueba que la extracción de permisos devuelve una lista."""
    a, _, _ = androguard_analysis
    permissions = a.get_permissions()
    assert isinstance(permissions, list)
    # Ejemplo: si sabemos que nuestro APK de prueba pide este permiso.
    # assert "android.permission.INTERNET" in permissions


# --- Pruebas de la Tarea Principal de Celery ---

def test_analyze_static_success():
    """Prueba el flujo completo de la tarea con un APK válido."""
    result = analyze_static.delay(job_id='test-success', object_path=APK_VALIDO_PATH).get()
    
    assert result['status'] == 'success'
    assert result['job_id'] == 'test-success'
    assert 'package_name' in result['features']
    assert 'permissions' in result['features']

def test_analyze_static_apk_corrupto():
    """Prueba que la tarea maneja correctamente un archivo que no es un APK."""
    # Creamos un archivo falso para la prueba
    with open(APK_INVALIDO_PATH, "w") as f:
        f.write("esto no es un apk")
    
    result = analyze_static.delay(job_id='test-corrupt', object_path=APK_INVALIDO_PATH).get()

    assert result['status'] == 'error'
    assert 'Archivo APK corrupto o inválido' in result['message']

def test_analyze_static_timeout(mocker):
    """
    Prueba que el timeout funciona.
    Usamos 'mocker' para reemplazar la función de análisis por una que simplemente duerme.
    """
    # Importamos el módulo que queremos 'mockear'
    import analyzer
    
    # Hacemos que _perform_analysis duerma por más tiempo que el timeout
    mocker.patch('analyzer._perform_analysis', side_effect=lambda path: time.sleep(0.2))

    # Ejecutamos la tarea con un timeout muy corto para la prueba
    from config import ANALYSIS_TIMEOUT_SECONDS
    analyzer.ANALYSIS_TIMEOUT_SECONDS = 0.1 # Sobrescribimos temporalmente
    
    result = analyze_static.delay(job_id='test-timeout', object_path=APK_VALIDO_PATH).get()

    assert result['status'] == 'error'
    assert 'Timeout' in result['message']
    
    # Restauramos el valor original para no afectar otros tests
    analyzer.ANALYSIS_TIMEOUT_SECONDS = ANALYSIS_TIMEOUT_SECONDS
