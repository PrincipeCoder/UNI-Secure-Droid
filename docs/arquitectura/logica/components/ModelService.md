# ModelService

## Responsabilidad
Recibe el vector de características y aplica el modelo entrenado (Random Forest / Bagging / DBN).  
Devuelve veredicto binario (“benigno” | “malicioso”) y, si procede, la familia de malware.  
Si la confianza es baja, solicita ejecución dinámica al **DynamicRunner**.

## Entradas / Salidas (DTO)
- Entrada: `{ job_id, vector }`  
- Salida: `{ job_id, verdict, risk, family?, score }`

## Dependencias
- FeatureBuilder  
- DynamicRunner (cuando score < umbral)  
- MetadataDB

## Errores y Timeouts
| Caso                      | Manejo                 |
|---------------------------|------------------------|
| Modelo no disponible      | 503                    |
| Tiempo de inferencia >5 s | marca job como `error` |

## RF / RNF soportados
- RF-15, RF-16  
- RNF-8, RNF-13, RNF-14