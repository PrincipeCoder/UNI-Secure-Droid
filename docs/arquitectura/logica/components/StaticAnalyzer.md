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
- RF-02, RF-03, RF-04, RF-05, RF-06, RF-07
- RNF-1, RNF-2 