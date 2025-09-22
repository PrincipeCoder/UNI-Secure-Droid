# FeatureBuilder

## Responsabilidad
Fusiona características estáticas y dinámicas.  
Normaliza, filtra y selecciona las más relevantes (ej. Random Forest importance).  
Entrega un vector listo para el modelo de IA.

## Entradas / Salidas (DTO)
- Entrada: `{ "job_id": "...", "static_features": {...}, "dynamic_features": {...}? }`  
- Salida: `{ "job_id": "...", "vector": [0.1,0.5,...] }`

## Dependencias
- StaticAnalyzer 
- DynamicRunner (opcional)  
- ModelService

## Errores y Timeouts
| Caso                                                 | Manejo                                                                 |
|------------------------------------------------------|------------------------------------------------------------------------|
| Faltan datos dinámicos (`dynamic_features` ausentes) | Continuar sólo con `static_features` (modo MVP).                       |
| Vector incompleto o con valores inválidos            | Validar integridad; si falla, registrar error y marcar job en `error`. |
| Tiempo de procesamiento >3 s                         | Registrar alerta y devolver mensaje de error.                          |

## RF / RNF soportados
- RF-13, RF-14