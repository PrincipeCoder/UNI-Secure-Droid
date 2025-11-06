# /DynamicAnalyzer/config.py

import os

# RNF-2: Tiempo máximo de análisis dinámico
DYNAMIC_ANALYSIS_TIMEOUT = int(os.getenv("DYNAMIC_TIMEOUT", "90"))

# RNF-4: Configuración de aislamiento
SANDBOX_NETWORK_ISOLATED = True
SANDBOX_NO_HOST_ACCESS = True

# Configuración del emulador
EMULATOR_AVD_NAME = os.getenv("AVD_NAME", "malware_sandbox")
EMULATOR_PORT = int(os.getenv("EMULATOR_PORT", "5554"))
ADB_PATH = os.getenv("ADB_PATH", "adb")

# RNF-7: Almacenamiento seguro de artefactos
ARTIFACTS_PATH = "/tmp/sandbox_artifacts/"
ARTIFACTS_PERMISSIONS = 0o600

# Celery
CELERY_BROKER_URL = os.getenv("CELERY_BROKER_URL", "amqp://guest@localhost//")
