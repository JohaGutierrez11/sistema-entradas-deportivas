-- =====================================================================
-- auth_db · ms-usuarios · DATOS SEMILLA
-- HU-04 · Al menos 5 registros por tabla
--
-- Requiere haber ejecutado 01_schema.sql.
-- Se puede ejecutar varias veces: vacía las tablas y reinicia los ids,
-- así los ids son siempre los mismos (otros servicios los referencian).
--
-- Credenciales de DESARROLLO (nunca usar en producción):
--   admin                     → Admin2026*
--   todos los clientes        → Cliente2026*
-- Las contraseñas se guardan como hash BCrypt ($2a$, costo 10).
-- =====================================================================

SET client_encoding = 'UTF8';

TRUNCATE TABLE client, app_user, role RESTART IDENTITY CASCADE;

-- ---------------------------------------------------------------------
-- role
-- El sistema usa ADMIN y CLIENTE. Los otros tres quedan INACTIVOS:
-- muestran el uso del estado del rol y están reservados para fases futuras.
-- ---------------------------------------------------------------------
INSERT INTO role (id, name, description, status) VALUES
  (1, 'ADMIN',    'Acceso completo: usuarios, catálogo, reservas y reportes', 'ACTIVO'),
  (2, 'CLIENTE',  'Consulta partidos, reserva entradas y ve sus boletos',     'ACTIVO'),
  (3, 'SOPORTE',  'Atención al cliente (fase futura)',                        'INACTIVO'),
  (4, 'TAQUILLA', 'Venta presencial en boletería (fase futura)',              'INACTIVO'),
  (5, 'AUDITOR',  'Solo lectura de reportes y registros (fase futura)',       'INACTIVO');

-- ---------------------------------------------------------------------
-- app_user
-- id 1 = administrador (sin fila en client).
-- id 2..7 = clientes. Casos preparados para pruebas:
--   id 6 (rcastillo)  → cuenta INACTIVA: el login debe responder 403.
--   id 7 (sjaramillo) → recién registrada: must_change_password = TRUE.
--   id 8 (pbravo)     → BLOQUEADO por 5 intentos fallidos; se libera solo
--                       15 minutos después de ejecutar este script.
-- ---------------------------------------------------------------------
INSERT INTO app_user (id, username, password_hash, role_id, status, must_change_password, created_at, last_login_at) VALUES
  (1, 'admin',      '$2a$10$8BovIz1uc8OccW2X2xifkOpbJapt6ZvQHNftl4gQZ8th5EqWfi/2W', 1, 'ACTIVO',   FALSE, '2026-10-01 08:00', '2026-10-17 09:15'),
  (2, 'mlopez',     '$2a$10$MIo0G.WHio6QgnpiIMUxNuogx2EBncKIw2n6EDtrP0F7G6f0PC0fS', 2, 'ACTIVO',   FALSE, '2026-10-02 10:20', '2026-10-15 19:40'),
  (3, 'cvera',      '$2a$10$MIo0G.WHio6QgnpiIMUxNuogx2EBncKIw2n6EDtrP0F7G6f0PC0fS', 2, 'ACTIVO',   FALSE, '2026-10-03 14:05', '2026-10-15 20:02'),
  (4, 'aandrade',   '$2a$10$MIo0G.WHio6QgnpiIMUxNuogx2EBncKIw2n6EDtrP0F7G6f0PC0fS', 2, 'ACTIVO',   FALSE, '2026-10-05 09:30', '2026-10-17 08:55'),
  (5, 'dmorales',   '$2a$10$MIo0G.WHio6QgnpiIMUxNuogx2EBncKIw2n6EDtrP0F7G6f0PC0fS', 2, 'ACTIVO',   FALSE, '2026-10-06 16:45', '2026-10-16 11:10'),
  (6, 'rcastillo',  '$2a$10$MIo0G.WHio6QgnpiIMUxNuogx2EBncKIw2n6EDtrP0F7G6f0PC0fS', 2, 'INACTIVO', FALSE, '2026-10-07 11:00', '2026-10-08 12:30'),
  (7, 'sjaramillo', '$2a$10$MIo0G.WHio6QgnpiIMUxNuogx2EBncKIw2n6EDtrP0F7G6f0PC0fS', 2, 'ACTIVO',   TRUE,  '2026-10-17 07:50', NULL);

INSERT INTO app_user (id, username, password_hash, role_id, status, must_change_password, failed_login_attempts, locked_until, created_at, last_login_at) VALUES
  (8, 'pbravo',     '$2a$10$MIo0G.WHio6QgnpiIMUxNuogx2EBncKIw2n6EDtrP0F7G6f0PC0fS', 2, 'BLOQUEADO', FALSE, 5, now() + interval '15 minutes', '2026-10-09 15:00', '2026-10-14 21:05');

-- ---------------------------------------------------------------------
-- client  (personas ficticias; cédulas con dígito verificador válido)
-- ---------------------------------------------------------------------
INSERT INTO client (id, first_name, middle_name, last_name, cedula, nationality, address, phone, email, user_id) VALUES
  (1, 'María',   'Fernanda', 'López',     '1712345675', 'Ecuatoriana', 'Av. 6 de Diciembre N34-120, Quito',        '0991234567', 'maria.lopez@example.com',     2),
  (2, 'Carlos',  'Andrés',   'Vera',      '1704567898', 'Ecuatoriana', 'Av. Amazonas y Naciones Unidas, Quito',    '0987654321', 'carlos.vera@example.com',     3),
  (3, 'Andrea',  NULL,       'Andrade',   '0913579249', 'Ecuatoriana', 'Cdla. Kennedy Norte Mz. 5, Guayaquil',     '0998765432', 'andrea.andrade@example.com',  4),
  (4, 'Diego',   'Sebastián','Morales',   '1724680135', 'Ecuatoriana', 'Calle Rumipamba E2-45, Quito',             '0976543210', 'diego.morales@example.com',   5),
  (5, 'Rosa',    'Elena',    'Castillo',  '0908642135', 'Ecuatoriana', 'Av. 9 de Octubre 1520, Guayaquil',         '0965432109', 'rosa.castillo@example.com',   6),
  (6, 'Sofía',   NULL,       'Jaramillo', '1743210989', 'Colombiana',  'Av. República de El Salvador N35-20, Quito','+573001234567','sofia.jaramillo@example.com', 7),
  (7, 'Pablo',   'Javier',   'Bravo',     '0135791358', 'Ecuatoriana', 'Calle Larga 7-45, Cuenca',                 '0954321098', 'pablo.bravo@example.com',     8);

-- ---------------------------------------------------------------------
-- Reiniciar las secuencias.
-- Insertamos ids explícitos, así que la secuencia sigue en 1. Sin esto,
-- el primer INSERT de la aplicación intentaría usar id = 1 y fallaría
-- con "duplicate key". Bug clásico de datos semilla.
-- ---------------------------------------------------------------------
DO $$
BEGIN
    PERFORM setval(pg_get_serial_sequence('role',     'id'), (SELECT MAX(id) FROM role));
    PERFORM setval(pg_get_serial_sequence('app_user', 'id'), (SELECT MAX(id) FROM app_user));
    PERFORM setval(pg_get_serial_sequence('client',   'id'), (SELECT MAX(id) FROM client));
END $$;
