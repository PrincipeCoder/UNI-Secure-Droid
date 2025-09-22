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
- RF-4.3, RF-4.4, RF-4.5, RF-4.6
- RNF-4.1, RNF-4.2, RNF-4.4, RNF-4.5, RNF-4.8