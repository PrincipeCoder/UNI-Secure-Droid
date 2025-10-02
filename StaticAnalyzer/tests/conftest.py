import pytest
from unittest.mock import MagicMock

@pytest.fixture
def mock_androguard_success():
    """Crea un mock de un resultado de análisis de Androguard exitoso."""
    mock_apk = MagicMock()
    mock_apk.get_package.return_value = "com.test.app"
    mock_apk.get_main_activity.return_value = "com.test.app.MainActivity"
    mock_apk.get_permissions.return_value = ["android.permission.INTERNET"]

    mock_dx = MagicMock()
    # Mock para _extract_api_calls
    mock_method = MagicMock()
    mock_method.is_external.return_value = True
    mock_method.get_class_name.return_value = "Landroid/telephony/TelephonyManager;"
    mock_method.get_name.return_value = "getLine1Number"
    mock_dx.get_methods.return_value = [mock_method]

    # Mock para _extract_urls
    mock_url = MagicMock()
    mock_url.get_value.return_value = b"https://example.com"
    mock_string_analysis = MagicMock()
    mock_string_analysis.get_urls.return_value = [mock_url]
    mock_dx.get_strings_analysis.return_value = {"key": mock_string_analysis}

    return mock_apk, None, mock_dx