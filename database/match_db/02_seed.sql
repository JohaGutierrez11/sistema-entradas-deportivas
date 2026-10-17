-- =====================================================================
-- match_db · ms-partidos · DATOS SEMILLA
-- HU-04 · Al menos 5 registros por tabla
--
-- Requiere haber ejecutado 01_schema.sql.
-- Se puede ejecutar varias veces: vacía las tablas y reinicia los ids.
--
-- Datos de demostración: los estadios conservan una capacidad total
-- referencial, pero solo se modelan 4 localidades pequeñas por estadio
-- (122 asientos) para que el mapa gráfico sea manejable.
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

-- ---------------------------------------------------------------------
-- section: 4 localidades por estadio, una de cada tipo.
-- capacity = row_count * seats_per_row (lo exige ck_section_capacity).
--   Palco Occidental        PALCOS       2 filas x  5 = 10
--   Preferencia Occidental  PREFERENCIA  3 filas x  8 = 24
--   Tribuna Oriental        TRIBUNAS     4 filas x 10 = 40
--   General Norte           GENERAL      4 filas x 12 = 48   → 122 por estadio
-- ---------------------------------------------------------------------
INSERT INTO section (id, stadium_id, name, description, section_type, row_count, seats_per_row, capacity)
SELECT (s.id - 1) * 4 + t.ord,
       s.id, t.name, t.description, t.section_type, t.row_count, t.seats_per_row,
       t.row_count * t.seats_per_row
FROM stadium s
CROSS JOIN (VALUES
  (1, 'Palco Occidental',       'Palcos privados con vista central', 'PALCOS',      2,  5),
  (2, 'Preferencia Occidental', 'Graderío con asiento numerado',     'PREFERENCIA', 3,  8),
  (3, 'Tribuna Oriental',       'Graderío frente a la preferencia',  'TRIBUNAS',    4, 10),
  (4, 'General Norte',          'Graderío popular detrás del arco',  'GENERAL',     4, 12)
) AS t(ord, name, description, section_type, row_count, seats_per_row)
ORDER BY s.id, t.ord;

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
-- Estadio 1 (Atahualpa) ocupa los seat.id 1..122:
--   Palco Occidental        A1-A5 = 1-5,    B1-B5 = 6-10
--   Preferencia Occidental  A1-A8 = 11-18,  B = 19-26,  C = 27-34
--   Tribuna Oriental        A1-A10 = 35-44, B = 45-54,  C = 55-64,  D = 65-74
--   General Norte           A1-A12 = 75-86, B = 87-98,  C = 99-110, D = 111-122
-- Cada estadio siguiente suma 122: estadio N empieza en (N-1)*122 + 1.
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
  (6, 'Universidad Católica vs El Nacional',      'LigaPro · Fecha 17', '2026-11-28', '12:00', 'CANCELADO',  1);

-- ---------------------------------------------------------------------
-- Reiniciar las secuencias de las tablas con ids explícitos.
-- ---------------------------------------------------------------------
DO $$
BEGIN
    PERFORM setval(pg_get_serial_sequence('stadium',     'id'), (SELECT MAX(id) FROM stadium));
    PERFORM setval(pg_get_serial_sequence('section',     'id'), (SELECT MAX(id) FROM section));
    PERFORM setval(pg_get_serial_sequence('sport_match', 'id'), (SELECT MAX(id) FROM sport_match));
END $$;
