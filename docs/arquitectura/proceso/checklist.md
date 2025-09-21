# Checklist - Vista 2 (Proceso)

Este checklist asegura que la Vista de Procesos cumple con los criterios definidos en el plan de arquitectura.

## Cobertura del flujo
- [ ] Las secuencias representan el MVP completo  
- [ ] Se incluye el flujo híbrido (normal + alternativo/excepciones)  

## Manejo de errores
- [ ] Los timeouts están explícitamente representados  
- [ ] Los posibles errores en los procesos están modelados (ejemplo: fallo de autenticación, caída de red, etc.)  

## Concurrencia y colas
- [ ] Los procesos que requieren colas (ejemplo: solicitudes concurrentes) están documentados  
- [ ] La concurrencia está explícitamente representada (ejemplo: múltiples usuarios en paralelo)  

## Métricas y RNF (Requisitos No Funcionales)
- [ ] Se enlazan métricas de rendimiento a RNF relevantes (ejemplo: tiempo de respuesta < 2s, disponibilidad, escalabilidad)  
- [ ] Los puntos de monitoreo están identificados en los procesos  

---
✔️ Cuando todas las casillas estén marcadas, la Vista Lógica está lista para revisión cruzada y cierre.