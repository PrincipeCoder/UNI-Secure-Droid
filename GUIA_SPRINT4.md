# Guía para Sprint 4 - Sistema Completo Integrado

## ✅ Estado Actual (Sprint 3 + Integración)

### Componentes Implementados

1. **Frontend Android (Kotlin)** - 100% completo
   - UI para selección de APK
   - Subida al backend vía HTTP
   - Polling de estado
   - Visualización de resultados (estático + dinámico)
   - Generación de PDF

2. **API REST (FastAPI)** - 100% completo
   - Endpoints: upload, status, report, health
   - Validación de archivos
   - Persistencia en BD simple (JSON)
   - **Conexión con Orchestrator** (si está disponible)
   - Fallback a modo simulado

3. **Backend Sprint 3** - 100% completo
   - StaticAnalyzer (Python)
   - DynamicAnalyzer (Sandbox)
   - Orchestrator (Celery)
   - ReportService

## 🔌 Cómo Funciona la Conexión

### Flujo Completo

```
1. Usuario selecciona APK en Android
   ↓
2. Frontend sube APK → POST /api/upload
   ↓
3. API guarda APK y crea job en BD
   ↓
4. API intenta llamar al Orchestrator:
   
   SI Celery está corriendo:
   ├─ orchestrate_analysis.delay(job_id, apk_path)
   ├─ Orchestrator coordina StaticAnalyzer + DynamicAnalyzer
   └─ Resultado se guarda en BD
   
   SI NO está Celery:
   └─ Retorna datos simulados (para testing)
   ↓
5. Frontend hace polling → GET /api/status/{job_id}
   ↓
6. Cuando status = "completed" → GET /api/report/{job_id}
   ↓
7. Muestra resultados en pantalla
```

## 🚀 Cómo Iniciar el Sistema Completo

### Opción 1: Con Docker Compose (Recomendado)

```bash
# Iniciar todo el sistema
docker-compose up -d

# Verificar que todo esté corriendo
docker-compose ps

# Ver logs
docker-compose logs -f
```

### Opción 2: Manual (Para desarrollo)

**Terminal 1 - RabbitMQ:**
```bash
docker run -d -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

**Terminal 2 - API REST:**
```bash
cd src/api
pip install -r requirements.txt
python main.py
```

**Terminal 3 - Orchestrator Worker:**
```bash
cd Orchestrator
celery -A orchestrator worker --loglevel=info
```

**Terminal 4 - StaticAnalyzer Worker:**
```bash
cd StaticAnalyzer
celery -A analyzer worker --loglevel=info -Q static_analysis
```

**Terminal 5 - DynamicAnalyzer Worker:**
```bash
cd DynamicAnalyzer
celery -A analyzer worker --loglevel=info -Q dynamic_analysis
```

**Terminal 6 - Android App:**
```bash
# Abrir Android Studio y ejecutar la app
```

## 📝 Para Sprint 4 - Tareas Pendientes

### 1. Integración del Modelo de IA

**Ubicación:** `StaticAnalyzer/analyzer.py`

**Qué hacer:**
```python
# Agregar al StaticAnalyzer
import tensorflow as tf  # o PyTorch

def classify_with_ai(features):
    """
    Clasificar APK usando modelo entrenado
    """
    model = tf.keras.models.load_model('models/malware_classifier.h5')
    prediction = model.predict(features)
    
    return {
        "verdict": "MALICIOSO" if prediction > 0.5 else "BENIGNO",
        "ai_probability": float(prediction),
        "confidence": "high" if abs(prediction - 0.5) > 0.3 else "low"
    }
```

**Integrar en el análisis:**
```python
# En analyze_apk_task
static_features = extract_features(apk_path)
ai_result = classify_with_ai(static_features)

return {
    "verdict": ai_result["verdict"],
    "ai_probability": ai_result["ai_probability"],
    # ... resto de features
}
```

### 2. Base de Datos Real (PostgreSQL/MongoDB)

**Reemplazar:** `src/api/database.py`

**Con:**
```python
from sqlalchemy import create_engine, Column, String, JSON, DateTime
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker

Base = declarative_base()

class Job(Base):
    __tablename__ = 'jobs'
    
    job_id = Column(String, primary_key=True)
    apk_path = Column(String)
    sha256 = Column(String)
    status = Column(String)
    result = Column(JSON)
    created_at = Column(DateTime)
    completed_at = Column(DateTime)
```

### 3. Autenticación (JWT)

**Agregar a la API:**
```python
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials

security = HTTPBearer()

@app.post("/api/upload")
async def upload_apk(
    file: UploadFile,
    credentials: HTTPAuthorizationCredentials = Depends(security)
):
    # Validar token
    user = verify_token(credentials.credentials)
    # ... resto del código
```

### 4. Cache de Resultados

**Evitar re-analizar APKs:**
```python
@app.post("/api/upload")
async def upload_apk(file: UploadFile):
    sha256 = calculate_hash(file)
    
    # Buscar en cache
    cached_result = db.get_by_hash(sha256)
    if cached_result:
        return {"job_id": cached_result["job_id"], "cached": True}
    
    # Análisis nuevo
    # ...
```

### 5. Notificaciones Push (Firebase)

**Para avisar cuando el análisis complete:**
```python
from firebase_admin import messaging

def notify_completion(user_token, job_id):
    message = messaging.Message(
        notification=messaging.Notification(
            title='Análisis Completo',
            body=f'El análisis {job_id} ha finalizado'
        ),
        token=user_token
    )
    messaging.send(message)
```

### 6. Dashboard Web (Opcional)

**Tecnologías sugeridas:**
- React/Vue.js para frontend
- Gráficos con Chart.js
- Tabla de jobs recientes
- Estadísticas de malware detectado

## 🧪 Testing

### Probar la Integración Completa

```bash
# 1. Health check
curl http://localhost:8000/health

# 2. Subir APK
curl -X POST http://localhost:8000/api/upload \
  -F "file=@test_app.apk"

# Respuesta:
# {"job_id": "abc-123", "hash": "...", "status": "queued"}

# 3. Consultar estado
curl http://localhost:8000/api/status/abc-123

# 4. Obtener reporte
curl http://localhost:8000/api/report/abc-123
```

### Desde Android

1. Iniciar backend: `cd src/api && python main.py`
2. Ejecutar app en emulador
3. Seleccionar APK
4. Presionar "Analizar con IA"
5. Esperar resultados
6. Verificar que muestre análisis estático + dinámico

## 📊 Métricas a Implementar (Sprint 4)

1. **Tiempo de análisis promedio**
2. **Tasa de detección (TP, FP, TN, FN)**
3. **APKs analizados por día**
4. **Familias de malware más comunes**
5. **Uso de recursos (CPU, RAM)**

## 🔒 Seguridad (Sprint 4)

1. **Rate limiting** - Limitar requests por IP
2. **Validación de APK** - Verificar firma digital
3. **Sandbox hardening** - Mejorar aislamiento
4. **Logs de auditoría** - Registrar todas las acciones
5. **Encriptación** - HTTPS obligatorio

## 📦 Deployment (Sprint 4)

### Docker Compose Completo

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:14
    environment:
      POSTGRES_DB: unisecuredroid
      POSTGRES_USER: admin
      POSTGRES_PASSWORD: secure_password
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7

  rabbitmq:
    image: rabbitmq:3-management

  api:
    build: ./src/api
    ports:
      - "8000:8000"
    depends_on:
      - postgres
      - rabbitmq

  orchestrator:
    build: ./Orchestrator
    depends_on:
      - rabbitmq

  static-analyzer:
    build: ./StaticAnalyzer
    depends_on:
      - rabbitmq

  dynamic-analyzer:
    build: ./DynamicAnalyzer
    depends_on:
      - rabbitmq
    privileged: true

volumes:
  postgres_data:
```

## 🎯 Checklist Sprint 4

- [ ] Integrar modelo de IA entrenado
- [ ] Migrar a PostgreSQL/MongoDB
- [ ] Implementar autenticación JWT
- [ ] Agregar cache de resultados
- [ ] Implementar notificaciones push
- [ ] Crear dashboard web
- [ ] Agregar métricas y logging
- [ ] Implementar rate limiting
- [ ] Configurar HTTPS
- [ ] Escribir tests automatizados
- [ ] Documentar API (Swagger)
- [ ] Preparar deployment en producción

## 📞 Soporte

Si hay problemas con la integración:

1. Verificar logs: `docker-compose logs -f`
2. Verificar que RabbitMQ esté corriendo: `http://localhost:15672`
3. Verificar que la API responda: `curl http://localhost:8000/health`
4. Revisar `jobs_db.json` para ver el estado de los jobs

## 🎉 Conclusión

La integración backend-frontend está **100% completa**. El sistema puede funcionar en:

- **Modo completo:** Con Celery, Orchestrator, y análisis real
- **Modo simulado:** Sin Celery, con datos de prueba (para desarrollo frontend)

Para Sprint 4, el enfoque debe ser:
1. Agregar el modelo de IA
2. Mejorar persistencia y seguridad
3. Agregar features avanzadas (cache, notificaciones, dashboard)
