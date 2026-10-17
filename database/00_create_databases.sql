-- =====================================================================
-- 00_create_databases.sql
-- Crea las tres bases de datos del sistema, una por microservicio.
--
-- Ejecutar conectado a la base "postgres" con un usuario administrador.
-- No usar con Docker Compose: ahí cada contenedor crea su propia base
-- con la variable POSTGRES_DB.
-- =====================================================================

CREATE DATABASE auth_db;         -- ms-usuarios
CREATE DATABASE match_db;        -- ms-partidos
CREATE DATABASE reservation_db;  -- ms-reservas
