# Sprint 2 · Microservicio de Reservas

**Fechas:** 20 al 22 de octubre de 2026 (replanificado: terminaba el 21) · **Estado:** cerrado · **Puntos:** 43 de 43 (10 historias)

**Objetivo del sprint:** tener funcionando `reservation-service`: disponibilidad de asientos, reservas con máximo de 6 entradas por partido, expiración automática, confirmación con tickets QR, cancelación y reportes PDF, integrado con `match-service` y con el JWT de `auth-service`.

## Qué se entregó

| HU | Entregable | Dónde está | PR |
|---|---|---|---|
| HU-22 | `reservation-service` valida el JWT; `match-service` exige rol ADMIN para escribir y expone un endpoint interno protegido con `X-Internal-Key` | `reservation-service`, `match-service` | #14, #15 |
| HU-15 | Disponibilidad de asientos de un partido (solo los ocupados) | `GET /matches/{id}/availability` | #14, #15 |
| HU-16 | Reservar hasta 6 entradas por partido; asiento ocupado → 409 | `POST /reservations` | #14, #15 |
| HU-18 | Confirmar la reserva y emitir un ticket con QR por asiento | `POST /reservations/{id}/confirm`, `GET /tickets/{id}/qr` | #14, #15, #16 |
| HU-17 | Expiración automática de reservas pendientes | Tarea programada | #16 |
| HU-19 | Cancelar reserva (dueño o ADMIN) | `POST /reservations/{id}/cancel` | #17 |
| HU-20 | "Mis reservas": historial del cliente en JSON y PDF | `GET /reservations/me`, `/me/pdf` | #18 |
| HU-37 | Entradas en PDF, una página por asiento con su QR | `GET /reservations/{id}/tickets/pdf` | #19 |
| HU-21 | Reporte consolidado del administrador con filtros y ocupación por sección, en JSON y PDF | `GET /reports/reservations`, `/pdf` | #20 |
| HU-34 | Registro de auditoría `audit_log` en `auth_db` y `reservation_db` (tríada AAA) | `auth-service`, `reservation-service` | Sin PR (directo a `main`) |

| Servicio | Puerto | Base | Swagger |
|---|---|---|---|
| `auth-service` | 8081 | `auth_db` | http://localhost:8081/swagger-ui.html |
| `match-service` | 8082 | `match_db` | http://localhost:8082/swagger-ui.html |
| `reservation-service` | 8083 | `reservation_db` | http://localhost:8083/swagger-ui.html |

Contratos para Apidog: `docs/apis/reservation-service.openapi.yaml` (12 endpoints) y `docs/apis/ms-partidos.openapi.yaml` (endpoints internos).

## Cómo se comunican los servicios

```
Cliente (Apidog / front)
   │  Authorization: Bearer <JWT>
   ▼
reservation-service ──REST + X-Internal-Key──▶ match-service
   │  verifica la firma del JWT                  (partidos, asientos, secciones)
   │  con el mismo secreto (no llama a auth)
   ▼
reservation_db (reservas, bloqueos, tickets, parámetros)
```

- **El JWT se verifica en cada servicio**, sin llamar a `auth-service`.
- **`reservation-service` pregunta a `match-service`** si el partido existe y está `PROGRAMADO`, si los asientos son de su estadio y cuál es la ocupación por sección. Como no hay claves foráneas entre bases, esa consulta reemplaza la integridad referencial.
- **Endpoints internos** (`/match-api/v1/internal/**`): no los usa el front; se protegen con una clave compartida comparada en tiempo constante (`MessageDigest.isEqual`).
- **Timeouts** de 2 s (conexión) y 3 s (lectura). Si `match-service` no responde, el usuario recibe 503 con un mensaje claro, nunca un 500.

## Decisiones de diseño y por qué

1. **La llamada remota va fuera de la transacción.** Primero se consulta a `match-service` y después se abre la transacción en otra clase (`ReservationService` coordina y `ReservationWriter` escribe). Una transacción abierta mientras se espera la red bloquea conexiones de la base.
2. **`@Transactional` solo funciona entre clases.** Spring la activa con un *proxy* que solo intercepta llamadas que vienen de afuera. SonarQube (`java:S6809`) detectó una auto-invocación (`this.expirePending()`) y se corrigió dejándolo como método privado dentro de la transacción de quien lo llama.
3. **Las transacciones devuelven DTOs, no entidades.** Así la respuesta se arma con la sesión de Hibernate abierta y nunca aparece `LazyInitializationException`.
4. **Regla de las 6 entradas leída de la tabla `parameter`** (`MAX_TICKETS_PER_MATCH`). Cuenta los asientos vendidos y los retenidos que aún no vencen; el cupo se libera solo al cancelar o expirar.
5. **Asiento ocupado protegido en dos capas:** una consulta previa da un mensaje claro, y el `UNIQUE(match_id, seat_id)` de la base frena a dos clientes simultáneos (`saveAndFlush` para que el choque ocurra dentro de la transacción y responda 409).
6. **Hibernate inserta antes de borrar.** Al liberar reservas vencidas y crear otra con los mismos asientos, se fuerza `flush()` para que los `DELETE` ocurran antes que los `INSERT`.
7. **Bloqueo pesimista (`SELECT … FOR UPDATE`)** al confirmar, cancelar y expirar. Un doble clic en "Confirmar" no emite tickets duplicados, y la tarea automática nunca choca con el cliente que confirma justo en el último segundo.
8. **El cliente sale del token, nunca del body ni de la URL.** `/reservations/me` ni siquiera recibe un id: es imposible pedir el historial de otro (seguridad por diseño).
9. **Recurso ajeno → 404, no 403** en consultar, confirmar, cancelar, ver el QR y descargar entradas. Un 403 confirmaría que ese id existe (anti-IDOR y anti-enumeración). Se aplica la misma regla en todos los endpoints.
10. **Identificadores no adivinables:** reservas y tickets con UUID; código de reserva `RES-2026-K7P4QX` aleatorio y sin caracteres confusos (`0/O`, `1/I`) para dictarlo en boletería.
11. **El QR es un código aleatorio de 64 caracteres** (`SecureRandom`), sin datos personales. En la puerta, el sistema busca el ticket por ese código; una foto del QR no revela nada.
12. **Expiración automática con intervalo parametrizable:** un `Trigger` propio lee `EXPIRATION_JOB_SECONDS` antes de cada ciclo, así se cambia sin reiniciar. Si el parámetro falta o es inválido, usa 60 s (*fail-safe*): un error de configuración nunca apaga una función crítica.
13. **Modelo de dominio rico:** `reservation.expire()` y `reservation.cancel()` concentran cada regla en un solo lugar.
14. **Cancelar es un borrado lógico** (`POST /cancel`, no `DELETE`): la reserva queda `CANCELADA` para los reportes. Una reserva `PENDIENTE` se puede cancelar aunque `match-service` esté caído; solo una confirmada consulta si el partido ya empezó.
15. **Degradación elegante:** el historial, las entradas en PDF y el reporte se generan aunque `match-service` no responda (sin los datos del partido). En cambio, reservar sí falla con 503, porque sin `match-service` no se puede validar nada.
16. **La lógica va donde están los datos:** para la ocupación por sección, `reservation-service` envía los asientos vendidos y `match-service` los cuenta con un `GROUP BY`. No viajan los miles de asientos del estadio.
17. **Capacidad = suma de las secciones creadas**, no la capacidad declarada del estadio: refleja los asientos que de verdad se pueden vender.
18. **JSON y PDF salen de la misma función.** El PDF solo dibuja el mismo objeto, así que nunca muestran números distintos.
19. **N+1 resuelto con `default_batch_fetch_size`:** las listas de varias reservas se cargan con una sola consulta `IN (...)`.
20. **CSRF desactivado y documentado** (`@SuppressWarnings("java:S4502")` con su justificación): el JWT viaja en un encabezado que el navegador no envía solo. Se debe reactivar si algún día el token viaja en una cookie.
21. **Auditoría en cada servicio, en su propia transacción.**
    - Cada microservicio audita en su base (`audit_log`); un servicio central de auditoría queda como mejora futura.
    - `AuditWriter` guarda con `REQUIRES_NEW`: el registro sobrevive aunque la operación principal haga rollback (por ejemplo, el intento de login con la cuenta bloqueada, que responde 423).
    - `AuditService`, sin transacción, atrapa cualquier error: la auditoría nunca tumba el negocio.
    - El usuario se toma del `SecurityContext` (el `sub` del token), y `SISTEMA` cuando no hay usuario; la IP, de la petición.
    - Registro de solo inserción (`updatable = false`). Nunca guarda contraseñas ni tokens.
    - Se audita por riesgo: cambios de estado, login (éxito y fallo con su motivo), bloqueos, reportes generados (con sus filtros) y descargas de entradas. No se audita la disponibilidad (consulta cada 5 s), el catálogo ni las consultas de datos propios.

## Cómo se verificó

- Pruebas manuales en Apidog por cada historia: caminos felices, errores, valores límite y seguridad. Cada historia se probó con al menos 3 casos al compilar y con su tabla completa antes del PR.
- Técnicas aplicadas:
  - **Valores límite:** exactamente 6 entradas, fecha "hasta" incluida completa, ocupación al 0 % y al 100 %.
  - **Transición de estados:** confirmar o cancelar reservas pendientes, confirmadas, canceladas y expiradas.
  - **Partición de equivalencia:** secciones con ocupación baja, media y alta.
  - **Seguridad:** recursos ajenos (IDOR), roles 401/403, header interno ausente, token manipulado.
  - **Resiliencia:** `match-service` apagado en cada historia y recuperación al volver a encenderlo.
- **Procesos en segundo plano:** preparar → esperar → verificar, acortando los tiempos con los parámetros (`HOLD_MINUTES = 1`).
- **Oráculos en DBeaver:** conteos de `seat_lock` contra lo que responde el reporte; estados de reservas y tickets.
- **Consistencia:** el mismo reporte en JSON y en PDF muestra los mismos números; el QR escaneado con el celular coincide con el `qrCode` del ticket.
- **Análisis estático con SonarQube for IDE.**

**Bugs y hallazgos del sprint:**

| Bug o hallazgo | Cómo se encontró | Corrección |
|---|---|---|
| `X-Internal-Key` no llegaba como header desde Apidog | Prueba del endpoint interno | El contrato lo declaraba como esquema de seguridad; se cambió a parámetro de header |
| El código en GitHub no compilaba (`qrCodeGenerator` sin inyectar) | Revisión del repositorio | Campo y constructor agregados; lección: "en mi máquina funciona" no es evidencia |
| Confirmar respondía 500 aunque la reserva quedaba `CONFIRMADA` | Prueba de regresión tras recompilar | `LazyInitializationException`: la respuesta se arma dentro de la transacción |
| Auto-invocación de un método `@Transactional` | SonarQube (`java:S6809`) | Método privado dentro de la transacción de quien lo llama |
| `throws Exception` sobrante, CSRF y literales de rol repetidos | SonarQube (`S112`, `S4502`, `S1192`) | Firma limpia, decisión documentada y constantes de rol |
| `default_batch_fetch_size` se ignoraba sin error | Aviso del IDE y revisión del yml | Movido a `spring.jpa.properties.hibernate` |
| `Predicate` equivocado en la consulta del reporte | Error de compilación | Import de `jakarta.persistence.criteria.Predicate` |
| Endpoints que daban 401/403 en Apidog | Pruebas | La carpeta apuntaba al puerto de otro servicio; se revisa el puerto ante cualquier 401/403 extraño |
| La variable `public_id` mezclaba ids de reserva y de ticket | Prueba del QR (404) | Variables `reservationId` y `ticketId` |
| Hueco funcional: no se puede cancelar solo una entrada | Pregunta durante las pruebas de HU-19 | Decisión: se cancela la reserva completa; cancelación parcial anotada como HU-36 (Could) |
| Hueco del modelo: una reserva cancelada sin confirmar no conserva sus asientos en el historial | Prueba de HU-20 | Documentado; `seat_lock` es el candado vigente, no un historial |
| El script de la base fallaba la segunda vez (`audit_log` ya existe) | Ejecutar `setup-db` dos veces | `DROP TABLE IF EXISTS audit_log` al inicio: los scripts deben ser idempotentes |
| Las consultas y los reportes no se auditaban | Revisión del alcance durante HU-34 | Se auditan por riesgo: reportes (con sus filtros) y descargas de entradas |
| HU-34 se trabajó y subió directo a `main`, sin rama ni PR | Revisión del repositorio | No se reescribe la historia publicada (sin *force push*); los 4 commits quedaron separados y se documenta la desviación. Prevención: regla de protección de `main` en GitHub que exige PR |

## Conceptos clave

| Concepto | En una frase |
|---|---|
| Proxy de Spring | Intermediario que activa `@Transactional`; solo actúa en llamadas que vienen de otra clase |
| `LazyInitializationException` | Se intenta leer una lista perezosa cuando la sesión de Hibernate ya se cerró |
| Bloqueo pesimista | `SELECT … FOR UPDATE`: la segunda operación espera a que termine la primera |
| Tarea programada (`Trigger`) | Código que Spring ejecuta solo, cada cierto tiempo |
| *Fail-safe* | Ante un error de configuración, usar un valor seguro en vez de apagar la función |
| Degradación elegante | Seguir funcionando con menos datos cuando otro servicio falla |
| Lector tolerante | Recibir enums de otro servicio como texto para no romperse si agregan valores |
| Borrado lógico | Cambiar el estado en vez de borrar, para conservar el historial |
| Modelo de dominio rico | La entidad sabe aplicar sus propias reglas (`expire()`, `cancel()`) |
| Composición | Un DTO reutiliza otro en vez de copiar sus campos |
| Oráculo de prueba | Cálculo independiente contra el que se compara lo que responde el sistema |
| `REQUIRES_NEW` | Transacción aparte que se guarda aunque la principal haga rollback |
| Idempotencia | Ejecutar algo una o diez veces deja el mismo resultado |
| Auditoría por riesgo | Registrar lo que expone datos de otros o entrega credenciales, no cada consulta |

## Preguntas probables del tribunal

**¿Cómo evitan que dos personas compren el mismo asiento?**
En dos capas. Una consulta previa da un mensaje claro, y la restricción `UNIQUE(match_id, seat_id)` de la base de datos garantiza que, si llegan al mismo milisegundo, solo una gana; la otra recibe 409.

**¿Cómo se liberan los asientos de reservas no pagadas?**
Una tarea programada revisa cada `EXPIRATION_JOB_SECONDS` las reservas pendientes vencidas, las pasa a `EXPIRADA` y borra sus bloqueos. Además, al reservar se liberan en el momento las vencidas de ese partido, por si la tarea aún no pasó.

**¿Qué pasa si el cliente confirma justo cuando la tarea está expirando su reserva?**
Ambas operaciones toman el mismo candado (`FOR UPDATE`). La segunda espera, vuelve a leer el estado y actúa en consecuencia: nunca queda una reserva confirmada sin asientos.

**¿Qué pasa si se cae `match-service`?**
Reservar y confirmar responden 503 con un mensaje claro y sin guardar nada. El historial, las entradas y el reporte siguen funcionando, sin los datos del partido.

**¿Por qué un cliente recibe 404 y no 403 al pedir la reserva de otro?**
Un 403 confirmaría que esa reserva existe. Con 404 no se revela nada.

**¿Qué contiene el QR?**
Solo un código aleatorio de 64 caracteres. No tiene datos personales; en la puerta, el sistema busca el ticket por ese código.

**¿Por qué desactivaron CSRF?**
Porque la API no usa cookies: el JWT viaja en el header `Authorization`, que el navegador no envía por su cuenta. CSRF ataca credenciales automáticas. SonarQube lo marcó, se revisó y se documentó la condición que mantiene válida la decisión.

**¿Cumplen la tríada AAA?**
Autenticación: login con BCrypt, bloqueo e inactividad. Autorización: roles en el JWT (401/403) y recursos propios (404). Auditoría: `audit_log` registra quién hizo qué, cuándo, desde qué IP y con qué resultado, incluidos los intentos fallidos y los reportes generados.

**¿Por qué la auditoría usa otra transacción?**
Porque si el login falla con la cuenta bloqueada, la transacción se deshace. Con `REQUIRES_NEW`, el registro del intento se guarda igual: justo el evento sospechoso que más importa.

**¿Cómo calculan la ocupación por sección si los datos están en dos bases?**
`reservation-service` envía los asientos vendidos y `match-service`, que conoce las secciones, los cuenta con un `GROUP BY`. La lógica va donde están los datos.

## Retrospectiva

- **Salió bien:** se terminaron todas las historias comprometidas. El trabajo resultó interesante, en especial entender cómo se generan los PDF.
- **Costó:** el sprint llevó más tiempo del planificado (se extendió un día y el backlog creció con nuevas historias descubiertas al probar).
- **Acción de mejora (propuesta):** reducir el retrabajo que alargó el sprint:
  - Probar apenas compila, y volver a probar después de cada arreglo (dos bugs aparecieron por probar una compilación anterior).
  - Un commit por cada parte terminada, revisando la lista de archivos y el mensaje antes de confirmar.
  - Al importar un contrato en Apidog, revisar que cada carpeta apunte a su servicio.
  - Antes de escribir código, confirmar en qué rama se está (la HU-34 se hizo en `main`).
- **Ánimo para el Sprint 3:** muchas ganas de empezar el frontend.

## Cambios de alcance

- **Agregadas al backlog:** HU-36 cancelación parcial (Could), HU-37 entradas en PDF (Should, hecha en este sprint), HU-38 secciones como recuadros sobre el plano del estadio + `/layout` (Must, Sprint 3) y HU-39 editor de arrastrar y soltar (Could).

## Pendiente para el Sprint 3

- **HU-38:** recuadros de las secciones (x, y, ancho y alto en %) y `GET /stadiums/{id}/layout`. Es requisito del mapa de asientos.
- **Frontend:** login, catálogo, mapa de asientos con disponibilidad en vivo, reservar, confirmar, QR, mis reservas, PDF y panel del administrador.
- El frontend no envía token en el login, no envía filtros vacíos y guarda el token sin usar cookies (para que la decisión sobre CSRF siga siendo válida).
- Prueba de concurrencia con k6 (Sprint 4).
- README con `.env` (`JWT_SECRET`, `INTERNAL_API_KEY`), supuestos (cancelación por reserva completa, fechas del reporte = fecha de la reserva) y declaración de uso de IA.
