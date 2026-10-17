-- =====================================================================
-- Verificación de datos semilla · auth_db
-- Ejecutar después de 02_seed.sql. Solo lee: no modifica nada.
--   psql -d auth_db -v ON_ERROR_STOP=1 -f tests/auth_db_seed_checks.sql
-- =====================================================================
SET client_encoding = 'UTF8';
BEGIN;
\pset tuples_only on
\i tests/00_test_helpers.sql

SELECT pg_temp.expect_true('role tiene al menos 5 registros',     $$SELECT COUNT(*) >= 5 FROM role$$);
SELECT pg_temp.expect_true('app_user tiene al menos 5 registros', $$SELECT COUNT(*) >= 5 FROM app_user$$);
SELECT pg_temp.expect_true('client tiene al menos 5 registros',   $$SELECT COUNT(*) >= 5 FROM client$$);

SELECT pg_temp.expect_true('Existe al menos un ADMIN activo',
  $$SELECT EXISTS (SELECT 1 FROM app_user u JOIN role r ON r.id = u.role_id
                   WHERE r.name = 'ADMIN' AND u.status = 'ACTIVO' AND r.status = 'ACTIVO')$$);
SELECT pg_temp.expect_true('Ningún administrador tiene ficha de cliente',
  $$SELECT NOT EXISTS (SELECT 1 FROM client c JOIN app_user u ON u.id = c.user_id
                       JOIN role r ON r.id = u.role_id WHERE r.name = 'ADMIN')$$);
SELECT pg_temp.expect_true('Todo cliente tiene un usuario con rol CLIENTE',
  $$SELECT NOT EXISTS (SELECT 1 FROM client c JOIN app_user u ON u.id = c.user_id
                       JOIN role r ON r.id = u.role_id WHERE r.name <> 'CLIENTE')$$);
SELECT pg_temp.expect_true('Todas las contraseñas son hash BCrypt (60 caracteres, $2a$)',
  $$SELECT bool_and(password_hash LIKE '$2a$%' AND LENGTH(password_hash) = 60) FROM app_user$$);
SELECT pg_temp.expect_true('Ningún usuario activo usa un rol inactivo',
  $$SELECT NOT EXISTS (SELECT 1 FROM app_user u JOIN role r ON r.id = u.role_id
                       WHERE u.status = 'ACTIVO' AND r.status = 'INACTIVO')$$);
SELECT pg_temp.expect_true('Hay usuarios en los 3 estados (ACTIVO, INACTIVO, BLOQUEADO)',
  $$SELECT COUNT(DISTINCT status) = 3 FROM app_user$$);
SELECT pg_temp.expect_true('Todo usuario bloqueado tiene intentos fallidos y fecha de fin',
  $$SELECT NOT EXISTS (SELECT 1 FROM app_user
                       WHERE status = 'BLOQUEADO' AND (failed_login_attempts = 0 OR locked_until IS NULL))$$);
SELECT pg_temp.expect_true('Las secuencias quedaron después del último id',
  $$SELECT (SELECT last_value FROM role_id_seq)     >= (SELECT MAX(id) FROM role)
       AND (SELECT last_value FROM app_user_id_seq) >= (SELECT MAX(id) FROM app_user)
       AND (SELECT last_value FROM client_id_seq)   >= (SELECT MAX(id) FROM client)$$);

SELECT pg_temp.report();
ROLLBACK;
