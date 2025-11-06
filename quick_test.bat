@echo off
REM Script de prueba rápida para Windows

echo === PRUEBA RAPIDA UNI-SECURE-DROID ===

REM 1. Iniciar RabbitMQ
echo Iniciando RabbitMQ...
docker run -d --name rabbitmq -p 5672:5672 rabbitmq:3-management 2>nul || docker start rabbitmq
timeout /t 5 /nobreak >nul

REM 2. Iniciar Static Analyzer Worker
echo Iniciando Static Analyzer Worker...
cd StaticAnalyzer
start /B celery -A analyzer worker --loglevel=info -Q static_analysis
timeout /t 3 /nobreak >nul

REM 3. Iniciar API
echo Iniciando API...
start /B uvicorn main:app --host 0.0.0.0 --port 8000
timeout /t 3 /nobreak >nul

REM 4. Volver al directorio raíz
cd ..

echo.
echo === SISTEMA INICIADO ===
echo.
echo API corriendo en: http://localhost:8000
echo RabbitMQ Management: http://localhost:15672 (guest/guest)
echo.
echo Para probar el sistema, ejecuta en otra terminal:
echo   python test_manual.py
echo.
echo O prueba manualmente:
echo   curl -X POST http://localhost:8000/analyze -F "file=@test_dummy.apk"
echo.
echo Presiona Ctrl+C para detener los servicios
pause
