# Guía de Pruebas - UNI-Secure-Droid Sprint 3

## Opción 1: Prueba Rápida (Sin Docker)

### 1. Instalar dependencias
```bash
# StaticAnalyzer
cd StaticAnalyzer
pip install -r requirements.txt

# DynamicAnalyzer
cd ../DynamicAnalyzer
pip install -r requirements.txt
```

### 2. Iniciar RabbitMQ
```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

### 3. Iniciar workers (en terminales separadas)

**Terminal 1 - Static Analyzer Worker:**
```bash
cd StaticAnalyzer
celery -A analyzer worker --loglevel=info -Q static_analysis
```

**Terminal 2 - Static Analyzer API:**
```bash
cd StaticAnalyzer
uvicorn main:app --host 0.0.0.0 --port 8000
```

**Terminal 3 - Dynamic Analyzer Worker (simulado):**
```bash
cd DynamicAnalyzer
celery -A analyzer worker --loglevel=info -Q dynamic_analysis
```

**Terminal 4 - Dynamic Analyzer API:**
```bash
cd DynamicAnalyzer
uvicorn main:app --host 0.0.0.0 --port 8001
```

### 4. Probar análisis estático
```bash
curl -X POST http://localhost:8000/analyze \
  -F "file=@tests/test_app.apk"
```

Respuesta:
```json
{"job_id": "abc-123", "message": "El trabajo de análisis ha sido aceptado."}
```

### 5. Consultar estado
```bash
curl http://localhost:8000/status/abc-123
```

---

## Opción 2: Prueba Completa (Con Docker)

### 1. Construir imágenes
```bash
docker-compose build
```

### 2. Iniciar servicios
```bash
docker-compose up -d
```

### 3. Ver logs
```bash
docker-compose logs -f
```

### 4. Probar con script Python
```bash
python test_system.py
```

---

## Opción 3: Prueba Manual Paso a Paso

### Paso 1: Subir APK
```python
import requests

with open("tests/test_app.apk", "rb") as f:
    files = {"file": ("test.apk", f)}
    response = requests.post("http://localhost:8000/analyze", files=files)
    job_id = response.json()["job_id"]
    print(f"Job ID: {job_id}")
```

### Paso 2: Verificar análisis estático
```python
import requests
import time

response = requests.get(f"http://localhost:8000/status/{job_id}")
print(response.json())
```

### Paso 3: Invocar análisis dinámico (desde orquestador)
```python
from Orchestrator.orchestrator import orchestrate_analysis

result = orchestrate_analysis.delay(
    job_id="test_123",
    apk_path="/tmp/apk_storage/test.apk"
)

print(result.get(timeout=180))
```

---

## Pruebas de Seguridad (RNF)

### Test RNF-4: Aislamiento de red
```bash
cd DynamicAnalyzer
pytest tests/test_sandbox_security.py::TestSandboxSecurity::test_rnf4_network_isolation -v
```

### Test RNF-5: Timeout y kill-switch
```bash
pytest tests/test_sandbox_security.py::TestSandboxSecurity::test_rnf5_timeout_killswitch -v
```

### Test RNF-7: Almacenamiento seguro
```bash
pytest tests/test_sandbox_security.py::TestSandboxSecurity::test_rnf7_secure_artifact_storage -v
```

---

## Verificar Componentes

### RabbitMQ Management
```
http://localhost:15672
Usuario: guest
Password: guest
```

### Ver colas activas
```bash
docker exec rabbitmq rabbitmqctl list_queues
```

### Ver workers conectados
```bash
celery -A analyzer inspect active
```

---

## Troubleshooting

### Error: "No module named 'androguard'"
```bash
pip install androguard
```

### Error: "Connection refused to RabbitMQ"
```bash
docker ps | grep rabbitmq
docker logs rabbitmq
```

### Error: "Emulator not found"
```bash
# Instalar Android SDK
export ANDROID_HOME=/opt/android-sdk
export PATH=$PATH:$ANDROID_HOME/emulator
```

### Ver logs de análisis
```bash
# Static
docker logs static-analyzer

# Dynamic
docker logs dynamic-analyzer
```

---

## Resultados Esperados

✅ **Análisis Estático:**
- Permisos extraídos
- APIs detectadas
- URLs encontradas

✅ **Análisis Dinámico:**
- Conexiones de red capturadas
- Archivos creados/modificados
- Syscalls monitoreadas

✅ **Reporte:**
- PDF generado en `reports/`
- Incluye hallazgos estáticos y dinámicos
- Tiempo < 2 segundos

---

## Comandos Útiles

```bash
# Detener todo
docker-compose down

# Limpiar volúmenes
docker-compose down -v

# Reconstruir
docker-compose up -d --build

# Ver recursos
docker stats

# Entrar a contenedor
docker exec -it dynamic-analyzer bash
```
