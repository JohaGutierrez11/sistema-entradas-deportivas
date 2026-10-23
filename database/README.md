# Base de datos

Scripts SQL de creación (HU-03), datos semilla (HU-04) y pruebas automáticas para las tres bases del sistema, una por microservicio. Probados en PostgreSQL 16.

```
database/
├── setup-db.ps1                   levanta todo con un comando (Docker)
├── 00_create_databases.sql        crea auth_db, match_db y reservation_db
├── auth_db/
│   ├── 01_schema.sql              ms-usuarios: role, app_user, client
│   └── 02_seed.sql                5 roles, 8 usuarios, 7 clientes
├── match_db/
│   ├── 01_schema.sql              ms-partidos: stadium, section, seat_row, seat, sport_match
│   └── 02_seed.sql                5 estadios, 20 secciones, 65 filas, 610 asientos, 6 partidos
├── reservation_db/
│   ├── 01_schema.sql              ms-reservas: parameter, reservation, seat_lock, ticket
│   └── 02_seed.sql                5 parámetros, 7 reservas, 17 bloqueos, 16 tickets
└── tests/
    ├── 00_test_helpers.sql        expect_ok / expect_violation / expect_true / report
    ├── *_tests.sql                70 pruebas de restricciones (intentan romper cada regla)
    └── *_seed_checks.sql          36 verificaciones de que las semillas cumplen las reglas de negocio
```

El modelo y sus decisiones de diseño están en [`docs/modelo-er`](../docs/modelo-er/README.md).

## Inicio rápido (Docker)

Requisito: Docker Desktop abierto. No hace falta instalar PostgreSQL.

```powershell
cd database
.\setup-db.ps1
```

El script crea el contenedor `pg-entradas` (PostgreSQL 16 en el puerto **5433**), crea las tres bases, las tablas, carga las semillas y corre las 106 pruebas. Se puede ejecutar todas las veces que haga falta: recarga los datos desde cero.

Al final muestra un total general:

```
================================================
 TOTAL: 106 casos · 106 PASS · 0 FAIL
================================================
```

Con `.\setup-db.ps1 -Detail` muestra además cada caso `PASS`; sin esa opción, solo los resúmenes por archivo y los fallos.

Si Windows bloquea el script: `powershell -ExecutionPolicy Bypass -File .\setup-db.ps1`

### Manejo del contenedor

| Acción | Comando |
|---|---|
| Ver si está corriendo | `docker ps` |
| Encender / apagar (los datos se conservan) | `docker start pg-entradas` / `docker stop pg-entradas` |
| Borrar (**se pierden los datos**) | `docker rm -f pg-entradas` |

### Ver los datos en DBeaver

Nueva conexión PostgreSQL: host `localhost`, puerto `5433`, usuario `postgres`, contraseña `postgres`. En la pestaña *PostgreSQL*, activa *Show all databases*. El contenedor debe estar encendido.

## Paso a paso (sin el script)

### Con Docker

Desde la carpeta `database/`:

```powershell
docker run -d --name pg-entradas -e POSTGRES_PASSWORD=postgres -p 5433:5432 -v "${PWD}:/database" postgres:16
docker exec -w /database pg-entradas psql -U postgres -f 00_create_databases.sql

foreach ($db in "auth_db","match_db","reservation_db") {
  docker exec -w /database pg-entradas psql -U postgres -d $db -q -v ON_ERROR_STOP=1 -f "$db/01_schema.sql"
  docker exec -w /database pg-entradas psql -U postgres -d $db -q -v ON_ERROR_STOP=1 -f "$db/02_seed.sql"
  docker exec -w /database pg-entradas psql -U postgres -d $db -q -v ON_ERROR_STOP=1 -f "tests/${db}_seed_checks.sql"
  docker exec -w /database pg-entradas psql -U postgres -d $db -q -v ON_ERROR_STOP=1 -f "tests/${db}_tests.sql"
}
```

### Con PostgreSQL instalado localmente

Mismo orden, con `psql` (en Windows: `C:\Program Files\PostgreSQL\16\bin\psql.exe`):

```powershell
psql -U postgres -d postgres -f 00_create_databases.sql
psql -U postgres -d auth_db -v ON_ERROR_STOP=1 -f auth_db/01_schema.sql
psql -U postgres -d auth_db -v ON_ERROR_STOP=1 -f auth_db/02_seed.sql
# ...repetir para match_db y reservation_db, y luego los archivos de tests/
```

Si en la consola de Windows las tildes se ven mal, ejecuta `chcp 65001` antes. Los datos se guardan bien de todas formas: los scripts fijan `client_encoding = 'UTF8'`.

Los archivos de `tests/` usan comandos propios de `psql` (`\i`, `\pset`) y no funcionan en DBeaver. Los de esquema y semillas sí.

> ⚠️ `01_schema.sql` y `02_seed.sql` **borran los datos existentes**. Son solo para desarrollo.

## Datos semilla

### Credenciales de desarrollo

| Usuario | Contraseña | Rol | Para probar |
|---|---|---|---|
| `admin` | `Admin2026*` | ADMIN | Funciones de administrador |
| `mlopez` | `Cliente2026*` | CLIENTE | Cliente con el máximo de entradas (6) en el partido 1 |
| `cvera` | `Cliente2026*` | CLIENTE | Historial con un partido ya jugado y un ticket utilizado |
| `aandrade` | `Cliente2026*` | CLIENTE | Reserva pendiente y reserva expirada |
| `dmorales` | `Cliente2026*` | CLIENTE | Reserva cancelada y reserva confirmada |
| `rcastillo` | `Cliente2026*` | CLIENTE | Cuenta **INACTIVA**: el login debe rechazarse |
| `sjaramillo` | `Cliente2026*` | CLIENTE | Recién registrada: debe cambiar la contraseña |
| `pbravo` | `Cliente2026*` | CLIENTE | Cuenta **BLOQUEADA** por 5 intentos fallidos; se libera sola a los 15 minutos |

Las contraseñas se guardan como hash BCrypt, compatible con `BCryptPasswordEncoder` de Spring Security.

### Escenarios preparados

Las semillas no son datos al azar: cada registro existe para demostrar o probar una regla.

| Escenario | Dato | Resultado esperado |
|---|---|---|
| Límite de entradas | `mlopez` ya tiene 6 entradas del partido 1 | Reservar una más → `422` |
| Doble venta | Palco Occidental A1 del partido 1 (seat 1) está vendido | Reservarlo → `409` |
| Partido cancelado | Partido 6 en estado CANCELADO | No admite reservas |
| Expiración | Reserva 3 PENDIENTE con 3 asientos HELD | Pasa a EXPIRADA a los 10 minutos y libera los asientos |
| Cancelación | Reserva 4 CANCELADA | Tickets ANULADOS, asientos libres |
| Ticket usado | Partido 5 ya jugado, un ticket UTILIZADO | No se puede cancelar |
| Cuenta inactiva | `rcastillo` | Login → `403` |
| Cuenta bloqueada | `pbravo` | Login → `423`, aunque la contraseña sea correcta, hasta que pase `locked_until` |
| Bloqueo por fuerza bruta | 5 contraseñas incorrectas seguidas con `cvera` | La cuenta pasa a BLOQUEADO |

### Ids estables entre microservicios

Las referencias lógicas de `reservation_db` (`client_id`, `match_id`, `seat_id`) apuntan a ids de las otras bases. Por eso las semillas reinician los ids (`TRUNCATE ... RESTART IDENTITY`) y siempre producen los mismos valores. El mapa de asientos está en el encabezado de `match_db/02_seed.sql`: cada estadio tiene 7 localidades y 8 460 asientos, y el estadio N ocupa los `seat.id` desde `(N-1)*8460 + 1`.

Después de insertar ids explícitos, cada semilla mueve la secuencia al último id (`setval`). Sin eso, el primer registro creado por la aplicación intentaría usar `id = 1` y fallaría con *duplicate key*.

## Convenciones

| Prefijo | Tipo de restricción | Ejemplo |
|---|---|---|
| `pk_` | Llave primaria | `pk_seat` |
| `fk_` | Llave foránea | `fk_section_stadium` |
| `uk_` | Unicidad | `uk_seat_lock_match_seat` |
| `ck_` | Regla de validación | `ck_section_capacity` |
| `idx_` | Índice de rendimiento | `idx_sport_match_date` |

Nombrar las restricciones permite que el error diga exactamente qué regla se violó. El microservicio usa ese nombre para responder el código HTTP correcto; por ejemplo, `uk_seat_lock_match_seat` → `409 Conflict`.

- **`ON DELETE RESTRICT` en todas las FK.** Un borrado en cascada eliminaría asientos con tickets vendidos en otra base de datos, sin aviso.
- **Índices en todas las FK.** PostgreSQL no los crea automáticamente.
- **Unicidad sin distinguir mayúsculas** en `username` y `email` (índice sobre `LOWER(...)`).
- **Id interno + id público:** toda tabla usa `id BIGINT` como PK interna. `client`, `reservation` y `ticket` agregan `public_id UUID` (generado con `gen_random_uuid()`), que es el único identificador que viaja en las URLs de la API.
- **Estados con `CHECK`** en lugar de tipos `ENUM`: más fáciles de modificar y se mapean directo con `@Enumerated(EnumType.STRING)` en JPA.

## Pruebas

| Tipo | Qué verifica | Casos |
|---|---|---|
| Restricciones (`*_tests.sql`) | Cada regla rechaza lo que debe rechazar, con la restricción exacta | 70 |
| Semillas (`*_seed_checks.sql`) | Mínimo 5 registros por tabla y coherencia con las reglas de negocio (capacidades, máximo de entradas, estados de reservas y de cuentas) | 36 |

Las pruebas corren dentro de una transacción que termina en `ROLLBACK`: no modifican los datos. Si algún caso falla, `psql` termina con error y `setup-db.ps1` se detiene.

```
PASS | DOBLE VENTA: el asiento 5 del partido 10 otra vez | Rechazado por uk_seat_lock_match_seat
---- 24 casos · 24 PASS · 0 FAIL ----
```

Para comprobar que las pruebas detectan errores reales, se eliminó a propósito `uk_seat_lock_match_seat` y la suite falló en el caso de doble venta, como se esperaba. Además se verificó la coherencia **entre** las tres bases: los 7 clientes, partidos y 33 referencias a asientos de `reservation_db` existen y cada asiento pertenece al estadio de su partido.
