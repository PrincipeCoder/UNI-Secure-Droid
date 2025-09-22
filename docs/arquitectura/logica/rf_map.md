# Mapeo de Requisitos Funcionales y No Funcionales → Componentes


| Componente          | RF cubiertos                             | RNF cubiertos                                                            |
|---------------------|------------------------------------------|--------------------------------------------------------------------------|
| **UploadService**   | RF-01                                    | RNF-1 (rendimiento en carga), RNF-2 (uso memoria)                        |
| **StaticAnalyzer**  | RF-02, RF-03, RF-04, RF-05, RF-06, RF-07 | RNF-1 (rendimiento ≤15 s), RNF-2 (uso memoria)                           |
| **DynamicRunner**   | RF-08, RF-09, RF-10, RF-11, RF-12        | RNF-3 (sandbox), RNF-2 (uso memoria)                                     |
| **FeatureBuilder**  | RF-13, RF-14                             |                                                                          |
| **ModelService**    | RF-15, RF-16                             | RNF-8 (falsos negativos), RNF-13 (actualizar modelos), RNF-14 (datasets) |
| **ReportService**   | RF-17, RF-18, RF-19                      | RNF-6 (UI: niveles de riesgo), RNF-7 (usabilidad)                        |
| **AuthZService**    | RF-20                                    | RNF-3 (seguridad acceso), RNF-4 (privacidad)                             |
| **JobQueue**        | RF-21                                    | RNF-10 (reintentos en colas)                                             |
| **ObjectStore**     | RF-22                                    | RNF-5 (cifrado en reposo)                                                |
| **MetadataDB**      | RF-23                                    | RNF-11 (integridad datos)                                                |
