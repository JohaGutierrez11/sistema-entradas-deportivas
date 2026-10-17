-- =====================================================================
-- auth_db · ms-usuarios
-- Esquema: role, app_user, client
-- HU-03 · Script SQL de creación
--
-- El script se puede ejecutar varias veces: borra las tablas antes de
-- crearlas. Úsalo solo en desarrollo; borra todos los datos.
-- =====================================================================

-- Los archivos están en UTF-8 (tildes y ñ en comentarios y datos).
SET client_encoding = 'UTF8';

DROP TABLE IF EXISTS client   CASCADE;
DROP TABLE IF EXISTS app_user CASCADE;
DROP TABLE IF EXISTS role     CASCADE;

-- ---------------------------------------------------------------------
-- role: perfiles de acceso (ADMIN, CLIENTE)
-- ---------------------------------------------------------------------
CREATE TABLE role (
    id          BIGSERIAL     NOT NULL,
    name        VARCHAR(30)   NOT NULL,
    description VARCHAR(150),
    status      VARCHAR(10)   NOT NULL DEFAULT 'ACTIVO',

    CONSTRAINT pk_role        PRIMARY KEY (id),
    CONSTRAINT uk_role_name   UNIQUE (name),
    CONSTRAINT ck_role_status CHECK (status IN ('ACTIVO', 'INACTIVO'))
);

COMMENT ON TABLE role IS 'Perfiles de acceso. Un usuario tiene un solo rol (segregación de funciones).';

-- ---------------------------------------------------------------------
-- app_user: credenciales de acceso
-- Se llama app_user porque "user" es palabra reservada en PostgreSQL.
-- ---------------------------------------------------------------------
CREATE TABLE app_user (
    id                   BIGSERIAL    NOT NULL,
    username             VARCHAR(50)  NOT NULL,
    password_hash        VARCHAR(100) NOT NULL,
    role_id              BIGINT       NOT NULL,
    status               VARCHAR(10)  NOT NULL DEFAULT 'ACTIVO',
    must_change_password BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at        TIMESTAMP,

    CONSTRAINT pk_app_user          PRIMARY KEY (id),
    CONSTRAINT fk_app_user_role     FOREIGN KEY (role_id) REFERENCES role (id) ON DELETE RESTRICT,
    CONSTRAINT ck_app_user_status   CHECK (status IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT ck_app_user_username CHECK (username ~ '^[A-Za-z0-9._-]{4,50}$')
);

-- Usuario único sin distinguir mayúsculas: "JGutierrez" y "jgutierrez" son el mismo.
CREATE UNIQUE INDEX uk_app_user_username ON app_user (LOWER(username));
CREATE INDEX idx_app_user_role ON app_user (role_id);

COMMENT ON TABLE  app_user IS 'Cuenta de acceso. El admin es un app_user con rol ADMIN y sin fila en client.';
COMMENT ON COLUMN app_user.password_hash        IS 'Hash BCrypt. Nunca la contraseña en texto plano.';
COMMENT ON COLUMN app_user.must_change_password IS 'TRUE mientras el usuario no cambie la contraseña de primer acceso enviada por correo.';

-- ---------------------------------------------------------------------
-- client: datos personales del cliente
-- Relación 1 : 0..1 con app_user. La FK vive aquí porque todo cliente
-- tiene usuario (participación total), así puede ser NOT NULL.
-- ---------------------------------------------------------------------
CREATE TABLE client (
    id          BIGSERIAL    NOT NULL,
    public_id   UUID         NOT NULL DEFAULT gen_random_uuid(),
    first_name  VARCHAR(50)  NOT NULL,
    middle_name VARCHAR(50),
    last_name   VARCHAR(50)  NOT NULL,
    cedula      VARCHAR(10)  NOT NULL,
    nationality VARCHAR(50)  NOT NULL,
    address     VARCHAR(200) NOT NULL,
    phone       VARCHAR(15)  NOT NULL,
    email       VARCHAR(100) NOT NULL,
    user_id     BIGINT       NOT NULL,

    CONSTRAINT pk_client         PRIMARY KEY (id),
    CONSTRAINT uk_client_public  UNIQUE (public_id),
    CONSTRAINT uk_client_cedula  UNIQUE (cedula),
    CONSTRAINT uk_client_user    UNIQUE (user_id),
    CONSTRAINT fk_client_user    FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE RESTRICT,
    CONSTRAINT ck_client_cedula  CHECK (cedula ~ '^[0-9]{10}$'),
    CONSTRAINT ck_client_phone   CHECK (phone  ~ '^\+?[0-9]{7,14}$'),
    CONSTRAINT ck_client_email   CHECK (email  ~ '^[^@\s]+@[^@\s]+\.[^@\s]+$')
);

CREATE UNIQUE INDEX uk_client_email ON client (LOWER(email));

COMMENT ON TABLE  client IS 'Datos personales del cliente. Solo los usuarios con rol CLIENTE tienen fila aquí.';
COMMENT ON COLUMN client.public_id IS 'Identificador expuesto en la API. El id interno nunca sale del servicio (evita enumeración / IDOR).';
COMMENT ON COLUMN client.cedula IS '10 dígitos. La validación del dígito verificador se hace en el servicio.';
