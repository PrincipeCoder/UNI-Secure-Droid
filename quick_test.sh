#!/bin/bash
# Script de prueba rápida

echo "=== PRUEBA RÁPIDA UNI-SECURE-DROID ==="

# 1. Iniciar RabbitMQ
echo "Iniciando RabbitMQ..."
docker run -d --name rabbitmq -p 5672:5672 rabbitmq:3-management 2>/dev/null || docker start rabbitmq
sleep 5

# 2. Iniciar Static Analyzer Worker
echo "Iniciando Static Analyzer Worker..."
cd StaticAnalyzer
celery -A analyzer worker --loglevel=info -Q static_analysis &
WORKER_PID=$!
sleep 3

# 3. Iniciar API
echo "Iniciando API..."
uvicorn main:app --host 0.0.0.0 --port 8000 &
API_PID=$!
sleep 3

# 4. Probar análisis
echo "Enviando APK de prueba..."
RESPONSE=$(curl -s -X POST http://localhost:8000/analyze -F "file=@../tests/test_app.apk")
JOB_ID=$(echo $RESPONSE | grep -o '"job_id":"[^"]*"' | cut -d'"' -f4)

echo "Job ID: $JOB_ID"

# 5. Consultar estado
echo "Consultando estado..."
for i in {1..10}; do
    sleep 2
    STATUS=$(curl -s http://localhost:8000/status/$JOB_ID | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    echo "  Estado: $STATUS"
    
    if [ "$STATUS" = "completed" ]; then
        echo "✓ Análisis completado"
        break
    fi
done

# Cleanup
echo "Limpiando..."
kill $WORKER_PID $API_PID 2>/dev/null

echo "=== PRUEBA FINALIZADA ==="
