-- =====================================================================
-- Pruebas de restricciones · auth_db
-- Se ejecutan dentro de una transacción que se revierte al final:
-- la base queda exactamente como estaba.
--   psql -d auth_db -v ON_ERROR_STOP=1 -f tests/auth_db_tests.sql
-- =====================================================================
SET client_encoding = 'UTF8';
BEGIN;
\pset tuples_only on
\i tests/00_test_helpers.sql

-- Datos base válidos
SELECT pg_temp.expect_ok('Crear rol CLIENTE',
  $$INSERT INTO role (id, name, description) VALUES (901, 'T_CLIENTE', 'Rol de prueba')$$);
SELECT pg_temp.expect_ok('Crear usuario válido',
  $$INSERT INTO app_user (id, username, password_hash, role_id) VALUES (901, 'jgutierrez', '$2a$10$hash', 901)$$);
SELECT pg_temp.expect_ok('Crear cliente válido',
  $$INSERT INTO client (first_name, last_name, cedula, nationality, address, phone, email, user_id)
    VALUES ('Johanna', 'Gutiérrez', '1712345678', 'Ecuatoriana', 'Quito', '0991234567', 'joha@test.com', 901)$$);

-- public_id (id público, no adivinable)
SELECT pg_temp.expect_ok('El cliente recibe public_id automáticamente',
  $q$DO $d$ BEGIN
     IF (SELECT public_id FROM client WHERE user_id = 901) IS NULL THEN RAISE EXCEPTION 'sin public_id'; END IF;
   END $d$$q$);
SELECT pg_temp.expect_violation('public_id repetido',
  $$INSERT INTO app_user (id, username, password_hash, role_id) VALUES (904, 'anaperez3', 'x', 901);
    INSERT INTO client (public_id, first_name, last_name, cedula, nationality, address, phone, email, user_id)
    SELECT public_id, 'Ana', 'Pérez', '1700000001', 'Ecuatoriana', 'Quito', '0990000001', 'ana@test.com', 904
    FROM client WHERE user_id = 901$$, 'uk_client_public');

-- role
SELECT pg_temp.expect_violation('Rol con nombre repetido',
  $$INSERT INTO role (name) VALUES ('T_CLIENTE')$$, 'uk_role_name');
SELECT pg_temp.expect_violation('Rol con estado inválido',
  $$INSERT INTO role (name, status) VALUES ('T_OTRO', 'BORRADO')$$, 'ck_role_status');

-- app_user
SELECT pg_temp.expect_violation('Usuario repetido con distinta mayúscula',
  $$INSERT INTO app_user (username, password_hash, role_id) VALUES ('JGutierrez', 'x', 901)$$, 'uk_app_user_username');
SELECT pg_temp.expect_violation('Usuario con rol inexistente',
  $$INSERT INTO app_user (username, password_hash, role_id) VALUES ('nuevo01', 'x', 999999)$$, 'fk_app_user_role');
SELECT pg_temp.expect_violation('Usuario sin contraseña',
  $$INSERT INTO app_user (username, role_id) VALUES ('nuevo02', 901)$$, 'NOT NULL:password_hash');
SELECT pg_temp.expect_violation('Username con espacios',
  $$INSERT INTO app_user (username, password_hash, role_id) VALUES ('jo ha', 'x', 901)$$, 'ck_app_user_username');
SELECT pg_temp.expect_violation('Borrar un rol que tiene usuarios',
  $$DELETE FROM role WHERE id = 901$$, 'fk_app_user_role');

-- client
SELECT pg_temp.expect_violation('Cliente sin usuario',
  $$INSERT INTO client (first_name, last_name, cedula, nationality, address, phone, email)
    VALUES ('Ana', 'Pérez', '1700000001', 'Ecuatoriana', 'Quito', '0990000001', 'ana@test.com')$$, 'NOT NULL:user_id');
SELECT pg_temp.expect_violation('Dos clientes con el mismo usuario',
  $$INSERT INTO client (first_name, last_name, cedula, nationality, address, phone, email, user_id)
    VALUES ('Ana', 'Pérez', '1700000001', 'Ecuatoriana', 'Quito', '0990000001', 'ana@test.com', 901)$$, 'uk_client_user');
SELECT pg_temp.expect_violation('Cédula repetida',
  $$INSERT INTO client (first_name, last_name, cedula, nationality, address, phone, email, user_id)
    VALUES ('Ana', 'Pérez', '1712345678', 'Ecuatoriana', 'Quito', '0990000001', 'ana@test.com', 901)$$, 'uk_client_cedula');
SELECT pg_temp.expect_violation('Cédula con letras',
  $$INSERT INTO client (first_name, last_name, cedula, nationality, address, phone, email, user_id)
    VALUES ('Ana', 'Pérez', '17ABC45678', 'Ecuatoriana', 'Quito', '0990000001', 'ana@test.com', 901)$$, 'ck_client_cedula');
SELECT pg_temp.expect_violation('Cédula de 9 dígitos',
  $$INSERT INTO client (first_name, last_name, cedula, nationality, address, phone, email, user_id)
    VALUES ('Ana', 'Pérez', '170000000', 'Ecuatoriana', 'Quito', '0990000001', 'ana@test.com', 901)$$, 'ck_client_cedula');
SELECT pg_temp.expect_violation('Email sin @',
  $$INSERT INTO client (first_name, last_name, cedula, nationality, address, phone, email, user_id)
    VALUES ('Ana', 'Pérez', '1700000001', 'Ecuatoriana', 'Quito', '0990000001', 'ana.test.com', 901)$$, 'ck_client_email');
SELECT pg_temp.expect_violation('Email repetido con distinta mayúscula',
  $$INSERT INTO app_user (id, username, password_hash, role_id) VALUES (902, 'anaperez', 'x', 901);
    INSERT INTO client (first_name, last_name, cedula, nationality, address, phone, email, user_id)
    VALUES ('Ana', 'Pérez', '1700000001', 'Ecuatoriana', 'Quito', '0990000001', 'JOHA@test.com', 902)$$, 'uk_client_email');
SELECT pg_temp.expect_violation('Teléfono con letras',
  $$INSERT INTO app_user (id, username, password_hash, role_id) VALUES (903, 'anaperez2', 'x', 901);
    INSERT INTO client (first_name, last_name, cedula, nationality, address, phone, email, user_id)
    VALUES ('Ana', 'Pérez', '1700000001', 'Ecuatoriana', 'Quito', '099ABC', 'ana@test.com', 903)$$, 'ck_client_phone');
SELECT pg_temp.expect_violation('Borrar un usuario que tiene cliente',
  $$DELETE FROM app_user WHERE id = 901$$, 'fk_client_user');

SELECT pg_temp.report();
ROLLBACK;
