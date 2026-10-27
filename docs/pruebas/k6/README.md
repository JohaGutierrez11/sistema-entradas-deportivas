# Prueba de concurrencia y carga (k6)

**Qué demuestra:** 50 usuarios virtuales intentan reservar el mismo asiento al mismo tiempo, y el sistema crea **una sola** reserva; las otras 49 reciben `409 Conflict`. Después, 20 usuarios consultan la disponibilidad durante 30 segundos para medir los tiempos de respuesta.

## Cómo ejecutarla

1. Instala k6 (una sola vez): `winget install k6 --source winget`. Cierra y vuelve a abrir la terminal.
2. Levanta los tres servicios y la base de datos.
3. Desde la raíz del repositorio: `k6 run docs/pruebas/k6/reserva-concurrente.js`
4. Toma una captura del resumen final: es la evidencia para el informe (Anexo E).

## Cómo leer el resultado

| Métrica | Esperado | Significado |
|---|---|---|
| `reservas_creadas` | 1 | Solo un usuario obtuvo el asiento |
| `reservas_rechazadas_409` | 49 | Los demás recibieron conflicto |
| `respuestas_inesperadas` | 0 | Ningún error 500 ni de otro tipo |
| `http_req_duration{scenario:carga_disponibilidad}` p(95) | < 500 ms | Tiempo de respuesta de la consulta de disponibilidad |

Si todos los umbrales (*thresholds*) aparecen con ✓, la prueba pasó. La reserva creada queda PENDIENTE y vence sola a los 10 minutos.

**Nota:** cada ejecución deja una reserva pendiente para uno de los clientes. Si la ejecutas muchas veces seguidas, ese cliente puede llegar al límite de 6 entradas; espera 10 minutos o recarga las semillas.
