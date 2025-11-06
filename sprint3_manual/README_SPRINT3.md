# Sprint 3: Sandbox y Análisis Dinámico

## Implementación Completada

### Módulos Implementados

#### 1. DynamicAnalyzer/
- **analyzer.py**: Tarea Celery principal para análisis dinámico
- **sandbox_manager.py**: Gestión del emulador aislado (RF-14, RNF-4, RNF-5, RNF-6)
- **behavior_monitor.py**: Captura de comportamiento (RF-15, RF-16, RF-17, RNF-7)
- **interaction_simulator.py**: Simulación de interacción (RF-18)
- **main.py**: API FastAPI para el servicio
- **config.py**: Configuración centralizada

#### 2. Orchestrator/
- **orchestrator.py**: Coordina análisis estático y dinámico (RNF-11)

#### 3. Actualización de Servicios
- **report_service.py**: Actualizado para incluir hallazgos dinámicos (RF-28, RF-29)

### Requerimientos Funcionales Implementados

✅ **RF-14**: Configurar emulador aislado (sandbox)
- SandboxManager gestiona el ciclo de vida del emulador
- Aislamiento de red configurado en docker-compose

✅ **RF-15**: Capturar llamadas al sistema
- BehaviorMonitor.capture_syscalls()

✅ **RF-16**: Capturar tráfico de red
- BehaviorMonitor.capture_network_traffic()

✅ **RF-17**: Capturar operaciones de archivos
- BehaviorMonitor.capture_file_operations()

✅ **RF-18**: Simular interacción
- InteractionSimulator con taps, swipes, navegación

✅ **RF-19**: Abortar sandbox de forma segura
- Context manager garantiza limpieza
- Kill-switch implementado

✅ **RF-28, RF-29**: Reporte con hallazgos dinámicos
- PDF actualizado con conexiones de red y archivos

### Requerimientos No Funcionales Implementados

✅ **RNF-2**: Tiempo de análisis dinámico ≤ 90s
- DYNAMIC_ANALYSIS_TIMEOUT = 90

✅ **RNF-4**: Aislamiento (CRÍTICO)
- Red aislada en docker-compose (internal: true)
- Sin acceso a red del host
- Usuario no privilegiado en contenedor

✅ **RNF-5**: Kill-switch / Timeout
- SandboxManager.check_timeout()
- SandboxManager.kill_emulator() con SIGKILL

✅ **RNF-6**: Snapshots/Restauración
- Emulador con -no-snapshot-save
- Destrucción y recreación garantizada

✅ **RNF-7**: Almacenamiento seguro
- Artefactos en /tmp/sandbox_artifacts
- Permisos 0600
- tmpfs en docker-compose

✅ **RNF-11**: Control de acceso a sandboxes
- Solo el Orchestrator puede invocar análisis dinámico
- Endpoint protegido

## Arquitectura de Seguridad

```
┌─────────────────────────────────────────────────┐
│              Orchestrator                       │
│         (Único punto de entrada)                │
└────────────┬────────────────────────────────────┘
             │
    ┌────────┴────────┐
    │                 │
    ▼                 ▼
┌─────────┐    ┌──────────────────────┐
│ Static  │    │  Dynamic Analyzer    │
│Analyzer │    │   (Red Aislada)      │
└─────────┘    └──────────────────────┘
                      │
                      ▼
               ┌──────────────┐
               │   Sandbox    │
               │ (Sin acceso  │
               │  a internet) │
               └──────────────┘
```

## Uso

### Iniciar el sistema completo:
```bash
docker-compose up -d
```

### Ejecutar análisis completo:
```python
from Orchestrator.orchestrator import orchestrate_analysis

result = orchestrate_analysis.delay(
    job_id="job_123",
    apk_path="/tmp/apk_storage/malware.apk"
)
```

### Ejecutar tests de seguridad:
```bash
cd DynamicAnalyzer
pytest tests/test_sandbox_security.py -v
```

## Validación de Seguridad

Los tests validan:
- RNF-4: Aislamiento de red
- RNF-5: Timeout y kill-switch
- RNF-6: Restauración de estado
- RNF-7: Permisos de artefactos
- RNF-11: Control de acceso

## Próximos Pasos (Sprint 4)

- Integrar modelo de IA para clasificación
- Dashboard de visualización
- API de consulta de reportes
