# Base de datos

Scripts SQL de creación (HU-03) y pruebas de restricciones para las tres bases del sistema, una por microservicio. Probados en PostgreSQL 16.

```
database/
├── 00_create_databases.sql        crea auth_db, match_db y reservation_db
├── auth_db/01_schema.sql          ms-usuarios:  role, app_user, client
├── match_db/01_schema.sql         ms-partidos:  stadium, section, seat_row, seat, sport_match
├── reservation_db/01_schema.sql   ms-reservas:  parameter, reservation, seat_lock, ticket
└── tests/
    ├── 00_test_helpers.sql        funciones expect_ok / expect_violation / report
    ├── auth_db_tests.sql          21 casos
    ├── match_db_tests.sql         20 casos
    └── reservation_db_tests.sql   24 casos
```

El modelo y sus decisiones de diseño están en [`docs/modelo-er`](../docs/modelo-er/README.md).

## Cómo ejecutarlos

Desde la carpeta `database/`, con `psql` (en Windows: `C:\Program Files\PostgreSQL\16\bin\psql.exe`):

```powershell
# 1. Crear las tres bases (una sola vez)
psql -U postgres -d postgres -f 00_create_databases.sql

# 2. Crear las tablas de cada base
psql -U postgres -d auth_db        -v ON_ERROR_STOP=1 -f auth_db/01_schema.sql
psql -U postgres -d match_db       -v ON_ERROR_STOP=1 -f match_db/01_schema.sql
psql -U postgres -d reservation_db -v ON_ERROR_STOP=1 -f reservation_db/01_schema.sql

# 3. Correr las pruebas de restricciones
psql -U postgres -d auth_db        -v ON_ERROR_STOP=1 -f tests/auth_db_tests.sql
psql -U postgres -d match_db       -v ON_ERROR_STOP=1 -f tests/match_db_tests.sql
psql -U postgres -d reservation_db -v ON_ERROR_STOP=1 -f tests/reservation_db_tests.sql
```

Los scripts de esquema **borran y recrean las tablas**: se pueden ejecutar varias veces, pero eliminan los datos. Úsalos solo en desarrollo.

Si en la consola de Windows las tildes se ven mal, ejecuta `chcp 65001` antes de `psql`. Los datos se guardan bien de todas formas: los scripts fijan `client_encoding = 'UTF8'`.

Las pruebas usan comandos propios de `psql` (`\i`, `\pset`), así que no funcionan en DBeaver. Los scripts de esquema sí.

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

## Cómo funcionan las pruebas

Cada archivo abre una transacción, inserta datos válidos, intenta romper cada regla y al final hace `ROLLBACK`: la base queda como estaba. `expect_violation` no solo verifica que haya un error, sino que lo cause **la restricción esperada**. Si el SQL se acepta, o falla por otro motivo, el caso queda en `FAIL` y `psql` termina con error.

```
PASS | DOBLE VENTA: el asiento 5 del partido 10 otra vez | Rechazado por uk_seat_lock_match_seat
---- 24 casos · 24 PASS · 0 FAIL ----
```

Para comprobar que las pruebas detectan errores reales, se eliminó a propósito `uk_seat_lock_match_seat` y la suite falló en el caso de doble venta, como se esperaba.
