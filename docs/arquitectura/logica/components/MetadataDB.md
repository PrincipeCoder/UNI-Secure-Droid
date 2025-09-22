# MetadataDB

## Responsabilidad
Base de datos que almacena metadatos y resultados del análisis.  
Registra estados (`queued`, `static`, `dynamic`, `done`, `error`), logs, hashes, scores y rutas de archivos.

## Entradas / Salidas (DTO)
- Entrada: `{ "job_id": "abc123", "status": "done", "score": 0.98 }`  
- Salida: `{ "ack": true }`

## Dependencias 
- UploadService, StaticAnalyzer, DynamicRunner, ModelService, ReportService
- JobQueue (actualiza estados)

## Errores y Timeouts
| Caso                       | Manejo                               |
|----------------------------|--------------------------------------|
| Error al escribir registro | Reintento (máx 2) o marcar job error |
| Consulta sin resultados    | 404 Not Found                        |
| Latencia >100 ms p95       | Registrar métrica                    |
 
## RF / RNF soportados
- RF-23 
- RNF-11