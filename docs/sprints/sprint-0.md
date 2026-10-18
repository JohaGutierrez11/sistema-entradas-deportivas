# Sprint 0 · Base de datos y entorno

**Fechas:** 17 de octubre de 2026 · **Estado:** cerrado · **Puntos:** 14 de 14 planificados (HU-05 pasa al Sprint 1)

**Objetivo del sprint:** tener el entorno listo y la base de datos diseñada, creada, poblada y probada, antes de escribir código de los microservicios.

## Qué se entregó

| HU | Entregable | Dónde está | PR |
|---|---|---|---|
| HU-01 | Entorno: JDK 17, IntelliJ, Docker Desktop, Node, Git, Apidog | — | — |
| HU-02 | Modelo entidad-relación: 12 tablas en 3 bases, una por microservicio | `docs/modelo-er/` | #1, #2 |
| HU-03 | Scripts SQL de creación con restricciones nombradas e índices | `database/*/01_schema.sql` | #3 |
| HU-04 | Datos semilla (≥5 registros por tabla) y script de instalación | `database/*/02_seed.sql`, `database/setup-db.ps1` | #4 |
| — | 106 pruebas automáticas: 70 de restricciones y 36 de semillas | `database/tests/` | #3, #4 |
| — | Plan de sprints con backlog, burndown y rúbrica | `docs/gestion/` | — |

## Decisiones de diseño y por qué

Estas son las decisiones que el tribunal puede preguntar. Cada una tiene su justificación.

1. **Una base de datos por microservicio** (`auth_db`, `match_db`, `reservation_db`). Lo pide el enunciado y es el principio de microservicios: cada servicio es dueño de sus datos. Consecuencia: entre bases no hay FK; la integridad se valida por REST.
2. **La FK va en el lado N de cada relación 1:N.** Cada hijo conoce a un solo padre. En la relación 1 : 0..1 entre usuario y cliente, va en el lado con participación total (`client.user_id NOT NULL UNIQUE`).
3. **El estado del asiento depende del partido, no del asiento.** El asiento A-5 puede estar vendido el sábado y libre el domingo. Por eso `seat` no tiene `status` y existe `seat_lock`.
4. **`UNIQUE(match_id, seat_id)` impide la doble venta** aunque dos clientes reserven el mismo asiento en el mismo segundo. El código revisa; la base de datos garantiza. El segundo intento se traduce en `409 Conflict`.
5. **`seat_lock` guarda solo los asientos tomados.** Sin fila = libre. Si guardara también los libres, el `UNIQUE` no serviría.
6. **Desnormalización deliberada:** `seat_lock.match_id` repite `reservation.match_id` porque un `UNIQUE` no puede abarcar dos tablas.
7. **Un ticket por entrada** con QR único y aleatorio: cada QR es una persona, un asiento y un estado.
8. **"Administrador" es un rol, no una tabla.** Un rol por cuenta (segregación de funciones): el admin no puede reservar; si quiere ir al partido, usa una cuenta de cliente.
9. **Id interno + id público.** PK `BIGINT` en todas las tablas; `public_id UUID` en `client`, `reservation` y `ticket`, que es lo único que viaja en las URLs. Evita la enumeración de recursos (IDOR). El UUID no reemplaza la autorización.
10. **Tres estados de cuenta** (`ACTIVO`, `INACTIVO`, `BLOQUEADO`), cada uno con un dueño distinto: el admin da de baja; el sistema bloquea tras 5 intentos fallidos y desbloquea solo al pasar `locked_until`.
11. **Contraseñas con hash BCrypt**, no con cifrado reversible (el enunciado cita AES256 solo como ejemplo).
12. **Reglas configurables en `parameter`** (máximo de 6 entradas, minutos de retención): cambiar una regla es actualizar un registro, no el código.
13. **`ON DELETE RESTRICT` en todas las FK.** Un borrado en cascada eliminaría, sin aviso, asientos con tickets vendidos en otra base.

## Cómo se verificó

- **70 pruebas de restricciones:** cada una intenta romper una regla y comprueba que la base la rechace **con la restricción exacta**.
- **Prueba de mutación:** se eliminó a propósito la regla anti doble venta y la suite falló, lo que demuestra que las pruebas detectan errores reales.
- **36 verificaciones de semillas:** capacidades que cuadran con los asientos generados, ningún cliente con más de 6 entradas, estados coherentes.
- **Coherencia entre las 3 bases:** los clientes, partidos y 33 asientos que referencia `reservation_db` existen, y cada asiento es del estadio de su partido.
- **Ejecución en Windows:** `setup-db.ps1` corrió sin errores en el equipo de desarrollo.

## Conceptos clave

| Concepto | En una frase |
|---|---|
| Cardinalidad | Cuántos de A se relacionan con cuántos de B (1:1, 1:N, N:M). Se responde con dos preguntas, una por cada dirección |
| Participación | Si la relación es obligatoria (total) u opcional (parcial) para cada lado |
| Tabla intermedia | La forma de resolver un N:M |
| Normalización (3FN) | Cada dato se guarda en un solo lugar, para que no pueda contradecirse |
| Restricción (`CHECK`, `UNIQUE`, FK) | Una regla que la base de datos hace cumplir aunque el código falle |
| Datos semilla | Datos iniciales pensados para demostrar y probar las reglas, no datos al azar |
| `setval` | Mueve la secuencia de ids después de insertar ids a mano; sin esto, el primer insert de la app falla con *duplicate key* |
| Idempotencia vs. concurrencia | Mismo cliente repitiendo la acción vs. dos clientes a la vez sobre el mismo recurso |
| IDOR | Acceder a recursos ajenos cambiando el id en la URL |
| Imagen vs. contenedor | Imagen = molde; contenedor = instancia en ejecución (como clase y objeto) |
| GitHub Flow | `main` siempre funciona; cada tarea en una rama corta que entra por Pull Request |

## Preguntas probables del tribunal

**¿Por qué no hay FK entre `reservation` y `seat`?**
Porque viven en bases distintas, una por microservicio. Reservas valida por REST que el asiento exista en Partidos antes de bloquearlo.

**¿Cómo evitan que dos personas compren el mismo asiento?**
Con `UNIQUE(match_id, seat_id)` en `seat_lock`. Aunque el código revise la disponibilidad, dos peticiones simultáneas podrían pasar esa revisión; la restricción garantiza que solo una se guarde.

**¿Por qué no usaron UUID como PK en todas las tablas?**
Porque el UUID resuelve la enumeración, y ese riesgo solo existe en recursos privados. En el catálogo costaría rendimiento y legibilidad sin beneficio.

**¿Dónde se valida el máximo de 6 entradas?**
En el servicio de Reservas: es una regla que suma datos de varias reservas y no se puede expresar como restricción. El límite se lee de `parameter`.

**¿Cumplen la tríada AAA?**
La base ya soporta autenticación (hash BCrypt, estados de cuenta, bloqueo por intentos) y autorización (roles con estado). La auditoría queda pendiente para el Sprint 2.

## Retrospectiva

- **Salió bien:** crear las bases de datos y encontrar las relaciones entre las tablas.
- **Costó:** recordar el modelo entidad-relación y saber dónde va la FK.
- **Acción de mejora:** probar en Apidog cada endpoint apenas esté listo, no al final del sprint.

## Pendiente para el Sprint 1

- HU-05: crear las carpetas `backend/` y `frontend/` (se hace con el primer microservicio).
