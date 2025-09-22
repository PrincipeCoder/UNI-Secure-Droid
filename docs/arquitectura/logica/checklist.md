# Checklist de Calidad – Vista Lógica

> Este checklist asegura que la Vista Lógica cumple con los criterios de aceptación definidos en el plan de arquitectura.

---

### 1 Estructura y componentes
- [ ] No hay acoplamientos circulares entre componentes.
- [ ] Cada componente tiene **una responsabilidad única** (SRP).
- [ ] Todas las fichas están en `components/` con los campos completos.
- [ ] El diagrama de componentes (Mermaid/PNG) está en `diagrams/` y refleja los mismos bloques que las fichas.

### 2 Contratos de datos (DTOs)
- [ ] Existe `contracts/dtos.yaml` con:
  - Formato de respuestas del API.
  - Contenido de las colas (`static_jobs`, `dynamic_jobs`).
  - Esquema del resultado final.
- [ ] Los DTOs definidos coinciden con las entradas/salidas descritas en las fichas.

### 3 Trazabilidad (RF / RNF)
- [ ] El archivo `rf_map.md` lista todos los componentes y los RF/RNF que cubren.
- [ ] Ningún RF quedó sin responsable.

### 5 Criterio de aceptación
- [ ] Un lector externo puede entender **quién hace qué** y **cómo fluye la información** sin ver el código.
- [ ] El documento es claro, coherente y puede congelarse como versión 1.0.

---

✔️ Cuando todas las casillas estén marcadas, la Vista Lógica está lista para revisión cruzada y cierre.
