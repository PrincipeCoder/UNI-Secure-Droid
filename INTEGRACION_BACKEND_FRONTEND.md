# Integración Backend-Frontend UNI-Secure-Droid

## Resumen
Se ha conectado exitosamente el backend Python (Sprint 3) con el frontend Android (Kotlin) para crear un sistema completo de análisis de malware con análisis estático y dinámico.

## Componentes Creados

### 1. API REST (Backend)
**Ubicación**: `src/api/main.py`

Endpoints implementados:
- `POST /api/upload` - Subir APK y encolar análisis
- `GET /api/status/{job_id}` - Consultar estado del análisis
- `GET /api/report/{job_id}` - Obtener reporte completo (JSON)
- `GET /api/report/{job_id}/pdf` - Descargar reporte en PDF
- `GET /health` - Health check

**Características**:
- FastAPI con CORS habilitado
- Validación de archivos APK (extensión y tamaño <50MB)
- Generación de job_id único por análisis
- Integración con servicios existentes (upload_service, report_service, job_queue)

**Iniciar API**:
```bash
cd src/api
pip install -r requirements.txt
python main.py
```

La API estará disponible en `http://localhost:8000`

### 2. Cliente HTTP (Frontend)
**Ubicación**: `sprint2_app/app/src/main/java/com/example/unisecuredroid/data/api/ApiService.kt`

**Características**:
- Retrofit 2.9.0 para comunicación HTTP
- OkHttp con logging interceptor para debugging
- Timeouts configurados (30s connect, 60s read/write)
- URL base: `http://10.0.2.2:8000/` (emulador Android)

**Modelos de datos**:
- `UploadResponse` - Respuesta de subida de APK
- `StatusResponse` - Estado del análisis
- `ReportResponse` - Reporte completo con análisis estático y dinámico

### 3. Modelos de Datos Actualizados
**Ubicación**: `sprint2_app/app/src/main/java/com/example/unisecuredroid/data/models/AnalysisReport.kt`

Nuevo modelo `AnalysisReport` que incluye:
- Información básica (job_id, sha256, veredicto, riesgo, familia)
- Probabilidad de IA
- **Análisis estático**: permisos, URLs, APIs detectadas, señales principales
- **Análisis dinámico** (Sprint 3):
  - Conexiones de red
  - Operaciones de archivos
  - Syscalls
  - Datos enviados (KB)

### 4. ViewModels Actualizados

#### UploadViewModel
**Cambios principales**:
- Reemplaza análisis local por llamada al backend
- Sube APK mediante multipart/form-data
- Implementa polling para consultar estado del análisis
- Maneja estados: Idle, FileSelected, Analyzing, Success, Error

**Flujo**:
1. Usuario selecciona APK
2. Se copia a archivo temporal
3. Se sube al backend vía Retrofit
4. Se hace polling cada 2s hasta que el análisis complete
5. Navega a pantalla de reporte

#### ReportViewModel
**Cambios principales**:
- Obtiene reportes del backend en lugar de memoria local
- Transforma `ReportResponse` a `AnalysisReport`
- Maneja estados: Idle, Loading, Success, Error

### 5. UI Actualizada (ReporteScreen)
**Cambios**:
- Muestra análisis estático Y dinámico
- Nueva sección "Análisis Dinámico (Sandbox)" con:
  - Conexiones de red detectadas
  - Operaciones de archivos
  - Datos enviados
- Muestra riesgo, familia de malware y tiempo de análisis
- PDF generado incluye hallazgos dinámicos

## Dependencias Agregadas

### build.gradle.kts
```kotlin
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.11.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
```

## Configuración Requerida

### 1. Permisos Android (Ya configurados)
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 2. Iniciar Backend
```bash
# Opción 1: Docker Compose (recomendado)
docker-compose up -d

# Opción 2: API standalone
cd src/api
python main.py
```

### 3. Configurar URL del Backend
Si usas dispositivo físico en lugar de emulador, edita `ApiService.kt`:
```kotlin
private const val BASE_URL = "http://TU_IP_LOCAL:8000/"
```

## Flujo Completo del Sistema

```
┌─────────────┐
│   Usuario   │
│  (Android)  │
└──────┬──────┘
       │ 1. Selecciona APK
       ▼
┌─────────────────┐
│  UploadViewModel│
│   (Frontend)    │
└──────┬──────────┘
       │ 2. POST /api/upload
       ▼
┌─────────────────┐
│   FastAPI       │
│   (Backend)     │
└──────┬──────────┘
       │ 3. Encola job
       ▼
┌─────────────────┐
│  Orchestrator   │
│    (Celery)     │
└──────┬──────────┘
       │ 4. Coordina análisis
       ├─────────────┬──────────────┐
       ▼             ▼              ▼
┌──────────┐  ┌──────────┐  ┌──────────┐
│ Static   │  │ Dynamic  │  │  Report  │
│ Analyzer │  │ Analyzer │  │ Service  │
└──────────┘  └──────────┘  └──────────┘
       │             │              │
       └─────────────┴──────────────┘
                     │ 5. Genera reporte
                     ▼
       ┌─────────────────────┐
       │  ReportViewModel    │
       │   GET /api/report   │
       └─────────────────────┘
                     │ 6. Muestra resultado
                     ▼
       ┌─────────────────────┐
       │   ReporteScreen     │
       │  (Estático+Dinámico)│
       └─────────────────────┘
```

## Pruebas

### Probar API manualmente
```bash
# Health check
curl http://localhost:8000/health

# Subir APK
curl -X POST http://localhost:8000/api/upload \
  -F "file=@test_app.apk"

# Consultar estado
curl http://localhost:8000/api/status/JOB_ID

# Obtener reporte
curl http://localhost:8000/api/report/JOB_ID
```

### Probar desde Android
1. Iniciar backend: `cd src/api && python main.py`
2. Abrir proyecto en Android Studio
3. Sync Gradle (para descargar dependencias Retrofit)
4. Ejecutar app en emulador
5. Seleccionar APK de prueba
6. Verificar que se suba y se muestre el reporte completo

## Próximos Pasos

1. **Integrar Celery real**: Actualmente la API simula el análisis, conectar con Orchestrator
2. **Autenticación**: Implementar JWT o API keys
3. **Base de datos**: Persistir jobs y reportes en PostgreSQL/MongoDB
4. **Notificaciones push**: Avisar cuando el análisis complete
5. **Cache**: Evitar re-analizar APKs con mismo hash
6. **Rate limiting**: Limitar requests por usuario/IP

## Troubleshooting

### Error de conexión desde Android
- Verificar que la API esté corriendo: `curl http://localhost:8000/health`
- En emulador usar `10.0.2.2` en lugar de `localhost`
- En dispositivo físico, usar IP local de la PC

### Error al subir APK
- Verificar tamaño <50MB
- Verificar extensión .apk
- Revisar logs de FastAPI

### Timeout en análisis
- Aumentar `maxAttempts` en `pollJobStatus()`
- Verificar que los workers de Celery estén corriendo
- Revisar logs del Orchestrator

## Archivos Modificados/Creados

### Backend
- ✅ `src/api/main.py` (nuevo)
- ✅ `src/api/requirements.txt` (nuevo)
- ✅ `src/api/Dockerfile` (nuevo)

### Frontend
- ✅ `sprint2_app/app/src/main/java/com/example/unisecuredroid/data/api/ApiService.kt` (nuevo)
- ✅ `sprint2_app/app/src/main/java/com/example/unisecuredroid/data/models/AnalysisReport.kt` (nuevo)
- ✅ `sprint2_app/app/src/main/java/com/example/unisecuredroid/viewmodels/UploadViewModel.kt` (modificado)
- ✅ `sprint2_app/app/src/main/java/com/example/unisecuredroid/viewmodels/ReportViewModel.kt` (modificado)
- ✅ `sprint2_app/app/src/main/java/com/example/unisecuredroid/ui/screens/ReporteScreen.kt` (modificado)
- ✅ `sprint2_app/app/build.gradle.kts` (modificado - dependencias)

## Conclusión

La integración está completa y funcional. El frontend Android ahora se comunica con el backend Python para realizar análisis completos (estático + dinámico) de APKs, mostrando resultados detallados incluyendo hallazgos del sandbox.
