# Sprint 1 · Microservicios de Partidos y de Autenticación

**Fechas:** 18 al 20 de octubre de 2026 · **Estado:** cerrado · **Puntos:** 31 de 31 (10 historias)

**Objetivo del sprint:** tener funcionando `match-service` (estadios, secciones, asientos y partidos) y `auth-service` (registro, login con JWT y roles), ambos documentados con Swagger.

## Qué se entregó

| HU | Entregable | Dónde está | PR |
|---|---|---|---|
| HU-05 | Estructura `backend/` y `frontend/` (Spring Boot 4.1.1 + React con Vite) | `backend/`, `frontend/` | #7 |
| HU-06 | CRUD de estadios | `match-service` | #8 |
| HU-07 | Secciones que generan sus filas (A, B, C…) y asientos (1..N) | `match-service` | #9 |
| HU-08 | Partidos con máquina de estados | `match-service` | #10 |
| HU-09 | Catálogo de partidos con filtros combinables | `match-service` | #11 |
| HU-10 | Registro de clientes con credenciales por correo | `auth-service` | Cierre del sprint |
| HU-11 | Login con JWT, bloqueo por intentos y cambio de contraseña | `auth-service` | Cierre del sprint |
| HU-12 | Administración de usuarios y roles (solo ADMIN) | `auth-service` | Cierre del sprint |
| HU-13 | Contraseñas con BCrypt y DTOs validados | `auth-service` | Cierre del sprint |
| HU-14 | Swagger en ambos servicios y contrato de `auth-service` | `/swagger-ui.html`, `docs/apis/` | Cierre del sprint |

| Servicio | Puerto | Base | Swagger |
|---|---|---|---|
| `match-service` | 8082 | `match_db` | http://localhost:8082/swagger-ui.html |
| `auth-service` | 8081 | `auth_db` | http://localhost:8081/swagger-ui.html |

## Arquitectura de cada microservicio

```
Controller  →  Service  →  Repository  →  PostgreSQL
(recibe HTTP)  (reglas)    (guarda/lee)
      ↑                                  
   DTO (lo que entra y sale)   Entity (espejo de la tabla)
      └──────── GlobalExceptionHandler: traduce cada error a su código HTTP ────────┘
```

- **Controller:** recibe la petición y valida el formato (`@Valid`). No tiene lógica.
- **Service:** aplica las reglas del negocio. Lanza excepciones propias cuando algo no se cumple.
- **Repository:** consultas a la base; Spring Data las genera a partir del nombre del método.
- **DTO vs. Entity:** la entidad es la tabla; el DTO es el contrato. El cliente solo puede enviar y recibir lo que el DTO define.

## Decisiones de diseño y por qué

1. **Spring Boot 4.1.1 estable y Java 17.** Se descartó la versión SNAPSHOT porque cambia a diario. Todos los servicios usan la misma versión.
2. **Nombres en inglés y un prefijo por servicio** (`/match-api/v1`, `/auth-api/v1`). Con solo leer la URL se sabe qué servicio responde, y cada uno versiona su API por separado.
3. **Errores en formato Problem Details (RFC 9457)** con un traductor central. Todos los errores tienen la misma forma, y los de validación traen la lista `errors` con el campo y el mensaje.
4. **Cada código HTTP tiene un significado preciso:** 400 datos mal formados, 404 no existe, 409 choca con algo existente, 422 rompe una regla de negocio, 423 cuenta bloqueada, 500 falla del servidor.
5. **Los errores internos no se disfrazan.** Una violación `NOT NULL` o `CHECK` en la base significa un bug propio, así que responde 500 y no 409 (se distingue por el SQLState de PostgreSQL). Al cliente nunca le llega SQL ni detalles internos.
6. **Duplicados protegidos en dos capas:** el servicio verifica antes de guardar (mensaje claro) y la restricción `UNIQUE` de la base frena las solicitudes simultáneas.
7. **Listas blancas en los textos** (`@Pattern`). La inyección SQL ya está neutralizada por las consultas parametrizadas de JPA, pero además se valida la entrada para evitar XSS e inyección de fórmulas en reportes. Regla: validar la entrada y escapar la salida.
8. **`ddl-auto: validate`.** Hibernate no crea ni modifica tablas: solo comprueba al arrancar que las entidades coincidan con los scripts SQL del Sprint 0.
9. **Secciones generadas en una sola transacción.** Un solo `save` con `cascade` inserta la sección, sus filas y sus asientos; si algo falla, no queda nada a medias.
10. **Invariante de capacidad:** la suma de las secciones nunca supera la capacidad del estadio. Se protege en las dos operaciones que pueden romperla: crear una sección y bajar la capacidad del estadio.
11. **Máquina de estados de partidos** dentro del enum `MatchStatus`: PROGRAMADO → EN_CURSO → FINALIZADO y PROGRAMADO → CANCELADO. Los estados finales no cambian. Los partidos no se eliminan (se cancelan) y no cambian de estadio, porque las reservas apuntan a asientos del estadio original.
12. **Hora de Ecuador explícita** (`America/Guayaquil`) al validar fechas, porque los contenedores suelen correr en UTC.
13. **Catálogo con `Specification`:** los cuatro filtros son opcionales y se combinan; el estadio se trae con `fetch` en la misma consulta para evitar el problema N+1. Sin resultados responde `[]`, no 404.
14. **JWT sin estado (HS256).** `auth-service` firma el token y cada servicio verifica la firma por su cuenta, sin llamar a `auth-service`. El token lleva `sub`, `role`, `userId`, `clientId` y vencimiento; nunca datos sensibles, porque se puede leer.
15. **Login seguro:**
    - Mensaje genérico para usuario inexistente y contraseña incorrecta (evita la enumeración de usuarios).
    - La cuenta bloqueada se revisa **antes** de la contraseña (no se puede seguir probando durante el bloqueo).
    - La cuenta inactiva se revisa **después** (solo el dueño se entera de que está inactiva).
    - `noRollbackFor` mantiene el contador de intentos aunque el login falle; sin él, el bloqueo nunca se activaría.
16. **Credenciales de primer acceso:** usuario generado (inicial + apellido, sin tildes, con número si ya existe) y contraseña con `SecureRandom`. La contraseña solo viaja por correo, nunca en la respuesta HTTP, y se guarda únicamente como hash BCrypt.
17. **Cédula ecuatoriana validada con el algoritmo módulo 10** mediante una anotación propia (`@CedulaEcuatoriana`), y teléfonos con el formato ecuatoriano.
18. **La identidad sale del token, nunca del body.** El cambio de contraseña usa el `sub` del JWT, así nadie puede cambiar la contraseña de otro usuario (prevención de IDOR).
19. **CSRF deshabilitado** porque el token viaja en un encabezado y no en cookies; ese ataque no aplica.
20. **Mailpit** como servidor de correo de desarrollo: muestra los correos en http://localhost:8025 sin enviar correos reales.

## Cómo se verificó

- Pruebas manuales en Apidog y Swagger por cada endpoint: caminos felices, errores y valores límite.
- Técnicas aplicadas: valores límite (capacidad 0, 1, 200000, 200001; secciones que suman exactamente la capacidad), transición de estados (todas las transiciones de partidos, válidas e inválidas), clases de equivalencia (cédulas y teléfonos) y pruebas de seguridad (inyección SQL, *mass assignment*, enumeración de usuarios, roles 401/403).
- Verificación en la base de datos con DBeaver: filas y asientos generados, hash BCrypt, contador de intentos y bloqueo.

**Bugs encontrados y corregidos durante el sprint:**

| Bug | Cómo se encontró | Corrección |
|---|---|---|
| Nombre duplicado respondía 500 | Prueba negativa en Apidog | 409 en dos capas |
| Al refactorizar se borró `setName` y crear estadios fallaba | Error en el log | Restaurado; regresión documentada |
| El handler convertía un `NOT NULL` en 409 y escondía el bug | Análisis del log | Clasificación por SQLState: solo `UNIQUE` y FK son 409 |
| Capacidad máxima 2 000 000 en vez de 200 000 | Revisión contra el contrato | `@Max(200000)` y prueba de valor límite |
| `location` sin `trim()` al editar | Revisión de código | Mismo tratamiento en crear y editar |
| Mensaje de error engañoso con JSON mal formado | Prueba con una coma faltante | Mensaje que menciona sintaxis y formatos |
| Mensajes de validación en inglés | Respuesta de Apidog | Idioma fijo en español |
| El endpoint de login no estaba expuesto | Swagger no lo mostraba (*contract drift*) | Endpoint agregado |

## Conceptos clave

| Concepto | En una frase |
|---|---|
| Inyección de dependencias | Cada clase pide lo que necesita en su constructor y Spring se lo entrega |
| DTO | Objeto que define exactamente qué entra y qué sale de la API |
| `@Transactional` | Todo se guarda junto o no se guarda nada |
| *Dirty checking* | Hibernate guarda solo los cambios de una entidad cargada en la transacción, sin llamar a `save()` |
| `LAZY` / `fetch` | Cargar una relación solo cuando se usa / traerla en la misma consulta |
| Problema N+1 | Una consulta para la lista y otra por cada elemento; se evita con `fetch` |
| Problem Details | Formato estándar de errores HTTP en JSON |
| JWT | "Pulsera" firmada que dice quién eres, tu rol y cuándo vence |
| 401 vs. 403 | No sé quién eres / sé quién eres, pero no tienes permiso |
| BCrypt | Hash lento y con sal para contraseñas; no se puede revertir |
| Invariante | Regla que siempre debe cumplirse y se protege en cada operación que pueda romperla |
| *Contract drift* | Diferencia entre lo que el contrato promete y lo que el código hace |

## Preguntas probables del tribunal

**¿Cómo se comunican los microservicios para saber quién es el usuario?**
Por el JWT. `auth-service` lo firma al iniciar sesión, y los demás servicios verifican la firma con el mismo secreto, sin llamar a `auth-service`. Así siguen funcionando aunque `auth-service` esté caído.

**¿Por qué el login no dice si el usuario existe?**
Para evitar la enumeración de usuarios: un atacante podría descubrir cuentas reales probando nombres. Por eso usuario inexistente y contraseña incorrecta responden el mismo 401.

**¿Qué pasa si alguien prueba contraseñas sin parar?**
Al quinto intento fallido la cuenta se bloquea 15 minutos (423). El bloqueo es temporal para no permitir que un atacante bloquee cuentas ajenas de forma permanente: es un equilibrio entre confidencialidad y disponibilidad.

**¿Están protegidos contra inyección SQL?**
Sí, en dos capas. JPA envía el SQL y los datos por separado (consultas parametrizadas), y además los textos se validan con listas blancas.

**¿Por qué 409 y no 422 para un nombre repetido?**
409 indica un conflicto con otro registro existente; 422 indica que los datos violan una regla de negocio aunque no choquen con nada.

**¿Por qué no se puede eliminar un partido?**
Porque tiene reservas y tickets en otro microservicio. Se cancela para conservar el historial.

**¿Cumplen la tríada AAA?**
Autenticación: login con BCrypt, bloqueo e inactividad. Autorización: roles en el JWT (401/403). Auditoría: `last_login_at` y el contador de intentos; el registro completo de auditoría (`audit_log`) queda para el Sprint 2.

## Retrospectiva

- **Salió bien:** se completaron las 10 historias del sprint y los dos microservicios funcionan y están documentados.
- **Costó:** surgieron muchas dudas con conceptos nuevos (sintaxis de Java, relaciones de JPA, seguridad con JWT) y con Git (trabajar en la rama equivocada).
- **Acción de mejora:** hacer las cosas más rápido. En concreto:
  - Revisar la rama actual antes de empezar cada historia.
  - Cerrar cada historia con su PR antes de empezar la siguiente.
  - Probar cada endpoint en Apidog apenas esté listo (acción que se mantiene del Sprint 0).

## Pendiente para el Sprint 2

- `reservation-service`: reservas con máximo de 6 entradas, bloqueo y expiración de asientos, QR y reportes PDF.
- **Riesgo abierto:** `match-service` todavía no valida el JWT; sus endpoints de escritura están expuestos.
- Secciones: editar, eliminar y el endpoint `layout` para el mapa de asientos.
- Endpoint interno `/match-api/v1/internal/seats/validate` para `reservation-service`.
- Bloqueo pesimista para las reglas de capacidad con concurrencia.
- Índice único sobre `LOWER(name)` en `stadium`.
- Respuestas 401/403 en formato Problem Details.
- Registro de auditoría `audit_log` (HU-34).
- README con `.env` (`JWT_SECRET`), Mailpit y desactivar Swagger en producción.
