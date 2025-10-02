# /StaticAnalyzer/config.py

# URL para que Celery se conecte al broker de mensajes.
# Este es el valor por defecto para RabbitMQ ejecutándose localmente.
CELERY_BROKER_URL = "amqp://guest:guest@localhost:5672//"

# Límite de tiempo en segundos para el análisis estático.
ANALYSIS_TIMEOUT_SECONDS = 15
