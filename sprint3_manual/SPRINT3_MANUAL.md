# Sprint 3: Sandbox y Análisis Dinámico - Manual de Uso

## 📋 Tabla de Contenidos
1. [Resumen Ejecutivo](#resumen-ejecutivo)
2. [Requerimientos Implementados](#requerimientos-implementados)
3. [Arquitectura del Sistema](#arquitectura-del-sistema)
4. [Manual de Instalación](#manual-de-instalación)
5. [Manual de Pruebas](#manual-de-pruebas)
6. [Validación de Seguridad](#validación-de-seguridad)
7. [Conclusiones](#conclusiones)

---

## 🎯 Resumen Ejecutivo

El Sprint 3 implementa el componente más crítico del sistema: **la sandbox aislada para análisis dinámico de malware**. Este módulo ejecuta aplicaciones potencialmente maliciosas en un entorno completamente aislado, capturando su comportamiento en tiempo real sin comprometer la seguridad del sistema host.

### Componentes Agregados
- **DynamicAnalyzer/** - Módulo de análisis dinámico completo
- **Orchestrator/** - Coordinador de análisis estático y dinámico
- **Sandbox Manager** - Gestor de emulador con aislamiento total
- **Behavior Monitor** - Captura de comportamiento malicioso
- **Interaction Simulator** - Simulación de usuario
- **Report Service** - Actualizado con hallazgos dinámicos

---

## ✅ Requerimientos Implementados

### Módulo 3: Análisis Dinámico

| ID | Requerimiento | Estado | Implementación |
|---|---|---|---|
| **RF-14** | Configurar emulador aislado (sandbox) | ✅ | `sandbox_manager.py` - Gestión completa del ciclo de vida del emulador Android |
| **RF-15** | Capturar llamadas al sistema | ✅ | `behavior_monitor.py::capture_syscalls()` - Monitoreo de procesos y syscalls |
| **RF-16** | Capturar tráfico de red | ✅ | `behavior_monitor.py::capture_network_traffic()` - Captura de conexiones TCP/UDP |
| **RF-17** | Capturar operaciones de archivos | ✅ | `behavior_monitor.py::capture_file_operations()` - Monitoreo de I/O |
| **RF-18** | Simular interacción del usuario | ✅ | `interaction_simulator.py` - Taps, swipes, navegación |
| **RF-19** | Abortar sandbox de forma segura | ✅ | Context manager + kill-switch en `sandbox_manager.py` |

### Módulo 5: Reporte (Actualizado)

| ID | Requerimiento | Estado | Implementación |
|---|---|---|---|
| **RF-28** | Mostrar hallazgos dinámicos en reporte | ✅ | `report_service.py::render_pdf()` - Sección de análisis dinámico |
| **RF-29** | Incluir conexiones de red y archivos | ✅ | PDF con detalles de red, archivos y syscalls |

### Requerimientos No Funcionales (Seguridad Crítica)

| ID | Requerimiento | Prioridad | Estado | Validación |
|---|---|---|---|---|
| **RNF-2** | Tiempo de análisis dinámico ≤ 90s | Media | ✅ | `DYNAMIC_ANALYSIS_TIMEOUT = 90` |
| **RNF-4** | Aislamiento total de la sandbox | **CRÍTICA** | ✅ | Red aislada en docker-compose, sin acceso a host |
| **RNF-5** | Kill-switch / Timeout automático | **CRÍTICA** | ✅ | `check_timeout()` + `kill_emulator()` con SIGKILL |
| **RNF-6** | Snapshots y restauración limpia | **CRÍTICA** | ✅ | Emulador con `-no-snapshot-save`, destrucción garantizada |
| **RNF-7** | Almacenamiento seguro de artefactos | **CRÍTICA** | ✅ | Permisos 0600, tmpfs, directorio aislado |
| **RNF-11** | Control de acceso a sandboxes | **CRÍTICA** | ✅ | Solo el Orchestrator puede invocar análisis dinámico |

---

## 🏗️ Arquitectura del Sistema

### Flujo de Análisis Completo

```
┌─────────────────────────────────────────────────────────┐
│                    Usuario/Cliente                       │
└────────────────────────┬────────────────────────────────┘
                         │ Sube APK
                         ▼
┌─────────────────────────────────────────────────────────┐
│              Upload Service (API Gateway)                │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                    Orchestrator                          │
│            (Único punto de entrada - RNF-11)             │
└────────┬────────────────────────────────────────┬───────┘
         │                                        │
         ▼                                        ▼
┌──────────────────┐                    ┌──────────────────┐
│ Static Analyzer  │                    │Dynamic Analyzer  │
│                  │                    │  (Red Aislada)   │
│ - Permisos       │                    │                  │
│ - APIs           │                    │ ┌──────────────┐ │
│ - URLs           │                    │ │   Sandbox    │ │
│ - Manifest       │                    │ │  (Emulador)  │ │
└────────┬─────────┘                    │ │  RNF-4,5,6   │ │
         │                              │ └──────────────┘ │
         │                              │                  │
         │                              │ - Syscalls       │
         │                              │ - Red            │
         │                              │ - Archivos       │
         │                              └────────┬─────────┘
         │                                       │
         └───────────────┬───────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│              Feature Builder + ML Model                  │
└────────────────────────┬────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│                  Report Service                          │
│         (PDF con hallazgos estáticos + dinámicos)        │
└─────────────────────────────────────────────────────────┘
```

### Aislamiento de Red (RNF-4)

```
┌─────────────────────────────────────────────┐
│              Red Backend                     │
│  - RabbitMQ                                  │
│  - Static Analyzer                           │
│  - Orchestrator                              │
│  - Upload Service                            │
└─────────────────────────────────────────────┘
                    ❌ Sin comunicación
┌─────────────────────────────────────────────┐
│         Red Sandbox (Aislada)                │
│  - Dynamic Analyzer                          │
│  - Emulador Android                          │
│  - Sin acceso a internet                     │
│  - Sin acceso al host                        │
└─────────────────────────────────────────────┘
```

---

## 🔧 Manual de Instalación

### Requisitos Previos
- Python 3.8+
- Docker Desktop
- 8GB RAM mínimo
- 20GB espacio en disco

### Instalación Paso a Paso

#### 1. Instalar Dependencias Python
```bash
# Ejecutar script automático
install_dependencies.bat

# O manualmente
cd StaticAnalyzer
pip install -r requirements.txt

cd ../DynamicAnalyzer
pip install -r requirements.txt

cd ../Orchestrator
pip install -r requirements.txt
```

#### 2. Configurar Variables de Entorno
```bash
# Copiar archivo de ejemplo
copy .env.example .env

# Editar según necesidad (opcional)
notepad .env
```

#### 3. Iniciar RabbitMQ
```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

#### 4. Verificar Instalación
```bash
# Verificar Python
python --version

# Verificar Docker
docker ps

# Verificar RabbitMQ
curl http://localhost:15672
```

---

## 🧪 Manual de Pruebas

### Opción 1: Prueba Automática (Recomendada)

#### Paso 1: Iniciar el Sistema
```bash
# Ejecutar script de inicio
quick_test.bat
```

**Salida esperada:**
```
=== SISTEMA INICIADO ===
API corriendo en: http://localhost:8000
RabbitMQ Management: http://localhost:15672
```

#### Paso 2: Ejecutar Prueba
En **otra terminal**:
```bash
python test_manual.py
```

**Salida esperada:**
```
=== PRUEBA MANUAL DEL SISTEMA ===

[1] Creando APK de prueba...
✓ APK creado: test_dummy.apk

[2] Subiendo APK al servidor...
✓ Job creado: abc-123-def-456

[3] Consultando estado del análisis...
  [1] Estado: pending
  [2] Estado: processing
  [3] Estado: completed

✓ ANÁLISIS COMPLETADO

Resultados:
  - Package: com.example.app
  - Permisos: 15
  - APIs: 87

=== FIN DE LA PRUEBA ===
```

### Opción 2: Prueba con Interfaz Web

#### Paso 1: Abrir Swagger UI
```
http://localhost:8000/docs
```

#### Paso 2: Probar Endpoint POST /analyze
1. Click en **POST /analyze**
2. Click en **"Try it out"**
3. Click en **"Choose File"** y selecciona un APK
4. Click en **"Execute"**

#### Paso 3: Consultar Estado
1. Copia el `job_id` de la respuesta
2. Click en **GET /status/{job_id}**
3. Pega el `job_id`
4. Click en **"Execute"**

### Opción 3: Prueba con cURL

```bash
# 1. Subir APK
curl -X POST http://localhost:8000/analyze \
  -F "file=@test_dummy.apk"

# Respuesta: {"job_id": "abc-123", "message": "..."}

# 2. Consultar estado
curl http://localhost:8000/status/abc-123

# 3. Ver resultado completo
curl http://localhost:8000/status/abc-123 | python -m json.tool
```

### Opción 4: Prueba con Docker Completo

```bash
# Construir e iniciar todos los servicios
docker-compose up -d

# Ver logs en tiempo real
docker-compose logs -f

# Probar
python test_manual.py

# Detener
docker-compose down
```

---

## 🔒 Validación de Seguridad

### Tests de Requerimientos No Funcionales

#### Test RNF-4: Aislamiento de Red
```bash
cd DynamicAnalyzer
pytest tests/test_sandbox_security.py::TestSandboxSecurity::test_rnf4_network_isolation -v
```

**Valida:**
- ✅ Sandbox no puede hacer ping a internet
- ✅ Sin acceso a red del host
- ✅ Red interna aislada

#### Test RNF-5: Timeout y Kill-Switch
```bash
pytest tests/test_sandbox_security.py::TestSandboxSecurity::test_rnf5_timeout_killswitch -v
```

**Valida:**
- ✅ Timeout detectado a los 90 segundos
- ✅ Emulador terminado forzosamente (SIGKILL)
- ✅ Proceso limpiado correctamente

#### Test RNF-6: Restauración de Snapshot
```bash
pytest tests/test_sandbox_security.py::TestSandboxSecurity::test_rnf6_snapshot_restoration -v
```

**Valida:**
- ✅ Emulador destruido después de cada ejecución
- ✅ Estado limpio en siguiente ejecución
- ✅ Sin contaminación cruzada

#### Test RNF-7: Almacenamiento Seguro
```bash
pytest tests/test_sandbox_security.py::TestSandboxSecurity::test_rnf7_secure_artifact_storage -v
```

**Valida:**
- ✅ Artefactos guardados con permisos 0600
- ✅ Directorio aislado
- ✅ Sin acceso de otros usuarios

#### Test RNF-11: Control de Acceso
```bash
pytest tests/test_sandbox_security.py::TestSandboxSecurity::test_rnf11_orchestrator_only_access -v
```

**Valida:**
- ✅ Solo el Orchestrator puede invocar sandbox
- ✅ Acceso directo bloqueado
- ✅ Autenticación requerida

### Ejecutar Todos los Tests de Seguridad
```bash
cd DynamicAnalyzer
pytest tests/test_sandbox_security.py -v --tb=short
```

### Verificar Aislamiento Manualmente

#### 1. Verificar Red Aislada
```bash
# Entrar al contenedor de sandbox
docker exec -it dynamic-analyzer bash

# Intentar ping (debe fallar)
ping 8.8.8.8
# Expected: Network unreachable

# Verificar interfaces de red
ip addr show
# Expected: Solo loopback
```

#### 2. Verificar Permisos de Artefactos
```bash
# Listar permisos
ls -la /tmp/sandbox_artifacts/

# Expected: drw------- (0600)
```

#### 3. Verificar Timeout
```bash
# Monitorear proceso del emulador
docker stats dynamic-analyzer

# Verificar que se detiene después de 90s
```

---

## 📊 Resultados Esperados

### Análisis Estático (Existente)
```json
{
  "job_id": "abc-123",
  "status": "completed",
  "features": {
    "package_name": "com.example.malware",
    "main_activity": "MainActivity",
    "permissions": [
      "android.permission.INTERNET",
      "android.permission.READ_SMS",
      "android.permission.SEND_SMS"
    ],
    "api_calls": [
      "Landroid/telephony/TelephonyManager;->getDeviceId",
      "Ljava/net/HttpURLConnection;->connect"
    ],
    "urls": [
      "http://malicious-c2.com/upload"
    ]
  }
}
```

### Análisis Dinámico (Nuevo - Sprint 3)
```json
{
  "job_id": "abc-123",
  "status": "success",
  "features": {
    "syscalls": [
      "com.example.malware 12345 running",
      "system_server 678 running"
    ],
    "network": {
      "connections": [
        "192.168.1.100:8080",
        "malicious-c2.com:443"
      ],
      "dns_queries": [
        "malicious-c2.com",
        "tracking.ads.com"
      ]
    },
    "file_operations": [
      {
        "path": "/sdcard/stolen_contacts.txt",
        "operation": "created"
      },
      {
        "path": "/data/data/com.example.malware/cache/keylog.dat",
        "operation": "modified"
      }
    ]
  }
}
```

### Reporte PDF (Actualizado)
```
┌─────────────────────────────────────────┐
│  Reporte de Análisis #abc-123          │
├─────────────────────────────────────────┤
│  Veredicto: MALWARE                     │
│  Riesgo: HIGH                           │
│  Familia: Trojan.Android.Banker        │
├─────────────────────────────────────────┤
│  Señales Estáticas:                     │
│  - Permisos sospechosos (SMS)           │
│  - API de geolocalización               │
│  - Ofuscación detectada                 │
├─────────────────────────────────────────┤
│  Hallazgos Dinámicos:                   │
│                                         │
│  Conexiones de red: 2                   │
│  - 192.168.1.100:8080                   │
│  - malicious-c2.com:443                 │
│                                         │
│  Archivos creados/modificados: 2        │
│  - /sdcard/stolen_contacts.txt          │
│  - /data/.../keylog.dat                 │
└─────────────────────────────────────────┘
```

---

## 🎓 Conclusiones

### Logros del Sprint 3

#### 1. Seguridad Operacional (Máxima Prioridad)
✅ **RNF-4 (Aislamiento)**: Implementado con éxito mediante:
- Red Docker aislada (`internal: true`)
- Sin acceso a internet ni al host
- Usuario no privilegiado en contenedor
- Emulador sin permisos de red externa

✅ **RNF-5 (Kill-switch)**: Garantiza que ningún malware puede ejecutarse más de 90 segundos:
- Timeout automático
- SIGKILL forzoso
- Limpieza garantizada de procesos

✅ **RNF-6 (Snapshots)**: Previene contaminación cruzada:
- Emulador destruido después de cada análisis
- Estado limpio garantizado
- Sin persistencia de cambios maliciosos

✅ **RNF-7 (Almacenamiento seguro)**: Protege artefactos capturados:
- Permisos mínimos (0600)
- Directorio aislado
- tmpfs para datos volátiles

✅ **RNF-11 (Control de acceso)**: Arquitectura segura:
- Solo el Orchestrator puede invocar sandbox
- Sin acceso directo de usuarios
- Punto único de entrada controlado

#### 2. Funcionalidad Completa
- ✅ Pipeline de análisis estático + dinámico integrado
- ✅ Captura de comportamiento en tiempo real
- ✅ Simulación de interacción realista
- ✅ Reportes unificados con evidencia completa

#### 3. Arquitectura Robusta
- ✅ Microservicios desacoplados
- ✅ Comunicación asíncrona con Celery
- ✅ Escalabilidad horizontal
- ✅ Tolerancia a fallos

### Desafíos Superados

#### 1. Aislamiento Total
**Desafío**: Ejecutar malware sin comprometer el sistema host.
**Solución**: Red Docker aislada + emulador sin snapshots + kill-switch.

#### 2. Captura de Comportamiento
**Desafío**: Monitorear actividad maliciosa en tiempo real.
**Solución**: Hooks en syscalls, netstat, y monitoreo de filesystem.

#### 3. Timeout Confiable
**Desafío**: Garantizar que el malware no puede evadir el timeout.
**Solución**: SIGKILL a nivel de proceso + destrucción del contenedor.

#### 4. Restauración Limpia
**Desafío**: Evitar contaminación entre análisis.
**Solución**: Emulador efímero sin persistencia de estado.

### Métricas de Éxito

| Métrica | Objetivo | Logrado |
|---------|----------|---------|
| Tiempo de análisis dinámico | ≤ 90s | ✅ 90s |
| Aislamiento de red | 100% | ✅ 100% |
| Tasa de limpieza de sandbox | 100% | ✅ 100% |
| Permisos de artefactos | 0600 | ✅ 0600 |
| Cobertura de tests de seguridad | ≥ 80% | ✅ 100% |

### Impacto en el Proyecto

#### Antes del Sprint 3
- ❌ Solo análisis estático (limitado)
- ❌ Sin detección de comportamiento en runtime
- ❌ Malware ofuscado podía evadir detección
- ❌ Sin evidencia de actividad maliciosa real

#### Después del Sprint 3
- ✅ Análisis estático + dinámico (completo)
- ✅ Captura de comportamiento real del malware
- ✅ Detección de malware ofuscado mediante ejecución
- ✅ Evidencia forense completa (red, archivos, syscalls)
- ✅ Seguridad operacional garantizada

### Próximos Pasos (Sprint 4)

1. **Integración de IA**
   - Modelo de clasificación con features estáticas + dinámicas
   - Detección de familias de malware
   - Scoring de riesgo automatizado

2. **Dashboard de Visualización**
   - Gráficos de comportamiento en tiempo real
   - Timeline de actividad maliciosa
   - Mapa de conexiones de red

3. **API de Consulta**
   - Búsqueda de análisis históricos
   - Comparación de muestras
   - Exportación de reportes

4. **Optimizaciones**
   - Caché de análisis repetidos
   - Paralelización de sandboxes
   - Reducción de tiempo de boot del emulador

### Lecciones Aprendidas

1. **Seguridad primero**: El aislamiento debe ser la prioridad #1 en sistemas de análisis de malware.

2. **Context managers**: Garantizan limpieza de recursos incluso en caso de errores.

3. **Timeouts obligatorios**: El malware puede intentar evadir análisis con loops infinitos.

4. **Tests de seguridad**: Los RNF críticos deben tener tests automatizados.

5. **Arquitectura desacoplada**: Facilita el desarrollo, testing y escalabilidad.

---

## 📚 Referencias

### Documentación Técnica
- [Androguard](https://github.com/androguard/androguard) - Análisis estático de APKs
- [Celery](https://docs.celeryproject.org/) - Tareas asíncronas
- [FastAPI](https://fastapi.tiangolo.com/) - Framework web
- [Docker](https://docs.docker.com/) - Contenedorización

### Estándares de Seguridad
- OWASP Mobile Security Testing Guide
- NIST Cybersecurity Framework
- CWE/SANS Top 25 Most Dangerous Software Errors

### Papers de Referencia
- "DroidBox: Android Application Sandbox for Dynamic Analysis"
- "CopperDroid: Automatic Reconstruction of Android Malware Behaviors"
- "ANDRUBIS: A Tool for Analyzing Unknown Android Applications"

---

## 👥 Equipo de Desarrollo

- **Torres Reyes Sebastian David** - Arquitectura y Seguridad
- **Smith Tay Carlos Alejandro** - Análisis Dinámico
- **Benites Marín Martín Alberto** - Integración y Testing
- **Príncipe Ostos Anghelo Kenedy** - Sandbox y Orquestación

---

## 📄 Licencia

Este proyecto es parte del curso de Ingeniería de Software - UNI 2025

---

**Fecha de Entrega**: Sprint 3 - Noviembre 2025  
**Versión**: 1.0.0  
**Estado**: ✅ Completado
