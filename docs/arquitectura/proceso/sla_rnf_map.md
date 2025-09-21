
## Tabla SLA / Timeouts / Retries

| Operación       | SLA/Timeout | Retry           | Métrica       | RNF              |
|-----------------|-------------|-----------------|---------------|------------------|
| Static analyze  | ≤15 s       | No              | Latencia p95  | 4.1 (Rendimiento)|
| Dynamic run     | ≤90 s       | 1 (backoff exp.)| Latencia p95  | 4.1 (Rendimiento)|
| Report gen      | ≤2 s (p95)  | No              | Latencia      | 4.1 (Rendimiento)|
| Concurrencia    | ≥3 jobs     | Automático      | Throughput    | 4.1 (Rendimiento)|
