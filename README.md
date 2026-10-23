# Sistema de Gestión de Entradas para Eventos Deportivos

Parte práctica del examen complexivo de Ingeniería en Sistemas de Información (UCE). Venta de entradas para partidos de fútbol: el cliente elige sus asientos en el plano del estadio, reserva, confirma y recibe sus entradas con código QR; el administrador gestiona estadios, partidos, usuarios y reportes.

## Arquitectura

```
                    ┌──────────────────────────────┐
  Navegador ──────► │ Frontend · React 19 + Vite   │  :5173
                    │ (proxy de Vite = mini gateway)│
                    └──┬───────────┬────────────┬──┘
           /auth-api   │ /match-api│            │ /reservation-api
                       ▼           ▼            ▼
              ┌──────────────┐ ┌──────────────┐ ┌───────────────────┐
              │ auth-service │ │ match-service│◄┤reservation-service│
              │    :8081     │ │    :8082     │ │      :8083        │
              │ emite el JWT │ │estadios,     │ │ reservas, tickets,│
              │              │ │partidos,plano│ │ QR, PDF, reportes │
              └──────┬───────┘ └──────┬───────┘ └─────────┬─────────┘
                     ▼                ▼                   ▼
                  auth_db          match_db         reservation_db     PostgreSQL 16 :5433
```

- **Una base de datos por microservicio.** Las referencias entre servicios (`client_id`, `match_id`, `seat_id`) son lógicas: se validan por REST, no con llaves foráneas.
- **JWT HS256** emitido por auth-service; los otros dos servicios lo validan con la misma clave.
- **reservation-service → match-service** por REST interno (`/internal/**`) protegido con la cabecera `X-Internal-Key`. Si match-service no responde, las pantallas que solo muestran datos siguen funcionando (degradación elegante).
- **Errores en formato Problem Details** (RFC 9457) en los tres servicios.

| Capa | Tecnología |
|---|---|
| Backend | Java 17, Spring Boot 4.1.1, Spring Security (OAuth2 Resource Server), Spring Data JPA / Hibernate 7 |
| Base de datos | PostgreSQL 16 en Docker |
| Frontend | React 19, Vite, React Router |
| Documentos | OpenPDF (PDF), ZXing (QR) |
| Correo (desarrollo) | Mailpit |
| Pruebas de API | Apidog |

## Cómo levantarlo

**Requisitos:** Docker Desktop, JDK 17, Node 22 LTS.

```powershell
# 1. Base de datos: contenedor pg-entradas, 3 bases, tablas, semillas y 116 pruebas
cd database
.\setup-db.ps1

# 2. Correo de desarrollo (las contraseñas temporales del registro llegan aquí)
docker run -d --name mailpit -p 1025:1025 -p 8025:8025 axllent/mailpit
#    Bandeja: http://localhost:8025

# 3. Microservicios: uno por terminal, en este orden
cd backend\auth-service;        .\mvnw spring-boot:run
cd backend\match-service;       .\mvnw spring-boot:run
cd backend\reservation-service; .\mvnw spring-boot:run

# 4. Frontend
cd frontend
npm install
npm run dev
#    App: http://localhost:5173
```

`setup-db.ps1` recarga los datos desde cero: sirve también para volver al estado inicial después de una demostración.

### Variables de entorno

Los servicios arrancan sin configurar nada: cada variable tiene un valor **solo para desarrollo**. En cualquier otro ambiente hay que definirlas.

| Variable | Servicios | Para qué | Valor de desarrollo |
|---|---|---|---|
| `JWT_SECRET` | los tres | Firma y validación del token (mínimo 32 caracteres) | `dev-only-secret-…` |
| `INTERNAL_API_KEY` | match, reservation | Clave de las llamadas internas entre servicios | `dev-internal-key-…` |
| `MATCH_SERVICE_URL` | reservation | Dónde está match-service | `http://localhost:8082` |

En PowerShell, antes de `mvnw spring-boot:run`: `$env:JWT_SECRET = "una-clave-larga-y-aleatoria"`.

### Usuarios de prueba

| Usuario | Contraseña | Para probar |
|---|---|---|
| `admin` | `Admin2026*` | Panel del administrador |
| `mlopez` | `Cliente2026*` | Ya tiene 6 entradas del partido 1: una más → 422 |
| `cvera` | `Cliente2026*` | Historial con un partido jugado y un ticket utilizado |
| `aandrade` | `Cliente2026*` | Reserva pendiente que vence a los 10 minutos |
| `rcastillo` | `Cliente2026*` | Cuenta inactiva: el login responde 403 |
| `pbravo` | `Cliente2026*` | Cuenta bloqueada: el login responde 423 |

Credenciales **solo de desarrollo**. La lista completa y los escenarios preparados están en [`database/README.md`](database/README.md).

## Documentación

| Qué | Dónde |
|---|---|
| Contratos de las APIs (OpenAPI 3) | [`docs/apis`](docs/apis) · Swagger UI en `http://localhost:808X/swagger-ui.html` |
| Modelo entidad-relación y decisiones | [`docs/modelo-er`](docs/modelo-er/README.md) |
| Base de datos, semillas y pruebas | [`database/README.md`](database/README.md) |
| Resumen y decisiones de cada sprint | [`docs/sprints`](docs/sprints) |
| Product backlog y plan de sprints | [`docs/gestion/Plan_Sprints_Complexivo.xlsx`](docs/gestion/Plan_Sprints_Complexivo.xlsx) |

## Reglas de negocio principales

| Regla | Cómo se garantiza |
|---|---|
| Máximo 6 entradas por cliente y partido | Parámetro `MAX_TICKETS_PER_MATCH` → 422 |
| Un asiento no se vende dos veces | Consulta previa (mensaje claro) + `UNIQUE(match_id, seat_id)` en la base (concurrencia) → 409 |
| La reserva retiene los asientos 10 minutos | Parámetro `HOLD_MINUTES` y tarea programada que la pasa a EXPIRADA |
| La venta online cierra 10 minutos después del inicio | Parámetro `SALES_CLOSE_OFFSET_MINUTES`; se mira la hora, no solo el estado del partido |
| Máquina de estados del partido | `PROGRAMADO → EN_CURSO → FINALIZADO` y `PROGRAMADO → CANCELADO`; los estados finales no cambian |
| Bloqueo por fuerza bruta | 5 intentos fallidos → BLOQUEADO 15 minutos |
| Un cliente solo ve sus reservas | El id del cliente sale del token, nunca del body; una reserva ajena responde 404 (anti-IDOR) |
| Nombres únicos sin importar tildes ni mayúsculas | Índice único sobre `name_key(name)` (`unaccent`) |

Las reglas viven en el backend. Cuando el front las repite (por ejemplo, el plano del estadio) es solo para avisar antes de enviar; el backend vuelve a validar.

## Supuestos

- Toda fecha y hora es de Ecuador (`America/Guayaquil`).
- Un usuario tiene un solo rol, asignado al crear la cuenta: los clientes se registran solos y el administrador viene en las semillas. No hay un endpoint para cambiar roles a propósito: sería una vía de escalamiento de privilegios.
- El pago está fuera del alcance: "confirmar" la reserva representa el pago aprobado.
- Las semillas modelan 7 localidades y 8 460 asientos por estadio; `total_capacity` conserva el aforo real. El modelo admite hasta 26 filas × 100 asientos por sección.
- El ADMIN no compra entradas.

## Limitaciones conocidas

| Limitación | Por qué | Cómo se resolvería |
|---|---|---|
| Desactivar un usuario no lo saca de una sesión abierta | El JWT no tiene estado y dura 60 minutos | Tokens cortos con *refresh token*, o una lista de tokens revocados |
| El admin ve "Cliente #id" en las reservas de un evento | El nombre vive en auth_db y reservation-service no lo conoce | Composición en el front o un BFF que junte ambas respuestas |
| Las secciones no se editan ni se eliminan | Sus asientos están referenciados desde otra base, sin llave foránea | Permitirlo solo si el estadio no tiene partidos |
| Swagger UI está abierto | Facilita la revisión del examen | Desactivarlo en producción (`springdoc.swagger-ui.enabled=false`) |
| Sin despliegue con un solo comando | Pendiente del Sprint 4 | `docker compose` con los cinco contenedores |

## Uso de inteligencia artificial

> ✏️ **Joha: esta sección es un borrador. Revísala y reescríbela con tus palabras antes de entregar; tiene que describir exactamente cómo trabajaste.**

Durante el desarrollo usé **Claude (Anthropic)** como asistente técnico. Lo usé para:

- Explicar conceptos y comparar alternativas de diseño antes de implementar (transacciones y llamadas remotas, bloqueo pesimista, máquina de estados, manejo de errores, seguridad).
- Proponer y generar parte del código y de los scripts SQL, que integré, probé y corregí.
- Revisar los pull requests y redactar borradores de la documentación (contratos OpenAPI, resúmenes de sprint, este README).

Lo que hice yo:

- Definí el alcance, las historias de usuario y las reglas de negocio, y tomé las decisiones finales (por ejemplo, el cierre de la venta 10 minutos después del inicio y que las semillas tengan tamaños reales).
- Probé cada funcionalidad en Apidog, en el navegador y en DBeaver. Varios defectos los encontré probando, entre ellos nombres de estadio duplicados que solo diferían en una tilde y la grilla de asientos inutilizable en las secciones grandes.
- Administré el repositorio: ramas, commits y pull requests.

Puedo explicar y defender cada parte del sistema.
