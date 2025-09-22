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
- RF-3.1, RF-3.2, RF-3.3, RF-3.4, RF-3.5, RF-3.6
- RNF-1.2, RNF-2.1, RNF-2.2, RNF-2.3, RNF-2.4