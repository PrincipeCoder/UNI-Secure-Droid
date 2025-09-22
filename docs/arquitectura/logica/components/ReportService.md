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
- RF-5.1, RF-5.2, RF-5.3, RF-5.4, RF-5.5  
- RNF-4.6, RNF-3.5, RNF-3.6  