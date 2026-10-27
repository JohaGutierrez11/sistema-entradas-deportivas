-- =====================================================================
-- match_db · ms-partidos · DATOS SEMILLA
-- HU-04 · Al menos 5 registros por tabla
--
-- Requiere haber ejecutado 01_schema.sql.
-- Se puede ejecutar varias veces: vacía las tablas y reinicia los ids.
--
-- Datos de demostración con proporciones reales: 7 localidades por estadio
-- (8 460 asientos), como en un estadio de fútbol ecuatoriano: palcos al
-- centro de occidente, preferencia a sus lados, tribuna en oriente y
-- general detrás de los arcos. total_capacity conserva el aforo real; las
-- localidades modeladas son una parte (el modelo admite hasta 26 filas x
-- 100 asientos por sección).
-- =====================================================================

SET client_encoding = 'UTF8';

TRUNCATE TABLE sport_match, seat, seat_row, section, stadium RESTART IDENTITY CASCADE;

-- ---------------------------------------------------------------------
-- stadium (capacidades totales aproximadas)
-- ---------------------------------------------------------------------
INSERT INTO stadium (id, name, description, location, total_capacity) VALUES
  (1, 'Estadio Olímpico Atahualpa',         'Escenario principal de Quito',               'Av. 6 de Diciembre y Naciones Unidas, Quito', 35000),
  (2, 'Estadio Rodrigo Paz Delgado',        'Conocido como Casa Blanca',                  'Av. Diego de Vásquez, Ponceano, Quito',        41000),
  (3, 'Estadio Monumental Banco Pichincha', 'El estadio de mayor capacidad del país',     'Av. Barcelona, Guayaquil',                     57000),
  (4, 'Estadio George Capwell',             'Escenario del barrio Centenario',            'Av. Quito y San Martín, Guayaquil',            40000),
  (5, 'Estadio Gonzalo Pozo Ripalda',       'Ubicado en el sur de Quito',                 'Chillogallo, Quito',                           18000);

-- Coliseo (el enunciado pide escenarios de tipo estadio y coliseo). Aforo referencial.
INSERT INTO stadium (id, name, description, location, total_capacity, venue_type) VALUES
  (6, 'Coliseo General Rumiñahui', 'Escenario cubierto para básquet, vóley y otros deportes', 'Quito', 8000, 'COLISEO');

-- ---------------------------------------------------------------------
-- section: 7 localidades por estadio (la misma plantilla en los 5).
-- capacity = row_count * seats_per_row (lo exige ck_section_capacity).
--   #  Localidad                       Tipo         Filas x asientos   Capacidad
--   1  Palco Occidental                PALCOS        8 x 25  (A-H)        200
--   2  Preferencia Occidental Norte    PREFERENCIA  20 x 40  (A-T)        800
--   3  Preferencia Occidental Sur      PREFERENCIA  20 x 40  (A-T)        800
--   4  Tribuna Oriental Norte          TRIBUNAS     25 x 50  (A-Y)      1 250
--   5  Tribuna Oriental Sur            TRIBUNAS     25 x 50  (A-Y)      1 250
--   6  General Norte                   GENERAL      26 x 80  (A-Z)      2 080
--   7  General Sur                     GENERAL      26 x 80  (A-Z)      2 080
--                                                                      -----
--                                                                      8 460
-- Recuadros sobre el plano (HU-38), en %: la cancha ocupa x 28-72, y 25-75.
--   Occidente (izquierda): Preferencia Norte, Palco al centro, Preferencia Sur.
--   Oriente (derecha): Tribuna Norte y Sur. Norte y Sur: General, detrás de los arcos.
-- ---------------------------------------------------------------------
INSERT INTO section (id, stadium_id, name, description, section_type, row_count, seats_per_row, capacity,
                     pos_x, pos_y, width, height)
SELECT (s.id - 1) * 7 + t.ord,
       s.id, t.name, t.description, t.section_type, t.row_count, t.seats_per_row,
       t.row_count * t.seats_per_row,
       t.pos_x, t.pos_y, t.width, t.height
FROM stadium s
CROSS JOIN (VALUES
  (1, 'Palco Occidental',             'Palcos privados frente a la mitad de la cancha', 'PALCOS',       8, 25,  4, 40, 22, 20),
  (2, 'Preferencia Occidental Norte', 'Graderío numerado bajo techo, lado norte',       'PREFERENCIA', 20, 40,  4, 18, 22, 20),
  (3, 'Preferencia Occidental Sur',   'Graderío numerado bajo techo, lado sur',         'PREFERENCIA', 20, 40,  4, 62, 22, 20),
  (4, 'Tribuna Oriental Norte',       'Graderío frente a la preferencia, lado norte',   'TRIBUNAS',    25, 50, 74, 18, 22, 30),
  (5, 'Tribuna Oriental Sur',         'Graderío frente a la preferencia, lado sur',     'TRIBUNAS',    25, 50, 74, 52, 22, 30),
  (6, 'General Norte',                'Graderío popular detrás del arco norte',         'GENERAL',     26, 80, 28,  3, 44, 20),
  (7, 'General Sur',                  'Graderío popular detrás del arco sur',           'GENERAL',     26, 80, 28, 77, 44, 20)
) AS t(ord, name, description, section_type, row_count, seats_per_row, pos_x, pos_y, width, height)
WHERE s.venue_type = 'ESTADIO'
ORDER BY s.id, t.ord;

-- Localidades del coliseo (ids 36 a 41, después de las de los estadios): graderíos alrededor
-- de la cancha, más pequeños que los de un estadio. Total: 2 140 asientos.
INSERT INTO section (id, stadium_id, name, description, section_type, row_count, seats_per_row, capacity,
                     pos_x, pos_y, width, height)
SELECT t.id, 6, t.name, t.description, t.section_type, t.row_count, t.seats_per_row,
       t.row_count * t.seats_per_row, t.pos_x, t.pos_y, t.width, t.height
FROM (VALUES
  (36, 'Palco Central',                'Palcos frente a la mitad de la cancha',  'PALCOS',       4, 20,  4, 40, 22, 20),
  (37, 'Preferencia Occidental Norte', 'Graderío numerado, lado norte',          'PREFERENCIA', 10, 25,  4, 18, 22, 20),
  (38, 'Preferencia Occidental Sur',   'Graderío numerado, lado sur',            'PREFERENCIA', 10, 25,  4, 62, 22, 20),
  (39, 'Tribuna Oriental',             'Graderío frente a la preferencia',       'TRIBUNAS',    15, 40, 74, 18, 22, 64),
  (40, 'General Norte',                'Graderío detrás del tablero norte',      'GENERAL',     12, 40, 28,  3, 44, 20),
  (41, 'General Sur',                  'Graderío detrás del tablero sur',        'GENERAL',     12, 40, 28, 77, 44, 20)
) AS t(id, name, description, section_type, row_count, seats_per_row, pos_x, pos_y, width, height)
ORDER BY t.id;

-- ---------------------------------------------------------------------
-- seat_row: una fila por letra (A, B, C...) según row_count.
-- generate_series(1, n) produce 1..n; chr(64 + n) lo convierte en A..Z.
-- ---------------------------------------------------------------------
INSERT INTO seat_row (section_id, letter)
SELECT s.id, chr(64 + g)
FROM section s
CROSS JOIN LATERAL generate_series(1, s.row_count) AS g
ORDER BY s.id, g;

-- ---------------------------------------------------------------------
-- seat: asientos 1..seats_per_row en cada fila.
-- Con el ORDER BY y los ids reiniciados, la numeración es siempre igual.
-- Estadio 1 (Atahualpa) ocupa los seat.id 1..8460, en este orden:
--   Palco Occidental              1 -  200   (fila A = 1-25, B = 26-50, ... H = 176-200)
--   Preferencia Occidental Norte  201 - 1000 (fila A = 201-240, B = 241-280, ...)
--   Preferencia Occidental Sur    1001 - 1800
--   Tribuna Oriental Norte        1801 - 3050 (fila A = 1801-1850, ...)
--   Tribuna Oriental Sur          3051 - 4300
--   General Norte                 4301 - 6380 (fila A = 4301-4380, ...)
--   General Sur                   6381 - 8460
-- Cada estadio siguiente suma 8460: estadio N empieza en (N-1)*8460 + 1.
-- El coliseo (escenario 6) va después de los 5 estadios: seat.id 42301..44440.
-- Fórmula: id = inicio_estadio + inicio_sección + (fila - 1) * asientos_por_fila + número - 1.
-- ---------------------------------------------------------------------
INSERT INTO seat (seat_row_id, seat_number)
SELECT r.id, n
FROM seat_row r
JOIN section s ON s.id = r.section_id
CROSS JOIN LATERAL generate_series(1, s.seats_per_row) AS n
ORDER BY r.id, n;

-- ---------------------------------------------------------------------
-- sport_match (partidos ficticios entre clubes reales)
-- id 5 ya se jugó (sirve para tickets UTILIZADOS y reportes históricos).
-- id 6 está CANCELADO (no debe permitir nuevas reservas).
-- ---------------------------------------------------------------------
INSERT INTO sport_match (id, name, description, match_date, match_time, status, stadium_id) VALUES
  (1, 'El Nacional vs Barcelona SC',              'LigaPro · Fecha 14', '2026-11-07', '15:30', 'PROGRAMADO', 1),
  (2, 'Liga de Quito vs Independiente del Valle', 'LigaPro · Fecha 14', '2026-11-08', '18:00', 'PROGRAMADO', 2),
  (3, 'Emelec vs Barcelona SC',                   'Clásico del Astillero', '2026-11-14', '19:00', 'PROGRAMADO', 4),
  (4, 'Barcelona SC vs Liga de Quito',            'LigaPro · Fecha 16', '2026-11-21', '17:00', 'PROGRAMADO', 3),
  (5, 'Aucas vs Deportivo Cuenca',                'LigaPro · Fecha 11', '2026-10-10', '15:00', 'FINALIZADO', 5),
  (6, 'Universidad Católica vs El Nacional',      'LigaPro · Fecha 17', '2026-11-28', '12:00', 'CANCELADO',  1),
  (7, 'Final de la Liga Ecuatoriana de Baloncesto', 'Baloncesto · Final', '2026-11-15', '19:30', 'PROGRAMADO', 6);

-- ---------------------------------------------------------------------
-- Reiniciar las secuencias de las tablas con ids explícitos.
-- ---------------------------------------------------------------------
DO $$
BEGIN
    PERFORM setval(pg_get_serial_sequence('stadium',     'id'), (SELECT MAX(id) FROM stadium));
    PERFORM setval(pg_get_serial_sequence('section',     'id'), (SELECT MAX(id) FROM section));
    PERFORM setval(pg_get_serial_sequence('sport_match', 'id'), (SELECT MAX(id) FROM sport_match));
END $$;
