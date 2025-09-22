# AuthZService

## Responsabilidad
Gestiona autenticación y autorización para los usuarios del sistema.  
Controla roles (`user`, `analyst`, `admin`) y scopes para acceder a reportes o lanzar análisis.

## Entradas / Salidas (DTO)
- Entrada: `{ "username": "juan", "password": "****" }`  
- Salida: `{ "access_token": "jwt...", "roles": ["analyst"] }`

## Dependencias 
- MetadataDB (opcional, si guarda usuarios/roles).
- UploadService, ReportService, API (consumidores)   

## Errores y Timeouts
| Caso                               | Manejo                    |
|------------------------------------|---------------------------|
| Credenciales inválidas             | 401 Unauthorized          |
| Usuario sin permisos para recurso  | 403 Forbidden             |
| Error interno                      | 500 Internal Server Error |
 
## RF / RNF soportados
- RF-20 
- RNF-3, RNF-4