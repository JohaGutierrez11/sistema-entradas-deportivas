-- =====================================================================
-- Verificación de datos semilla · match_db
-- Ejecutar después de 02_seed.sql. Solo lee: no modifica nada.
--   psql -d match_db -v ON_ERROR_STOP=1 -f tests/match_db_seed_checks.sql
-- =====================================================================
SET client_encoding = 'UTF8';
BEGIN;
\pset tuples_only on
\i tests/00_test_helpers.sql

SELECT pg_temp.expect_true('stadium tiene al menos 5 registros',     $$SELECT COUNT(*) >= 5 FROM stadium$$);
SELECT pg_temp.expect_true('section tiene al menos 5 registros',     $$SELECT COUNT(*) >= 5 FROM section$$);
SELECT pg_temp.expect_true('seat_row tiene al menos 5 registros',    $$SELECT COUNT(*) >= 5 FROM seat_row$$);
SELECT pg_temp.expect_true('seat tiene al menos 5 registros',        $$SELECT COUNT(*) >= 5 FROM seat$$);
SELECT pg_temp.expect_true('sport_match tiene al menos 5 registros', $$SELECT COUNT(*) >= 5 FROM sport_match$$);

SELECT pg_temp.expect_true('Los 4 tipos de sección están presentes',
  $$SELECT COUNT(DISTINCT section_type) = 4 FROM section$$);
SELECT pg_temp.expect_true('Cada sección tiene tantas filas como row_count',
  $$SELECT NOT EXISTS (SELECT 1 FROM section s
                       WHERE s.row_count <> (SELECT COUNT(*) FROM seat_row r WHERE r.section_id = s.id))$$);
SELECT pg_temp.expect_true('Cada sección tiene tantos asientos como su capacidad',
  $$SELECT NOT EXISTS (SELECT 1 FROM section s
                       WHERE s.capacity <> (SELECT COUNT(*) FROM seat t JOIN seat_row r ON r.id = t.seat_row_id
                                            WHERE r.section_id = s.id))$$);
SELECT pg_temp.expect_true('Las localidades no superan la capacidad total del estadio',
  $$SELECT NOT EXISTS (SELECT 1 FROM stadium st
                       WHERE (SELECT SUM(capacity) FROM section WHERE stadium_id = st.id) > st.total_capacity)$$);
SELECT pg_temp.expect_true('Las filas son letras consecutivas desde A',
  $$SELECT NOT EXISTS (SELECT 1 FROM (
       SELECT letter, ROW_NUMBER() OVER (PARTITION BY section_id ORDER BY letter) AS n FROM seat_row) x
     WHERE x.letter <> chr(64 + x.n::int))$$);
SELECT pg_temp.expect_true('El mapa de ids del README se cumple (Atahualpa, Palco Occidental A1 = seat 1)',
  $$SELECT EXISTS (SELECT 1 FROM seat t JOIN seat_row r ON r.id = t.seat_row_id JOIN section s ON s.id = r.section_id
                   WHERE t.id = 1 AND s.stadium_id = 1 AND s.name = 'Palco Occidental' AND r.letter = 'A' AND t.seat_number = 1)$$);
SELECT pg_temp.expect_true('Hay partidos programados, finalizados y cancelados',
  $$SELECT COUNT(DISTINCT status) >= 3 FROM sport_match$$);

SELECT pg_temp.report();
ROLLBACK;
