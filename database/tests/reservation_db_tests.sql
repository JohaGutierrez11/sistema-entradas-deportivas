-- =====================================================================
-- Pruebas de restricciones · reservation_db
--   psql -d reservation_db -v ON_ERROR_STOP=1 -f tests/reservation_db_tests.sql
-- =====================================================================
SET client_encoding = 'UTF8';
BEGIN;
\pset tuples_only on
\i tests/00_test_helpers.sql

-- Datos base válidos: el cliente 1 reserva el asiento 5 del partido 10
SELECT pg_temp.expect_ok('Crear parámetro',
  $$INSERT INTO parameter (code, value) VALUES ('T_MAX', '6')$$);
SELECT pg_temp.expect_ok('Crear reserva pendiente',
  $$INSERT INTO reservation (id, code, client_id, match_id, created_at, expires_at)
    VALUES (901, 'T-RES-001', 1, 10, '2026-10-17 10:00', '2026-10-17 10:10')$$);
SELECT pg_temp.expect_ok('Retener el asiento 5 del partido 10',
  $$INSERT INTO seat_lock (reservation_id, match_id, seat_id) VALUES (901, 10, 5)$$);
SELECT pg_temp.expect_ok('Reserva de otro cliente para el partido 11',
  $$INSERT INTO reservation (id, code, client_id, match_id, created_at, expires_at)
    VALUES (902, 'T-RES-002', 2, 11, '2026-10-17 10:00', '2026-10-17 10:10')$$);
SELECT pg_temp.expect_ok('El mismo asiento 5, pero en OTRO partido (11)',
  $$INSERT INTO seat_lock (reservation_id, match_id, seat_id) VALUES (902, 11, 5)$$);
SELECT pg_temp.expect_ok('Emitir ticket con QR de 36 caracteres',
  $$INSERT INTO ticket (reservation_id, seat_id, qr_code) VALUES (901, 5, '6f1c2b8e-3a4d-4f6e-9b1a-2c3d4e5f6a7b')$$);

-- public_id
SELECT pg_temp.expect_ok('Dos reservas reciben public_id distintos',
  $q$DO $d$ BEGIN
     IF (SELECT COUNT(DISTINCT public_id) FROM reservation WHERE id IN (901, 902)) <> 2 THEN RAISE EXCEPTION 'public_id repetido o nulo'; END IF;
   END $d$$q$);
SELECT pg_temp.expect_violation('public_id de reserva repetido',
  $$INSERT INTO reservation (public_id, code, client_id, match_id, expires_at)
    SELECT public_id, 'T-RES-X3', 1, 10, now() + interval '10 min' FROM reservation WHERE id = 901$$, 'uk_reservation_public');
SELECT pg_temp.expect_violation('public_id de ticket repetido',
  $$INSERT INTO ticket (public_id, reservation_id, seat_id, qr_code)
    SELECT public_id, 901, 6, 'bbbbbbbb-bbbb-cccc-dddd-eeeeeeeeeeee' FROM ticket WHERE reservation_id = 901$$, 'uk_ticket_public');

-- La regla crítica: doble venta
SELECT pg_temp.expect_violation('DOBLE VENTA: el asiento 5 del partido 10 otra vez',
  $$INSERT INTO reservation (id, code, client_id, match_id, created_at, expires_at)
    VALUES (903, 'T-RES-003', 3, 10, '2026-10-17 10:00', '2026-10-17 10:10');
    INSERT INTO seat_lock (reservation_id, match_id, seat_id) VALUES (903, 10, 5)$$, 'uk_seat_lock_match_seat');

-- parameter
SELECT pg_temp.expect_violation('Parámetro con código repetido',
  $$INSERT INTO parameter (code, value) VALUES ('T_MAX', '8')$$, 'uk_parameter_code');

-- reservation
SELECT pg_temp.expect_violation('Código de reserva repetido',
  $$INSERT INTO reservation (code, client_id, match_id, expires_at) VALUES ('T-RES-001', 1, 10, now() + interval '10 min')$$, 'uk_reservation_code');
SELECT pg_temp.expect_violation('Reserva con estado inválido',
  $$INSERT INTO reservation (code, client_id, match_id, status, expires_at)
    VALUES ('T-RES-X1', 1, 10, 'PAGADA', now() + interval '10 min')$$, 'ck_reservation_status');
SELECT pg_temp.expect_violation('Reserva que expira antes de crearse',
  $$INSERT INTO reservation (code, client_id, match_id, created_at, expires_at)
    VALUES ('T-RES-X2', 1, 10, '2026-10-17 10:00', '2026-10-17 09:00')$$, 'ck_reservation_expires');
SELECT pg_temp.expect_violation('Confirmar sin fecha de confirmación',
  $$UPDATE reservation SET status = 'CONFIRMADA' WHERE id = 901$$, 'ck_reservation_confirmed');
SELECT pg_temp.expect_violation('Cancelar sin fecha de cancelación',
  $$UPDATE reservation SET status = 'CANCELADA' WHERE id = 901$$, 'ck_reservation_cancelled');
SELECT pg_temp.expect_ok('Confirmar con fecha de confirmación',
  $$UPDATE reservation SET status = 'CONFIRMADA', confirmed_at = '2026-10-17 10:05' WHERE id = 901$$);

-- seat_lock
SELECT pg_temp.expect_violation('Bloqueo con estado inválido',
  $$INSERT INTO seat_lock (reservation_id, match_id, seat_id, status) VALUES (901, 10, 6, 'LIBRE')$$, 'ck_seat_lock_status');
SELECT pg_temp.expect_violation('Bloqueo de una reserva inexistente',
  $$INSERT INTO seat_lock (reservation_id, match_id, seat_id) VALUES (999999, 10, 7)$$, 'fk_seat_lock_reservation');

-- ticket
SELECT pg_temp.expect_violation('QR repetido',
  $$INSERT INTO ticket (reservation_id, seat_id, qr_code) VALUES (901, 6, '6f1c2b8e-3a4d-4f6e-9b1a-2c3d4e5f6a7b')$$, 'uk_ticket_qr_code');
SELECT pg_temp.expect_violation('QR corto (adivinable)',
  $$INSERT INTO ticket (reservation_id, seat_id, qr_code) VALUES (901, 6, 'QR-0001')$$, 'ck_ticket_qr_length');
SELECT pg_temp.expect_violation('Dos tickets del mismo asiento en una reserva',
  $$INSERT INTO ticket (reservation_id, seat_id, qr_code) VALUES (901, 5, 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee')$$, 'uk_ticket_reservation_seat');
SELECT pg_temp.expect_violation('Ticket UTILIZADO sin fecha de uso',
  $$UPDATE ticket SET status = 'UTILIZADO' WHERE reservation_id = 901$$, 'ck_ticket_used');
SELECT pg_temp.expect_violation('Borrar una reserva que tiene tickets',
  $$DELETE FROM seat_lock WHERE reservation_id = 901; DELETE FROM reservation WHERE id = 901$$, 'fk_ticket_reservation');

SELECT pg_temp.report();
ROLLBACK;
