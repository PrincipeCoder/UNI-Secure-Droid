# ObjectStore

## Responsabilidad
Almacena artefactos del análisis: archivos APK, capturas de red, reportes PDF/HTML y otros datos binarios.  
Garantiza disponibilidad, cifrado en reposo y control de versiones básicos.

## Entradas / Salidas (DTO)
- Entrada: `{ "job_id": "abc123", "file": "<binary>" }`  
- Salida: `{ "object_path": "object://bucket/apk/abc123.apk" }`

## Dependencias 
- UploadService
- StaticAnalyzer, DynamicRunner, ReportService (lectores/escritores)

## Errores y Timeouts
| Caso                                     | Manejo                       |
|------------------------------------------|------------------------------|
| Error de conexión o espacio insuficiente | 500 Internal Server Error    |
| Archivo no encontrado                    | 404 Not Found                |
| Latencia alta en lectura                 | Reintento automático (máx 2) |
 
## RF / RNF soportados
- RF-22 
- RNF-5