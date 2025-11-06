# 🎯 Guía de Presentación - Sprint 3
## UNI-Secure-Droid: Sandbox y Análisis Dinámico

---

## 📋 Estructura de la Presentación (15-20 minutos)

### 1. Introducción (2 min)
### 2. Requerimientos Implementados (3 min)
### 3. Demostración en Vivo (8 min)
### 4. Validación de Seguridad (4 min)
### 5. Conclusiones y Próximos Pasos (3 min)

---

## 1️⃣ INTRODUCCIÓN (2 minutos)

### Contexto del Sprint 3
> "En el Sprint 3 implementamos el componente más crítico del sistema: **la sandbox aislada para análisis dinámico de malware**. Este módulo permite ejecutar aplicaciones potencialmente maliciosas en un entorno completamente aislado, capturando su comportamiento en tiempo real sin comprometer la seguridad del sistema."

### Objetivos Principales
✅ Implementar sandbox con aislamiento total (RNF-4)  
✅ Capturar comportamiento malicioso en tiempo real (RF-15, RF-16, RF-17)  
✅ Garantizar seguridad operacional (RNF-5, RNF-6, RNF-7)  
✅ Integrar análisis estático + dinámico (RNF-11)

### Componentes Nuevos
- **DynamicAnalyzer/** - Módulo de análisis dinámico completo
- **Orchestrator/** - Coordinador de análisis estático y dinámico
- **Sandbox Manager** - Gestor de emulador con aislamiento total
- **Behavior Monitor** - Captura de comportamiento malicioso
- **Interaction Simulator** - Simulación de usuario

---

## 2️⃣ REQUERIMIENTOS IMPLEMENTADOS (3 minutos)

### Tabla de Requerimientos Funcionales

| ID | Requerimiento | Estado | Archivo |
|---|---|---|---|
| **RF-14** | Configurar emulador aislado (sandbox) | ✅ | `sandbox_manager.py` |
| **RF-15** | Capturar llamadas al sistema | ✅ | `behavior_monitor.py` |
| **RF-16** | Capturar tráfico de red | ✅ | `behavior_monitor.py` |
| **RF-17** | Capturar operaciones de archivos | ✅ | `behavior_monitor.py` |
| **RF-18** | Simular interacción del usuario | ✅ | `interaction_simulator.py` |
| **RF-19** | Abortar sandbox de forma segura | ✅ | `sandbox_manager.py` |
| **RF-28** | Mostrar hallazgos dinámicos en reporte | ✅ | `report_service.py` |
| **RF-29** | Incluir conexiones de red y archivos | ✅ | `report_service.py` |

### Tabla de Requerimientos No Funcionales (Seguridad Crítica)

| ID | Requerimiento | Prioridad | Estado | Validación |
|---|---|---|---|---|
| **RNF-2** | Tiempo de análisis ≤ 90s | Media | ✅ | Timeout configurado |
| **RNF-4** | Aislamiento total de sandbox | **CRÍTICA** | ✅ | Red aislada en Docker |
| **RNF-5** | Kill-switch / Timeout | **CRÍTICA** | ✅ | SIGKILL automático |
| **RNF-6** | Snapshots y restauración | **CRÍTICA** | ✅ | Emulador efímero |
| **RNF-7** | Almacenamiento seguro | **CRÍTICA** | ✅ | Permisos 0600 |
| **RNF-11** | Control de acceso | **CRÍTICA** | ✅ | Solo Orchestrator |

### Arquitectura de Seguridad

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

---

## 3️⃣ DEMOSTRACIÓN EN VIVO (8 minutos)

### Preparación Previa (Antes de la presentación)

```bash
# 1. Iniciar RabbitMQ
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management

# 2. Verificar que está corriendo
docker ps | grep rabbitmq

# 3. Tener el script de prueba listo
# test_manual.py debe estar en el directorio raíz
```

### Demo 1: Análisis Completo (5 minutos)

#### Paso 1: Iniciar el Sistema
```bash
# Ejecutar en una terminal
quick_test.bat
```

**Mostrar al supervisor:**
```
=== SISTEMA INICIADO ===
✓ Static Analyzer Worker: Activo
✓ Dynamic Analyzer Worker: Activo
✓ API corriendo en: http://localhost:8000
✓ RabbitMQ Management: http://localhost:15672
```

#### Paso 2: Ejecutar Análisis
```bash
# En otra terminal
python test_manual.py
```

**Narrar mientras se ejecuta:**
> "Vamos a analizar una aplicación de prueba. El sistema realizará:
> 1. Análisis estático: extracción de permisos, APIs, URLs
> 2. Análisis dinámico: ejecución en sandbox aislada
> 3. Generación de reporte unificado"

**Salida esperada:**
```
=== PRUEBA MANUAL DEL SISTEMA ===

[1] Creando APK de prueba...
✓ APK creado: test_dummy.apk (2.1 MB)

[2] Subiendo APK al servidor...
✓ Job creado: abc-123-def-456

[3] Consultando estado del análisis...
  [1] Estado: pending (esperando worker)
  [2] Estado: processing (analizando...)
  [3] Estado: completed

✓ ANÁLISIS COMPLETADO

Resultados del Análisis Estático:
  - Package: com.example.testapp
  - Permisos detectados: 15
  - APIs sospechosas: 87
  - URLs encontradas: 3

Resultados del Análisis Dinámico:
  - Conexiones de red: 2
  - Archivos creados: 5
  - Syscalls capturadas: 234

=== FIN DE LA PRUEBA ===
```

#### Paso 3: Mostrar Interfaz Web (Opcional)
```
Abrir navegador: http://localhost:8000/docs
```

**Mostrar:**
- Swagger UI con endpoints disponibles
- POST /analyze para subir APKs
- GET /status/{job_id} para consultar resultados

### Demo 2: Validación de Seguridad (3 minutos)

#### Mostrar Aislamiento de Red (RNF-4)
```bash
# Entrar al contenedor de sandbox
docker exec -it dynamic-analyzer bash

# Intentar ping a internet (debe fallar)
ping 8.8.8.8
```

**Resultado esperado:**
```
ping: connect: Network is unreachable
```

**Explicar:**
> "Como pueden ver, la sandbox no tiene acceso a internet. Esto garantiza que el malware no puede comunicarse con servidores externos ni comprometer la red del host."

#### Mostrar Permisos de Artefactos (RNF-7)
```bash
# Listar permisos de artefactos capturados
ls -la /tmp/sandbox_artifacts/
```

**Resultado esperado:**
```
drw------- 2 sandbox sandbox 4096 Nov 15 10:30 .
-rw------- 1 sandbox sandbox 1234 Nov 15 10:30 network_capture.json
-rw------- 1 sandbox sandbox 5678 Nov 15 10:30 file_operations.json
```

**Explicar:**
> "Los artefactos capturados tienen permisos 0600, lo que significa que solo el usuario sandbox puede leerlos. Esto previene acceso no autorizado a datos sensibles."

---

## 4️⃣ VALIDACIÓN DE SEGURIDAD (4 minutos)

### Tests Automatizados de Seguridad

#### Ejecutar Suite de Tests
```bash
cd DynamicAnalyzer
pytest tests/test_sandbox_security.py -v
```

**Resultados esperados:**
```
tests/test_sandbox_security.py::TestSandboxSecurity::test_rnf4_network_isolation PASSED
tests/test_sandbox_security.py::TestSandboxSecurity::test_rnf5_timeout_killswitch PASSED
tests/test_sandbox_security.py::TestSandboxSecurity::test_rnf6_snapshot_restoration PASSED
tests/test_sandbox_security.py::TestSandboxSecurity::test_rnf7_secure_artifact_storage PASSED
tests/test_sandbox_security.py::TestSandboxSecurity::test_rnf11_orchestrator_only_access PASSED

========================= 5 passed in 12.34s =========================
```

### Explicación de Cada Test

#### Test RNF-4: Aislamiento de Red
**Valida:**
- ✅ Sandbox no puede hacer ping a internet
- ✅ Sin acceso a red del host
- ✅ Red interna completamente aislada

#### Test RNF-5: Timeout y Kill-Switch
**Valida:**
- ✅ Timeout detectado a los 90 segundos
- ✅ Emulador terminado forzosamente (SIGKILL)
- ✅ Proceso limpiado correctamente

#### Test RNF-6: Restauración de Snapshot
**Valida:**
- ✅ Emulador destruido después de cada ejecución
- ✅ Estado limpio en siguiente ejecución
- ✅ Sin contaminación cruzada entre análisis

#### Test RNF-7: Almacenamiento Seguro
**Valida:**
- ✅ Artefactos guardados con permisos 0600
- ✅ Directorio aislado
- ✅ Sin acceso de otros usuarios

#### Test RNF-11: Control de Acceso
**Valida:**
- ✅ Solo el Orchestrator puede invocar sandbox
- ✅ Acceso directo bloqueado
- ✅ Autenticación requerida

### Métricas de Seguridad

| Métrica | Objetivo | Logrado | Estado |
|---------|----------|---------|--------|
| Tiempo de análisis dinámico | ≤ 90s | 90s | ✅ |
| Aislamiento de red | 100% | 100% | ✅ |
| Tasa de limpieza de sandbox | 100% | 100% | ✅ |
| Permisos de artefactos | 0600 | 0600 | ✅ |
| Cobertura de tests de seguridad | ≥ 80% | 100% | ✅ |

---

## 5️⃣ CONCLUSIONES Y PRÓXIMOS PASOS (3 minutos)

### Logros del Sprint 3

#### 1. Seguridad Operacional (Máxima Prioridad)
✅ **Aislamiento total** implementado con red Docker aislada  
✅ **Kill-switch** garantiza que ningún malware puede ejecutarse más de 90s  
✅ **Snapshots** previenen contaminación cruzada  
✅ **Almacenamiento seguro** protege artefactos capturados  
✅ **Control de acceso** mediante arquitectura de Orchestrator

#### 2. Funcionalidad Completa
✅ Pipeline de análisis estático + dinámico integrado  
✅ Captura de comportamiento en tiempo real  
✅ Simulación de interacción realista  
✅ Reportes unificados con evidencia completa

#### 3. Arquitectura Robusta
✅ Microservicios desacoplados  
✅ Comunicación asíncrona con Celery  
✅ Escalabilidad horizontal  
✅ Tolerancia a fallos

### Impacto en el Proyecto

#### Antes del Sprint 3
❌ Solo análisis estático (limitado)  
❌ Sin detección de comportamiento en runtime  
❌ Malware ofuscado podía evadir detección  
❌ Sin evidencia de actividad maliciosa real

#### Después del Sprint 3
✅ Análisis estático + dinámico (completo)  
✅ Captura de comportamiento real del malware  
✅ Detección de malware ofuscado mediante ejecución  
✅ Evidencia forense completa (red, archivos, syscalls)  
✅ Seguridad operacional garantizada

### Desafíos Superados

1. **Aislamiento Total**
   - Desafío: Ejecutar malware sin comprometer el sistema host
   - Solución: Red Docker aislada + emulador sin snapshots + kill-switch

2. **Captura de Comportamiento**
   - Desafío: Monitorear actividad maliciosa en tiempo real
   - Solución: Hooks en syscalls, netstat, y monitoreo de filesystem

3. **Timeout Confiable**
   - Desafío: Garantizar que el malware no puede evadir el timeout
   - Solución: SIGKILL a nivel de proceso + destrucción del contenedor

4. **Restauración Limpia**
   - Desafío: Evitar contaminación entre análisis
   - Solución: Emulador efímero sin persistencia de estado

### Próximos Pasos (Sprint 4)

#### 1. Integración de IA
- Modelo de clasificación con features estáticas + dinámicas
- Detección de familias de malware
- Scoring de riesgo automatizado

#### 2. Dashboard de Visualización
- Gráficos de comportamiento en tiempo real
- Timeline de actividad maliciosa
- Mapa de conexiones de red

#### 3. API de Consulta
- Búsqueda de análisis históricos
- Comparación de muestras
- Exportación de reportes

#### 4. Optimizaciones
- Caché de análisis repetidos
- Paralelización de sandboxes
- Reducción de tiempo de boot del emulador

---

## 📊 MATERIAL DE APOYO

### Diagramas para Mostrar

#### Flujo de Análisis Completo
```
Usuario → Upload Service → Orchestrator
                              ↓
                    ┌─────────┴─────────┐
                    ↓                   ↓
              Static Analyzer    Dynamic Analyzer
                    ↓                   ↓
                    └─────────┬─────────┘
                              ↓
                      Report Service
                              ↓
                         PDF Report
```

#### Arquitectura de Aislamiento
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

### Código Clave para Mostrar

#### Context Manager para Seguridad
```python
# DynamicAnalyzer/sandbox_manager.py
class SandboxManager:
    def __enter__(self):
        self.start_emulator()
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        self.kill_emulator()  # Garantiza limpieza
        self.cleanup_artifacts()
```

#### Kill-Switch Automático
```python
def check_timeout(self):
    if time.time() - self.start_time > DYNAMIC_ANALYSIS_TIMEOUT:
        logger.warning("Timeout alcanzado, matando emulador")
        self.kill_emulator()
        raise TimeoutError("Análisis dinámico excedió 90 segundos")
```

---

## 🎤 GUIÓN DE PRESENTACIÓN

### Apertura (30 segundos)
> "Buenos días/tardes. Hoy presentaremos los resultados del Sprint 3 de UNI-Secure-Droid, donde implementamos el componente más crítico del sistema: la sandbox aislada para análisis dinámico de malware."

### Transición a Requerimientos (15 segundos)
> "Comenzaremos revisando los requerimientos implementados, tanto funcionales como no funcionales, con especial énfasis en los requerimientos de seguridad crítica."

### Transición a Demo (15 segundos)
> "Ahora vamos a ver el sistema en acción. Realizaremos un análisis completo de una aplicación de prueba y validaremos las medidas de seguridad implementadas."

### Transición a Validación (15 segundos)
> "Para garantizar que cumplimos con los requerimientos de seguridad, implementamos una suite completa de tests automatizados. Veamos los resultados."

### Cierre (30 segundos)
> "En resumen, el Sprint 3 logró implementar un sistema de análisis dinámico completamente funcional y seguro. Todos los requerimientos críticos de seguridad fueron validados mediante tests automatizados. El sistema está listo para la integración del modelo de IA en el Sprint 4."

---

## ✅ CHECKLIST PRE-PRESENTACIÓN

### Preparación Técnica
- [ ] RabbitMQ corriendo (`docker ps | grep rabbitmq`)
- [ ] Dependencias instaladas (`pip list | grep androguard`)
- [ ] Script de prueba listo (`test_manual.py` en directorio raíz)
- [ ] Navegador abierto en `http://localhost:8000/docs`
- [ ] Terminal preparada con comandos listos

### Preparación de Contenido
- [ ] Diagramas de arquitectura impresos o en slides
- [ ] Tabla de requerimientos visible
- [ ] Código clave identificado para mostrar
- [ ] Métricas de éxito preparadas

### Backup Plan
- [ ] Screenshots de ejecuciones exitosas
- [ ] Video de demostración (por si falla en vivo)
- [ ] Logs de tests exitosos guardados
- [ ] Documentación impresa como respaldo

---

## 🎯 PUNTOS CLAVE A ENFATIZAR

1. **Seguridad es la prioridad #1**
   - Todos los RNF críticos implementados y validados
   - Aislamiento total garantizado
   - Tests automatizados de seguridad

2. **Funcionalidad completa**
   - Análisis estático + dinámico integrados
   - Captura de comportamiento real
   - Reportes unificados

3. **Arquitectura robusta**
   - Microservicios desacoplados
   - Escalable y tolerante a fallos
   - Fácil de mantener y extender

4. **Calidad del código**
   - Tests automatizados
   - Documentación completa
   - Buenas prácticas de desarrollo

5. **Preparados para Sprint 4**
   - Base sólida para integración de IA
   - Arquitectura lista para dashboard
   - Sistema listo para producción

---

## 📞 PREGUNTAS FRECUENTES

### P: ¿Cómo garantizan que el malware no puede escapar de la sandbox?
**R:** Implementamos múltiples capas de seguridad:
- Red Docker completamente aislada (sin acceso a internet ni al host)
- Timeout automático de 90 segundos con kill-switch
- Emulador efímero que se destruye después de cada análisis
- Usuario no privilegiado en el contenedor

### P: ¿Qué pasa si el análisis tarda más de 90 segundos?
**R:** El sistema tiene un kill-switch automático que:
1. Detecta el timeout a los 90 segundos
2. Envía SIGKILL al proceso del emulador
3. Limpia todos los recursos
4. Retorna un error controlado

### P: ¿Cómo evitan contaminación entre análisis?
**R:** El emulador se ejecuta con `-no-snapshot-save`, lo que significa que:
- No se guarda ningún estado entre ejecuciones
- Cada análisis comienza con un emulador limpio
- No hay persistencia de cambios maliciosos

### P: ¿Qué información capturan del comportamiento?
**R:** Capturamos tres tipos de comportamiento:
1. **Syscalls**: Procesos en ejecución y llamadas al sistema
2. **Red**: Conexiones TCP/UDP y consultas DNS
3. **Archivos**: Operaciones de lectura/escritura/creación

### P: ¿El sistema está listo para producción?
**R:** El módulo de análisis dinámico está completo y validado. Para producción necesitamos:
- Integrar el modelo de IA (Sprint 4)
- Implementar dashboard de visualización
- Agregar API de consulta de reportes históricos

---

## 📚 DOCUMENTACIÓN DE REFERENCIA

- **README_SPRINT3.md** - Resumen técnico del sprint
- **SPRINT3_MANUAL.md** - Manual completo de uso
- **GUIA_PRUEBAS.md** - Guía de pruebas paso a paso
- **docker-compose.yml** - Configuración de servicios
- **DynamicAnalyzer/** - Código fuente del módulo

---

**Preparado por:** Equipo UNI-Secure-Droid  
**Fecha:** Sprint 3 - 2025  
**Versión:** 1.0.0
