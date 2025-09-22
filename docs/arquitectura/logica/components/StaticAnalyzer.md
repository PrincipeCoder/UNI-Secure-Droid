# StaticAnalyzer

## Responsabilidad
Descompila el APK con herramientas como APKTool/Androguard.  
Extrae permisos, llamadas a API, intents, URLs, opcodes y otros indicadores estáticos.  
Entrega un JSON de características al **FeatureBuilder**.

## Entradas / Salidas (DTO)
- Entrada: `{ job_id, object_path }`  
- Salida: `{ job_id, static_features: {...} }`

## Dependencias
- ObjectStore  
- MetadataDB  
- FeatureBuilder

## Errores y Timeouts
| Caso                    | Manejo                   |
|-------------------------|--------------------------|
| Fallo en descompilación | marca job `error`        |
| Archivo corrupto        | 422                      |
| Timeout >15 s           | aborta análisis estático |

## RF / RNF soportados
- RF-2.1, RF-2.2, RF-2.3, RF-2.4, RF-2.5, RF-2.6, RF-2.7 
- RNF-1.1, RNF-4.3, RNF-4.7