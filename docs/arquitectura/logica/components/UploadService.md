# UploadService

## Responsabilidad
Recibe el archivo APK desde la interfaz web, valida extensión y tamaño (≤50 MB), calcula su hash SHA-256 y genera un `job_id`.  
Guarda los metadatos iniciales en **MetadataDB** y sube el binario al **ObjectStore**.  
Finalmente, encola la tarea en **JobQueue** para el análisis estático.

## Entradas / Salidas (DTO)
- Entrada: `{ file: <binary APK> }`  
- Salida: `{ job_id, status: "queued" }`

## Dependencias
- ObjectStore  
- MetadataDB  
- JobQueue

## Errores y Timeouts
| Caso                               | Manejo             |
|------------------------------------|--------------------|
| Formato inválido o >50 MB          | 400 / 413          |
| Error al guardar en ObjectStore    | 500                |
| Timeout de hash (>5 s)             | marca job en error |

## RF / RNF soportados
- RF-01 
- RNF-1, RNF-2 
