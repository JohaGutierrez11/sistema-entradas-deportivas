-- =====================================================================
-- reservation_db · ms-reservas
-- Esquema: parameter, reservation, seat_lock, ticket
-- HU-03 · Script SQL de creación
--
-- client_id, match_id y seat_id son REFERENCIAS LÓGICAS a tablas de
-- otros microservicios. No tienen FK porque viven en otra base de datos;
-- el servicio valida que existan llamando por REST.
--
-- Identificadores: id (BIGINT) es interno; public_id (UUID) es el que
-- viaja en las URLs de la API. Ver docs/modelo-er, decisión 12.
--
-- El script se puede ejecutar varias veces: borra las tablas antes de
-- crearlas. Úsalo solo en desarrollo; borra todos los datos.
-- =====================================================================

-- Los archivos están en UTF-8 (tildes y ñ en comentarios y datos).
SET client_encoding = 'UTF8';

DROP TABLE IF EXISTS ticket      CASCADE;
DROP TABLE IF EXISTS seat_lock   CASCADE;
DROP TABLE IF EXISTS reservation CASCADE;
DROP TABLE IF EXISTS parameter   CASCADE;

-- ---------------------------------------------------------------------
-- parameter: reglas de negocio configurables sin cambiar código
-- ---------------------------------------------------------------------
CREATE TABLE parameter (
    id          BIGSERIAL    NOT NULL,
    code        VARCHAR(50)  NOT NULL,
    value       VARCHAR(100) NOT NULL,
    description VARCHAR(250),

    CONSTRAINT pk_parameter      PRIMARY KEY (id),
    CONSTRAINT uk_parameter_code UNIQUE (code)
);

COMMENT ON TABLE parameter IS 'Ej.: MAX_TICKETS_PER_MATCH = 6, HOLD_MINUTES = 10.';

-- ---------------------------------------------------------------------
-- reservation: la compra de un cliente para un partido
-- ---------------------------------------------------------------------
CREATE TABLE reservation (
    id           BIGSERIAL   NOT NULL,
    public_id    UUID        NOT NULL DEFAULT gen_random_uuid(),
    code         VARCHAR(20) NOT NULL,
    client_id    BIGINT      NOT NULL,
    match_id     BIGINT      NOT NULL,
    status       VARCHAR(12) NOT NULL DEFAULT 'PENDIENTE',
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at   TIMESTAMP   NOT NULL,
    confirmed_at TIMESTAMP,
    cancelled_at TIMESTAMP,

    CONSTRAINT pk_reservation           PRIMARY KEY (id),
    CONSTRAINT uk_reservation_public    UNIQUE (public_id),
    CONSTRAINT uk_reservation_code      UNIQUE (code),
    CONSTRAINT ck_reservation_status    CHECK (status IN ('PENDIENTE', 'CONFIRMADA', 'CANCELADA', 'EXPIRADA')),
    CONSTRAINT ck_reservation_expires   CHECK (expires_at > created_at),
    -- Coherencia entre el estado y sus fechas:
    CONSTRAINT ck_reservation_confirmed CHECK (status <> 'CONFIRMADA' OR confirmed_at IS NOT NULL),
    CONSTRAINT ck_reservation_cancelled CHECK (status <> 'CANCELADA'  OR cancelled_at IS NOT NULL)
);

-- "Mis reservas" y la regla de máximo de entradas por cliente y partido.
CREATE INDEX idx_reservation_client_match ON reservation (client_id, match_id);
-- Reporte del administrador por partido y estado.
CREATE INDEX idx_reservation_match_status ON reservation (match_id, status);
-- Tarea programada que expira las reservas pendientes vencidas.
CREATE INDEX idx_reservation_pending_exp  ON reservation (expires_at) WHERE status = 'PENDIENTE';

COMMENT ON COLUMN reservation.public_id IS 'Identificador expuesto en la API (/api/reservations/{public_id}). No adivinable.';
COMMENT ON COLUMN reservation.code      IS 'Código legible para personas (soporte, boleto impreso). No se usa en URLs porque es adivinable.';
COMMENT ON COLUMN reservation.client_id IS 'Referencia lógica a auth_db.client.id (validada por REST).';
COMMENT ON COLUMN reservation.match_id  IS 'Referencia lógica a match_db.sport_match.id (validada por REST).';

-- ---------------------------------------------------------------------
-- seat_lock: asientos tomados por partido.
--   Sin fila = libre · HELD = retenido · RESERVED = vendido
-- La fila se borra cuando la retención expira o la reserva se cancela.
-- ---------------------------------------------------------------------
CREATE TABLE seat_lock (
    id             BIGSERIAL   NOT NULL,
    reservation_id BIGINT      NOT NULL,
    match_id       BIGINT      NOT NULL,
    seat_id        BIGINT      NOT NULL,
    status         VARCHAR(10) NOT NULL DEFAULT 'HELD',

    CONSTRAINT pk_seat_lock             PRIMARY KEY (id),
    CONSTRAINT fk_seat_lock_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (id) ON DELETE RESTRICT,
    -- La regla más importante del sistema: un asiento, un partido, un dueño.
    CONSTRAINT uk_seat_lock_match_seat  UNIQUE (match_id, seat_id),
    CONSTRAINT ck_seat_lock_status      CHECK (status IN ('HELD', 'RESERVED'))
);

CREATE INDEX idx_seat_lock_reservation ON seat_lock (reservation_id);

COMMENT ON COLUMN seat_lock.match_id IS 'Copia de reservation.match_id. Desnormalización deliberada: un UNIQUE no puede abarcar dos tablas.';

-- ---------------------------------------------------------------------
-- ticket: una entrada por asiento, con QR único
-- ---------------------------------------------------------------------
CREATE TABLE ticket (
    id             BIGSERIAL   NOT NULL,
    public_id      UUID        NOT NULL DEFAULT gen_random_uuid(),
    reservation_id BIGINT      NOT NULL,
    seat_id        BIGINT      NOT NULL,
    qr_code        VARCHAR(64) NOT NULL,
    status         VARCHAR(10) NOT NULL DEFAULT 'ACTIVO',
    issued_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    used_at        TIMESTAMP,

    CONSTRAINT pk_ticket                  PRIMARY KEY (id),
    CONSTRAINT uk_ticket_public           UNIQUE (public_id),
    CONSTRAINT fk_ticket_reservation      FOREIGN KEY (reservation_id) REFERENCES reservation (id) ON DELETE RESTRICT,
    CONSTRAINT uk_ticket_qr_code          UNIQUE (qr_code),
    CONSTRAINT uk_ticket_reservation_seat UNIQUE (reservation_id, seat_id),
    CONSTRAINT ck_ticket_status           CHECK (status IN ('ACTIVO', 'UTILIZADO', 'ANULADO')),
    CONSTRAINT ck_ticket_used             CHECK (status <> 'UTILIZADO' OR used_at IS NOT NULL),
    CONSTRAINT ck_ticket_qr_length        CHECK (LENGTH(qr_code) >= 32)
);

CREATE INDEX idx_ticket_reservation ON ticket (reservation_id);

COMMENT ON COLUMN ticket.public_id IS 'Identificador expuesto en la API (/api/tickets/{public_id}).';
COMMENT ON COLUMN ticket.qr_code IS 'Token aleatorio de al menos 32 caracteres (UUID o similar). Nunca secuencial.';
