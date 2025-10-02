import pytest
from unittest.mock import Mock
from time import sleep
import sys
import os
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

# 🧩 Simulación de la clase real (ajusta el import a tu estructura)
from src.services.report_service import ReportService


@pytest.fixture
def mock_dependencies():
    """
    Crea dependencias simuladas de MetadataDB y ObjectStore.
    """
    metadata_db = Mock()
    object_store = Mock()
    return metadata_db, object_store


@pytest.fixture
def service(mock_dependencies):
    """
    Instancia el servicio con dependencias simuladas.
    """
    metadata_db, object_store = mock_dependencies
    return ReportService(metadata_db, object_store)


def test_generate_report_success(service):
    """
    ✅ Caso exitoso:
    Debe generar un reporte válido (PDF o JSON) si se pasa una entrada correcta.
    """
    entrada = {
        "job_id": "123",
        "verdict": "malicious",
        "risk": "high",
        "family": "trojan",
        "top_signals": ["api_call", "network_activity"]
    }

    result = service.generate_report(entrada)

    assert result["status_code"] == 200
    assert result["content_type"] in ["application/pdf", "application/json"]
    assert "verdict" in result["data"]
    assert "risk" in result["data"]


def test_generate_report_job_not_found(service):
    """
    ⚠️ Caso: job_id inexistente
    Debe devolver código 404 si no hay información del job.
    """
    entrada = {
        "job_id": "999"
    }

    # Simula que MetadataDB no encuentra el job
    service.metadata_db.get_job.return_value = None

    result = service.generate_report(entrada)
    assert result["status_code"] == 404
    assert "error" in result


def test_generate_report_timeout(service, monkeypatch):
    """
    🕒 Caso: Timeout de renderizado (>2s)
    Debe devolver JSON básico sin generar PDF.
    """
    def slow_render(*args, **kwargs):
        sleep(2.5)  # simula lentitud
        return None

    # Simulamos que el método interno de renderizado es lento
    monkeypatch.setattr(service, "render_pdf", slow_render)

    entrada = {
        "job_id": "321",
        "verdict": "benign",
        "risk": "low",
        "family": "none",
        "top_signals": []
    }

    result = service.generate_report(entrada)

    assert result["status_code"] == 200
    assert result["content_type"] == "application/json"
    assert "verdict" in result["data"]
    assert "top_signals" in result["data"]


def test_generate_report_internal_error(service, monkeypatch):
    """
    💥 Caso: Error interno al crear plantilla
    Debe devolver error 500 con mensaje claro.
    """
    def raise_error(*args, **kwargs):
        raise Exception("Fallo al crear plantilla")

    # Simulamos fallo interno
    monkeypatch.setattr(service, "render_pdf", raise_error)

    entrada = {
        "job_id": "500",
        "verdict": "unknown",
        "risk": "medium",
        "family": "unknown",
        "top_signals": []
    }

    result = service.generate_report(entrada)

    assert result["status_code"] == 500
    assert "error" in result
