# /StaticAnalyzer/utils/error_handling.py

class DecompilationError(Exception):
    """Error específico cuando Androguard no puede procesar el APK."""
    pass

class InvalidAPKError(Exception):
    """Se lanza cuando el archivo no es un APK válido o está corrupto."""
    pass
