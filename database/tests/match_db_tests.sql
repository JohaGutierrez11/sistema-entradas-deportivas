-- =====================================================================
-- Pruebas de restricciones · match_db
--   psql -d match_db -v ON_ERROR_STOP=1 -f tests/match_db_tests.sql
-- =====================================================================
SET client_encoding = 'UTF8';
BEGIN;
\pset tuples_only on
\i tests/00_test_helpers.sql

-- Datos base válidos
SELECT pg_temp.expect_ok('Crear estadio válido',
  $$INSERT INTO stadium (id, name, location, total_capacity) VALUES (901, 'T_Estadio', 'Quito', 1000)$$);
SELECT pg_temp.expect_ok('Crear sección válida (2 filas x 5 asientos = 10)',
  $$INSERT INTO section (id, stadium_id, name, section_type, row_count, seats_per_row, capacity)
    VALUES (901, 901, 'Palco Norte', 'PALCOS', 2, 5, 10)$$);
SELECT pg_temp.expect_ok('Crear fila A',
  $$INSERT INTO seat_row (id, section_id, letter) VALUES (901, 901, 'A')$$);
SELECT pg_temp.expect_ok('Crear asiento A-5',
  $$INSERT INTO seat (seat_row_id, seat_number) VALUES (901, 5)$$);
SELECT pg_temp.expect_ok('Crear partido válido',
  $$INSERT INTO sport_match (name, match_date, match_time, stadium_id) VALUES ('T_Partido', '2026-11-07', '16:00', 901)$$);

-- stadium
SELECT pg_temp.expect_violation('Estadio con nombre repetido',
  $$INSERT INTO stadium (name, location, total_capacity) VALUES ('T_Estadio', 'Guayaquil', 500)$$, 'uk_stadium_name');
SELECT pg_temp.expect_violation('Estadio con capacidad 0',
  $$INSERT INTO stadium (name, location, total_capacity) VALUES ('T_Otro', 'Quito', 0)$$, 'ck_stadium_capacity');

-- section
SELECT pg_temp.expect_violation('Tipo de sección fuera del catálogo',
  $$INSERT INTO section (stadium_id, name, section_type, row_count, seats_per_row, capacity)
    VALUES (901, 'VIP', 'VIP', 1, 1, 1)$$, 'ck_section_type');
SELECT pg_temp.expect_violation('Capacidad que no cuadra con filas x asientos',
  $$INSERT INTO section (stadium_id, name, section_type, row_count, seats_per_row, capacity)
    VALUES (901, 'General Sur', 'GENERAL', 10, 20, 150)$$, 'ck_section_capacity');
SELECT pg_temp.expect_violation('Sección con 0 filas',
  $$INSERT INTO section (stadium_id, name, section_type, row_count, seats_per_row, capacity)
    VALUES (901, 'General Sur', 'GENERAL', 0, 20, 0)$$, 'ck_section_row_count');
SELECT pg_temp.expect_violation('Dos secciones con el mismo nombre en un estadio',
  $$INSERT INTO section (stadium_id, name, section_type, row_count, seats_per_row, capacity)
    VALUES (901, 'Palco Norte', 'PALCOS', 1, 1, 1)$$, 'uk_section_stadium_name');
SELECT pg_temp.expect_violation('Recuadro incompleto (falta el alto)',
  $$INSERT INTO section (stadium_id, name, section_type, row_count, seats_per_row, capacity, pos_x, pos_y, width)
    VALUES (901, 'R1', 'GENERAL', 1, 1, 1, 10, 10, 20)$$, 'ck_section_box_complete');
SELECT pg_temp.expect_violation('Recuadro que se sale del plano (x 90 + ancho 20)',
  $$INSERT INTO section (stadium_id, name, section_type, row_count, seats_per_row, capacity, pos_x, pos_y, width, height)
    VALUES (901, 'R2', 'GENERAL', 1, 1, 1, 90, 10, 20, 10)$$, 'ck_section_box_inside');
SELECT pg_temp.expect_violation('Recuadro demasiado pequeño (ancho 4)',
  $$INSERT INTO section (stadium_id, name, section_type, row_count, seats_per_row, capacity, pos_x, pos_y, width, height)
    VALUES (901, 'R3', 'GENERAL', 1, 1, 1, 10, 10, 4, 10)$$, 'ck_section_box_inside');
SELECT pg_temp.expect_ok('Recuadro en el borde exacto del plano (x 80 + ancho 20 = 100)',
  $$INSERT INTO section (stadium_id, name, section_type, row_count, seats_per_row, capacity, pos_x, pos_y, width, height)
    VALUES (901, 'R4', 'GENERAL', 1, 1, 1, 80, 80, 20, 20)$$);
SELECT pg_temp.expect_violation('Sección en estadio inexistente',
  $$INSERT INTO section (stadium_id, name, section_type, row_count, seats_per_row, capacity)
    VALUES (999999, 'X', 'GENERAL', 1, 1, 1)$$, 'fk_section_stadium');

-- seat_row y seat
SELECT pg_temp.expect_violation('Fila repetida en la misma sección',
  $$INSERT INTO seat_row (section_id, letter) VALUES (901, 'A')$$, 'uk_seat_row_section_letter');
SELECT pg_temp.expect_violation('Fila con minúscula o número',
  $$INSERT INTO seat_row (section_id, letter) VALUES (901, 'b1')$$, 'ck_seat_row_letter');
SELECT pg_temp.expect_violation('Asiento repetido en la misma fila',
  $$INSERT INTO seat (seat_row_id, seat_number) VALUES (901, 5)$$, 'uk_seat_row_number');
SELECT pg_temp.expect_violation('Asiento número 0',
  $$INSERT INTO seat (seat_row_id, seat_number) VALUES (901, 0)$$, 'ck_seat_number');

-- sport_match
SELECT pg_temp.expect_violation('Partido con estado inválido',
  $$INSERT INTO sport_match (name, match_date, match_time, status, stadium_id)
    VALUES ('X', '2026-11-08', '16:00', 'SUSPENDIDO', 901)$$, 'ck_sport_match_status');
SELECT pg_temp.expect_violation('Partido sin estadio',
  $$INSERT INTO sport_match (name, match_date, match_time) VALUES ('X', '2026-11-08', '16:00')$$, 'NOT NULL:stadium_id');

-- ON DELETE RESTRICT
SELECT pg_temp.expect_violation('Borrar un estadio que tiene secciones',
  $$DELETE FROM stadium WHERE id = 901$$, 'fk_section_stadium');
SELECT pg_temp.expect_violation('Borrar una fila que tiene asientos',
  $$DELETE FROM seat_row WHERE id = 901$$, 'fk_seat_seat_row');

SELECT pg_temp.report();
ROLLBACK;
