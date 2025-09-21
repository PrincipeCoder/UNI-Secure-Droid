# ReportService

## Responsabilidad
Genera un reporte HTML/PDF con el resultado del análisis.  
Muestra veredicto, nivel de riesgo, familia de malware y señales más relevantes.  
Permite descargar el reporte o verlo en el navegador.

## Entradas / Salidas (DTO)
- Entrada: `{ job_id, verdict, risk, family?, top_signals[] }`  
- Salida: archivo PDF o JSON

## Dependencias
- MetadataDB 
- ObjectStore 

## Errores y Timeouts
| Caso                                                            | Manejo                                                                               |
|-----------------------------------------------------------------|--------------------------------------------------------------------------------------|
| Falta información del job (no existe o no completó el análisis) | 404                                                                                  |
| Timeout de renderizado (>2 s)                                   | Devolver un JSON básico con `verdict`, `risk` y `top_signals`, sin generar PDF/HTML. |
| Fallo interno al crear plantilla                                | 500                                                                                  |

## RF / RNF soportados
- RF-17, RF-18, RF-19  
- RNF-6, RNF-7