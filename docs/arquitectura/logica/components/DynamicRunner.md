# UploadService

## Responsabilidad
Ejecuta el APK dentro de un entorno aislado (sandbox Android x86) cuando el modelo no alcanza un nivel de confianza suficiente.  
Monitorea llamadas al sistema, tráfico de red, uso de SMS, registros y cualquier acción relevante.  
Devuelve un conjunto de `dynamic_features` para combinarlos con los datos estáticos.

## Entradas / Salidas (DTO)
- Entrada: `{ "job_id": "abc123", "apk_path": "object://bucket/apk/abc123.apk" }`  
- Salida: `{ "job_id": "abc123", "dynamic_features": { "network_calls": [], "sms_sent": 0 } }`

## Dependencias
- ObjectStore  
- MetadataDB  
- FeatureBuilder    

## Errores y Timeouts
| Caso                                           | Manejo                                         |
|------------------------------------------------|------------------------------------------------|
| Fallo al iniciar VM/sandbox                    | Marcar job como error, registrar detalle.      |
| Comportamiento no detectado o app no inicia    | Devolver solo metadatos mínimos.               |
| Timeout >90 s                                  | Cancelar ejecución y notificar a ModelService. |
 
## RF / RNF soportados
- RF-08, RF-09, RF-10, RF-11, RF-12
- RNF-3, RNF-2 