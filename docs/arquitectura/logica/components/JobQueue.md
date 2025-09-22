# JobQueue

## Responsabilidad
Orquesta los trabajos de análisis en segundo plano.  
Mantiene colas separadas (`static_jobs`, `dynamic_jobs`) y entrega tareas a los workers disponibles.  
Permite reintentos con backoff y notifica estados al MetadataDB.

## Entradas / Salidas (DTO)
- Entrada: `{ "job_id": "abc123", "phase": "static", "payload": {...} }`  
- Salida: `{ "ack": true }`

## Dependencias 
- UploadService, StaticAnalyzer, DynamicRunner (productores/consumidores)
- MetadataDB (persistencia de estado)

## Errores y Timeouts
| Caso                | Manejo                                         |
|---------------------|------------------------------------------------|
| Cola saturada       | 429 Too Many Requests; reintentar con backoff  |
| Worker no responde  | Reintentar (máx 3) o marcar job error          |
| Timeout en lectura  | Registrar métrica y continuar                  |
 
## RF / RNF soportados
- RF-21 
- RNF-10 