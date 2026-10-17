-- =====================================================================
-- match_db · ms-partidos
-- Esquema: stadium, section, seat_row, seat, sport_match
-- HU-03 · Script SQL de creación
--
-- El script se puede ejecutar varias veces: borra las tablas antes de
-- crearlas. Úsalo solo en desarrollo; borra todos los datos.
-- =====================================================================

-- Los archivos están en UTF-8 (tildes y ñ en comentarios y datos).
SET client_encoding = 'UTF8';

DROP TABLE IF EXISTS sport_match CASCADE;
DROP TABLE IF EXISTS seat        CASCADE;
DROP TABLE IF EXISTS seat_row    CASCADE;
DROP TABLE IF EXISTS section     CASCADE;
DROP TABLE IF EXISTS stadium     CASCADE;

-- ---------------------------------------------------------------------
-- stadium: escenario deportivo
-- ---------------------------------------------------------------------
CREATE TABLE stadium (
    id             BIGSERIAL    NOT NULL,
    name           VARCHAR(100) NOT NULL,
    description    VARCHAR(250),
    location       VARCHAR(200) NOT NULL,
    total_capacity INT          NOT NULL,

    CONSTRAINT pk_stadium          PRIMARY KEY (id),
    CONSTRAINT uk_stadium_name     UNIQUE (name),
    CONSTRAINT ck_stadium_capacity CHECK (total_capacity > 0)
);

-- ---------------------------------------------------------------------
-- section: localidad del estadio
-- capacity se guarda porque el enunciado lo pide, y el CHECK impide
-- que contradiga a row_count * seats_per_row.
-- ---------------------------------------------------------------------
CREATE TABLE section (
    id            BIGSERIAL    NOT NULL,
    stadium_id    BIGINT       NOT NULL,
    name          VARCHAR(50)  NOT NULL,
    description   VARCHAR(250),
    section_type  VARCHAR(15)  NOT NULL,
    row_count     INT          NOT NULL,
    seats_per_row INT          NOT NULL,
    capacity      INT          NOT NULL,

    CONSTRAINT pk_section               PRIMARY KEY (id),
    CONSTRAINT fk_section_stadium       FOREIGN KEY (stadium_id) REFERENCES stadium (id) ON DELETE RESTRICT,
    CONSTRAINT uk_section_stadium_name  UNIQUE (stadium_id, name),
    CONSTRAINT ck_section_type          CHECK (section_type IN ('PALCOS', 'GENERAL', 'PREFERENCIA', 'TRIBUNAS')),
    CONSTRAINT ck_section_row_count     CHECK (row_count > 0),
    CONSTRAINT ck_section_seats_per_row CHECK (seats_per_row > 0),
    CONSTRAINT ck_section_capacity      CHECK (capacity = row_count * seats_per_row)
);

CREATE INDEX idx_section_stadium ON section (stadium_id);

COMMENT ON COLUMN section.capacity IS 'Siempre igual a row_count * seats_per_row (ck_section_capacity).';

-- ---------------------------------------------------------------------
-- seat_row: fila de una sección (A, B, C...)
-- Se llama seat_row porque "row" es palabra reservada.
-- ---------------------------------------------------------------------
CREATE TABLE seat_row (
    id         BIGSERIAL  NOT NULL,
    section_id BIGINT     NOT NULL,
    letter     VARCHAR(2) NOT NULL,

    CONSTRAINT pk_seat_row                PRIMARY KEY (id),
    CONSTRAINT fk_seat_row_section        FOREIGN KEY (section_id) REFERENCES section (id) ON DELETE RESTRICT,
    CONSTRAINT uk_seat_row_section_letter UNIQUE (section_id, letter),
    CONSTRAINT ck_seat_row_letter         CHECK (letter ~ '^[A-Z]{1,2}$')
);

CREATE INDEX idx_seat_row_section ON seat_row (section_id);

-- ---------------------------------------------------------------------
-- seat: asiento físico. NO tiene estado: el estado depende del partido
-- y vive en reservation_db.seat_lock.
-- ---------------------------------------------------------------------
CREATE TABLE seat (
    id          BIGSERIAL NOT NULL,
    seat_row_id BIGINT    NOT NULL,
    seat_number INT       NOT NULL,

    CONSTRAINT pk_seat            PRIMARY KEY (id),
    CONSTRAINT fk_seat_seat_row   FOREIGN KEY (seat_row_id) REFERENCES seat_row (id) ON DELETE RESTRICT,
    CONSTRAINT uk_seat_row_number UNIQUE (seat_row_id, seat_number),
    CONSTRAINT ck_seat_number     CHECK (seat_number > 0)
);

CREATE INDEX idx_seat_seat_row ON seat (seat_row_id);

COMMENT ON TABLE seat IS 'Asiento físico. Su disponibilidad por partido está en reservation_db.seat_lock.';

-- ---------------------------------------------------------------------
-- sport_match: partido
-- Se llama sport_match porque "match" es palabra clave de SQL.
-- ---------------------------------------------------------------------
CREATE TABLE sport_match (
    id          BIGSERIAL    NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(250),
    match_date  DATE         NOT NULL,
    match_time  TIME         NOT NULL,
    status      VARCHAR(15)  NOT NULL DEFAULT 'PROGRAMADO',
    stadium_id  BIGINT       NOT NULL,

    CONSTRAINT pk_sport_match         PRIMARY KEY (id),
    CONSTRAINT fk_sport_match_stadium FOREIGN KEY (stadium_id) REFERENCES stadium (id) ON DELETE RESTRICT,
    CONSTRAINT ck_sport_match_status  CHECK (status IN ('PROGRAMADO', 'EN_CURSO', 'FINALIZADO', 'CANCELADO'))
);

CREATE INDEX idx_sport_match_stadium ON sport_match (stadium_id);
-- Consulta de partidos con filtro por fecha (requisito del catálogo).
CREATE INDEX idx_sport_match_date    ON sport_match (match_date);
