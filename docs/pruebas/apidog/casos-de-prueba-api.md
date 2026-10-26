# Casos de prueba de API (Apidog)

**56 casos** en 9 escenarios: 22 de tipo positivo, 24 de tipo negativo, 10 de tipo seguridad.

Corresponden a la HU-30. Se ejecutan en orden con el *runner* de Apidog. Cada caso valida el código HTTP y, cuando aplica, el contenido de la respuesta.

## Cómo importarlos en Apidog

1. **Importar la colección:** Apidog → *Importar* → *Postman* → `entradas-e2e.postman_collection.json`.
2. **Importar el entorno:** `local.postman_environment.json` (URLs de los servicios, contraseñas de las semillas de desarrollo y clave interna). Selecciónalo arriba a la derecha.
3. **Ejecutar:** crea un escenario de prueba con todas las peticiones en orden, o usa el *runner* de la colección. Ejecuta todo de una vez: cada carpeta usa variables (tokens, asientos, reservas) que deja la anterior.
4. **Repetir:** los asientos se eligen al azar entre los libres del partido 4, así que se puede ejecutar varias veces. Cada ejecución deja una entrada usada para María en ese partido; después de 5 ejecuciones alcanza el límite de 6 y hay que recargar las semillas.

**Precondiciones:** los tres servicios levantados (8081, 8082, 8083) y las semillas recién cargadas.

## 01 Autenticación

| ID | Caso | Tipo | HU | Petición | Resultado esperado |
|---|---|---|---|---|---|
| AUT-01 | Login del administrador | Positivo | HU-11 | `POST /auth-api/v1/auth/login` | 200 con accessToken, rol ADMIN y expiración de 3600 s |
| AUT-02 | Login de cliente (María) | Positivo | HU-11 | `POST /auth-api/v1/auth/login` | 200; el payload del JWT contiene role=CLIENTE y clientId, nunca la contraseña |
| AUT-03 | Login de otro cliente (Carlos) | Positivo | HU-11 | `POST /auth-api/v1/auth/login` | 200 con token de CLIENTE |
| AUT-04 | Contraseña incorrecta | Negativo | HU-11 | `POST /auth-api/v1/auth/login` | 401 con mensaje genérico (no revela si el usuario existe) |
| AUT-05 | Usuario inexistente | Negativo | HU-11 | `POST /auth-api/v1/auth/login` | 401, mismo mensaje que con contraseña incorrecta |
| AUT-06 | Campos vacíos | Negativo | HU-11, HU-13 | `POST /auth-api/v1/auth/login` | 400 por validación |
| AUT-07 | Cuenta inactiva | Negativo | HU-12 | `POST /auth-api/v1/auth/login` | 403: la cuenta está INACTIVA |
| AUT-08 | Cuenta bloqueada | Negativo | HU-11 | `POST /auth-api/v1/auth/login` | 423: cuenta bloqueada por intentos fallidos |
| AUT-09 | Registro con cédula inválida | Negativo | HU-10, HU-13 | `POST /auth-api/v1/auth/register` | 400: falla el dígito verificador de la cédula |

## 02 Autorización y roles

| ID | Caso | Tipo | HU | Petición | Resultado esperado |
|---|---|---|---|---|---|
| AZ-01 | Sin token | Negativo | HU-11 | `GET /auth-api/v1/users` | 401 |
| AZ-02 | Token alterado | Seguridad | HU-11, HU-22 | `GET /reservation-api/v1/reservations/me` | 401: la firma HS256 no coincide |
| AZ-03 | Cliente consulta usuarios (solo ADMIN) | Seguridad | HU-29 | `GET /auth-api/v1/users` | 403 |
| AZ-04 | Admin consulta usuarios | Positivo | HU-12 | `GET /auth-api/v1/users` | 200 con la lista de usuarios |
| AZ-05 | El administrador no puede reservar (segregación de funciones) | Seguridad | HU-29 | `POST /reservation-api/v1/reservations` | 403: el ADMIN gestiona, no compra |

## 03 Escenarios deportivos

| ID | Caso | Tipo | HU | Petición | Resultado esperado |
|---|---|---|---|---|---|
| ESC-01 | Crear un coliseo | Positivo | HU-06, HU-42 | `POST /match-api/v1/stadiums` | 201 con venueType COLISEO |
| ESC-02 | Nombre duplicado escrito distinto (mayúsculas) | Negativo | HU-06 | `POST /match-api/v1/stadiums` | 409: name_key() compara sin mayúsculas, tildes ni espacios repetidos |
| ESC-03 | Capacidad inválida | Negativo | HU-06, HU-13 | `POST /match-api/v1/stadiums` | 400: la capacidad mínima es 1 |
| ESC-04 | Cliente intenta crear un escenario | Seguridad | HU-29 | `POST /match-api/v1/stadiums` | 403 |
| ESC-05 | Plano del estadio 3 | Positivo | HU-07, HU-38 | `GET /match-api/v1/stadiums/3/layout` | 200 con secciones, filas y asientos |
| ESC-06 | Eliminar el coliseo de prueba (limpieza) | Positivo | HU-06 | `DELETE /match-api/v1/stadiums/{{stadiumId}}` | 204: se elimina porque no tiene partidos |

## 04 Reserva de asientos

| ID | Caso | Tipo | HU | Petición | Resultado esperado |
|---|---|---|---|---|---|
| RES-01 | Disponibilidad del partido 4 | Positivo | HU-15, HU-40 | `GET /reservation-api/v1/matches/4/availability` | 200 con salesOpen=true, asientos ocupados y refreshSeconds |
| RES-02 | Reservar un asiento libre | Positivo | HU-16 | `POST /reservation-api/v1/reservations` | 201 PENDIENTE con expiresAt y código RES- |
| RES-03 | Otro cliente intenta el mismo asiento | Negativo | HU-16 | `POST /reservation-api/v1/reservations` | 409: el asiento está retenido (UNIQUE match_id + seat_id) |
| RES-04 | Otro cliente consulta la reserva ajena (IDOR) | Seguridad | HU-13 | `GET /reservation-api/v1/reservations/{{resA}}` | 404, no 403: no se revela que la reserva existe |
| RES-05 | Confirmar la reserva | Positivo | HU-16, HU-18 | `POST /reservation-api/v1/reservations/{{resA}}/confirm` | 200 CONFIRMADA con un ticket ACTIVO y su qrCode |
| RES-06 | Confirmar dos veces | Negativo | HU-16 | `POST /reservation-api/v1/reservations/{{resA}}/confirm` | 409: ya no está PENDIENTE |
| RES-07 | Descargar las entradas en PDF | Positivo | HU-37 | `GET /reservation-api/v1/reservations/{{resA}}/tickets/pdf` | 200 application/pdf |
| RES-08 | Imagen del QR | Positivo | HU-18, HU-27 | `GET /reservation-api/v1/tickets/{{ticketA}}/qr` | 200 image/png |
| RES-09 | QR de un ticket ajeno | Seguridad | HU-13, HU-18 | `GET /reservation-api/v1/tickets/{{ticketA}}/qr` | 404 (IDOR) |

## 05 Reglas de negocio

| ID | Caso | Tipo | HU | Petición | Resultado esperado |
|---|---|---|---|---|---|
| REG-01 | Más de 6 asientos en una solicitud | Negativo | HU-16 | `POST /reservation-api/v1/reservations` | 400 por validación (máximo 6 por solicitud) |
| REG-02 | Superar el límite de entradas por partido | Negativo | HU-16, HU-43 | `POST /reservation-api/v1/reservations` | 422: ya tiene 1 entrada; 1 + 6 supera MAX_TICKETS_PER_MATCH |
| REG-03 | Reservar en un partido FINALIZADO | Negativo | HU-40, HU-41 | `POST /reservation-api/v1/reservations` | 422: el partido no admite reservas |
| REG-04 | Reservar en un partido CANCELADO | Negativo | HU-46 | `POST /reservation-api/v1/reservations` | 422: el partido no admite reservas |
| REG-05 | Asiento de otro estadio | Negativo | HU-22 | `POST /reservation-api/v1/reservations` | 404: el asiento 1 es del estadio 1, no del estadio del partido 4 |
| REG-06 | Partido inexistente | Negativo | HU-22 | `POST /reservation-api/v1/reservations` | 404 |

## 06 Cancelación

| ID | Caso | Tipo | HU | Petición | Resultado esperado |
|---|---|---|---|---|---|
| CAN-01 | Crear una reserva para cancelar | Positivo | HU-16 | `POST /reservation-api/v1/reservations` | 201 PENDIENTE |
| CAN-02 | Cancelar la reserva | Positivo | HU-19, HU-45 | `POST /reservation-api/v1/reservations/{{resC}}/cancel` | 200 CANCELADA (faltan más de 24 h para el partido) |
| CAN-03 | Cancelar dos veces | Negativo | HU-19 | `POST /reservation-api/v1/reservations/{{resC}}/cancel` | 409: ya está cancelada |
| CAN-04 | El asiento cancelado vuelve a estar libre | Positivo | HU-15, HU-19 | `GET /reservation-api/v1/matches/4/availability` | 200 y el asiento ya no aparece ocupado |

## 07 Control de acceso (QR)

| ID | Caso | Tipo | HU | Petición | Resultado esperado |
|---|---|---|---|---|---|
| ACC-01 | QR presentado en otro partido | Negativo | HU-47 | `POST /reservation-api/v1/tickets/validate` | 200, valid=false, OTRO_PARTIDO; la entrada no se consume |
| ACC-02 | QR válido en su partido | Positivo | HU-47 | `POST /reservation-api/v1/tickets/validate` | 200, valid=true, VALIDA; la entrada pasa a UTILIZADA |
| ACC-03 | El mismo QR por segunda vez | Negativo | HU-47 | `POST /reservation-api/v1/tickets/validate` | 200, valid=false, YA_UTILIZADA |
| ACC-04 | QR inventado | Negativo | HU-47 | `POST /reservation-api/v1/tickets/validate` | 200, valid=false, NO_RECONOCIDA |
| ACC-05 | Un cliente intenta validar entradas | Seguridad | HU-47 | `POST /reservation-api/v1/tickets/validate` | 403 |
| ACC-06 | Cancelar una reserva con la entrada ya usada | Negativo | HU-19, HU-47 | `POST /reservation-api/v1/reservations/{{resA}}/cancel` | 409: alguien ya ingresó con esa entrada |

## 08 Reportes y configuración

| ID | Caso | Tipo | HU | Petición | Resultado esperado |
|---|---|---|---|---|---|
| REP-01 | Mis reservas (cliente) | Positivo | HU-20 | `GET /reservation-api/v1/reservations/me` | 200 con las reservas del cliente del token |
| REP-02 | Reporte general filtrado por partido | Positivo | HU-21 | `GET /reservation-api/v1/reports/reservations?matchId=4` | 200 con reservas e indicadores |
| REP-03 | Reservas del partido con su cliente | Positivo | HU-44 | `GET /reservation-api/v1/reports/matches/4/reservations` | 200; cada elemento incluye clientId |
| REP-04 | Un cliente pide el reporte general | Seguridad | HU-21 | `GET /reservation-api/v1/reports/reservations` | 403 |
| CFG-01 | Listar parámetros | Positivo | HU-43 | `GET /reservation-api/v1/parameters` | 200 con los parámetros |
| CFG-02 | Parámetro fuera de rango | Negativo | HU-43 | `PUT /reservation-api/v1/parameters/MAX_TICKETS_PER_MATCH` | 422: el rango permitido es 1 a 10 |
| CFG-03 | Parámetro de solo lectura | Negativo | HU-43 | `PUT /reservation-api/v1/parameters/RESERVATION_CODE_PREFIX` | 422: no se puede modificar desde la aplicación |
| CFG-04 | Un cliente intenta cambiar un parámetro | Seguridad | HU-43 | `PUT /reservation-api/v1/parameters/MAX_TICKETS_PER_MATCH` | 403 |

## 09 Comunicación entre servicios

| ID | Caso | Tipo | HU | Petición | Resultado esperado |
|---|---|---|---|---|---|
| INT-01 | Endpoint interno sin clave | Seguridad | HU-22 | `POST /match-api/v1/internal/seats/validate` | 401: falta X-Internal-Key |
| INT-02 | Validar asientos con la clave interna | Positivo | HU-22 | `POST /match-api/v1/internal/seats/validate` | 200; 16921 válido y 1 inválido (otro estadio) |
| INT-03 | Nombre legible de asientos | Positivo | HU-37 | `POST /match-api/v1/internal/seats/labels` | 200 con sectionName, rowLetter y seatNumber |

## Resultado de la ejecución

| Fecha | Casos | Aprobados | Fallidos | Observaciones |
|---|---|---|---|---|
| | | | | |
