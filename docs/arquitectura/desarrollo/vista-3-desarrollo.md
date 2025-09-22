# Vista 3 — Desarrollo

### **Dueño:** Persona C  
### **Objetivo:** Definir estructura del repositorio, módulos, dependencias, estándares de código, pruebas y versionado.

---

## 1. Árbol de carpetas

ui/ # Frontend / interfaz gráfica

api/ # Endpoints de la API (FastAPI u otro framework)

analyzers/

static/ # Análisis estático de binarios

dynamic/ # Análisis dinámico (sandbox, emulación)

ml/ # Modelos de machine learning (detección)

reporting/ # Generación de reportes PDF/JSON

infra/ # Infraestructura (Docker, Kubernetes, CI/CD)

docs/ # Documentación

tests/ # Pruebas unitarias e integraciones


---

## 2. Convenciones de nombres
- **Archivos y módulos:** `snake_case` (ej. `file_utils.py`)
- **Clases:** `PascalCase` (ej. `JobManager`)
- **Funciones y variables:** `snake_case` (ej. `process_file()`)
- **Constantes:** `MAYUSCULAS` (ej. `MAX_RETRIES`)
- **Ramas:**  
  - `feature/nombre-feature`  
  - `fix/bug-descripcion`  
  - `docs/actualizacion-docs`  

---

## 3. Estándares de código y herramientas
- Lenguaje: **Python 3.11**
- Estándares: **PEP8 + type hints**
- Linters: `flake8`, `ruff`
- Formateador: `black`
- Orden de imports: `isort`
- Pre-commit hooks definidos en `.pre-commit-config.yaml`
- Dependencias fijas en `requirements.txt` o `pyproject.toml`
- Branching: **Git Flow simplificado** (`main`, `develop`, `feature/*`, `hotfix/*`)

---

## 4. Política de pruebas
- **Unitarias:** en `tests/unit/`
- **Integración:** en `tests/integration/`
- **E2E (opcional):** en `tests/e2e/`
- Framework: `pytest`
- Umbral mínimo de cobertura: **>= 75%**
- CI ejecuta: *lint → pruebas unitarias → pruebas de integración*

---

## 5. Revisión de código (Code Review Policy)
- Toda PR debe ser revisada por al menos 1 revisor.
- CI/CD debe pasar sin errores antes del merge.
- Checklist para revisores:
  - ¿El código es legible y sigue convenciones?
  - ¿Existen pruebas suficientes?
  - ¿No hay secretos ni credenciales en commits?
  - ¿Se actualizó la documentación si era necesario?

---

## 6. Gestión de issues y ramas
- Issues se crean en GitHub con etiquetas: `bug`, `feature`, `docs`, `enhancement`.
- Commits deben referenciar el issue:  
  Ejemplo: `fix(api): corrige validación de uploads (#42)`
- Ramas se eliminan después de hacer merge.

---

## 7. Guía de contribución rápida
1. Clonar repo:  
   ```bash
   git clone git@github.com:PrincipeCoder/UNI-Secure-Droid.git
   cd UNI-Secure-Droid
2. Crear entorno virtual:
    ```bash
    python -m venv venv && source venv/bin/activate
    pip install -r requirements.txt
3. Correr linters y tests:
    ```bash
    make lint
    make test
4. Ejecutar app local:
    ```bash
    uvicorn api.main:app -reload

## 8. Buenas prácticas de seguridad

- Nunca subir llaves ni tokens al repo (usar `.env` o GitHub Secrets).

- Usar `.gitignore` para excluir `__pycache__`, `.env`, `*.log`.

- Escaneo de dependencias: `pip-audit` o `safety`.

- Revisar dependencias críticas cada sprint.

## 9. Integración continua y despliegue

- CI definido en `.github/workflows/ci.yml`

  - Instala dependencias

  - Corre `make lint && make test`

  - Verifica cobertura >= 75%

- CD (opcional): despliegue automático a staging tras merge en `develop`.

- Releases en tags (`v1.0.0`, `v1.1.0`, …).

## 10. Documentación del código

- Docstrings obligatorios en clases y funciones.

- Formato sugerido: Google Style.

- Generación automática con `pdoc` o `sphinx`.

- Mantener `README.md` actualizado con instrucciones básicas.

## 11. OpenAPI v1

El archivo `openapi-v1.yaml` se encuentra en `docs/arquitectura/desarrollo/openapi-v1.yaml`.
Endpoints definidos:

- `POST /api/v1/uploads`

- `GET /api/v1/status/{job_id}`

- `GET /api/v1/report/{job_id}`

## 12. Versionado y releases

Usar semver (`MAJOR.MINOR.PATCH`).

Ejemplo: `v1.0.0`

Mantener `CHANGELOG.md` actualizado siguiendo Keep a Changelog.

## 13. Ejemplo de flujo de desarrollo

1. Crear branch:
`git checkout -b feature/nuevo-modulo`
2. Desarrollar cambios.

3. Correr linters y tests:
`make lint && make test`

4. Commit y push:
    ```bash
    git commit -m "feat(api): agrega endpoint X (#15)"
    git push origin feature/nuevo-modulo

5. Crear Pull Request → Revisión → Merge a `develop`.

## 14. Mapeo RF/RNF
|Requisito	|Módulos|
|-----------|---------|
|RF-01 (subida de archivos)	|api.uploads, analyzers.static
|RF-02 (consulta de estado)	|api.status, jobqueue
|RF-03 (generación de reporte)	|api.report, reporting
|RNF-4.1 (rendimiento)	|jobqueue, workers
|RNF-5.2 (seguridad)	|infra, validación inputs
## 15. Criterios de aceptación

- Repositorio clonado, dependencias instaladas, app corre en local.

- OpenAPI válido y accesible en Swagger UI.

- Cobertura de tests >= 75%.

- CI/CD en verde en cada PR.