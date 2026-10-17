-- =====================================================================
-- Verificación de datos semilla · reservation_db
-- Ejecutar después de 02_seed.sql. Solo lee: no modifica nada.
--   psql -d reservation_db -v ON_ERROR_STOP=1 -f tests/reservation_db_seed_checks.sql
-- =====================================================================
SET client_encoding = 'UTF8';
BEGIN;
\pset tuples_only on
\i tests/00_test_helpers.sql

SELECT pg_temp.expect_true('parameter tiene al menos 5 registros',   $$SELECT COUNT(*) >= 5 FROM parameter$$);
SELECT pg_temp.expect_true('reservation tiene al menos 5 registros', $$SELECT COUNT(*) >= 5 FROM reservation$$);
SELECT pg_temp.expect_true('seat_lock tiene al menos 5 registros',   $$SELECT COUNT(*) >= 5 FROM seat_lock$$);
SELECT pg_temp.expect_true('ticket tiene al menos 5 registros',      $$SELECT COUNT(*) >= 5 FROM ticket$$);

SELECT pg_temp.expect_true('Hay reservas en los 4 estados',
  $$SELECT COUNT(DISTINCT status) = 4 FROM reservation$$);
SELECT pg_temp.expect_true('Ningún cliente supera MAX_TICKETS_PER_MATCH en un partido',
  $$SELECT NOT EXISTS (
      SELECT 1 FROM seat_lock l JOIN reservation r ON r.id = l.reservation_id
      GROUP BY r.client_id, r.match_id
      HAVING COUNT(*) > (SELECT value::int FROM parameter WHERE code = 'MAX_TICKETS_PER_MATCH'))$$);
SELECT pg_temp.expect_true('seat_lock.match_id coincide con el partido de su reserva',
  $$SELECT NOT EXISTS (SELECT 1 FROM seat_lock l JOIN reservation r ON r.id = l.reservation_id
                       WHERE l.match_id <> r.match_id)$$);
SELECT pg_temp.expect_true('HELD solo en reservas PENDIENTE; RESERVED solo en CONFIRMADA',
  $$SELECT NOT EXISTS (SELECT 1 FROM seat_lock l JOIN reservation r ON r.id = l.reservation_id
                       WHERE (l.status = 'HELD' AND r.status <> 'PENDIENTE')
                          OR (l.status = 'RESERVED' AND r.status <> 'CONFIRMADA'))$$);
SELECT pg_temp.expect_true('Reservas canceladas y expiradas no bloquean asientos',
  $$SELECT NOT EXISTS (SELECT 1 FROM seat_lock l JOIN reservation r ON r.id = l.reservation_id
                       WHERE r.status IN ('CANCELADA', 'EXPIRADA'))$$);
SELECT pg_temp.expect_true('Cada reserva confirmada tiene un ticket por asiento',
  $$SELECT NOT EXISTS (SELECT 1 FROM reservation r WHERE r.status = 'CONFIRMADA'
       AND (SELECT COUNT(*) FROM ticket t WHERE t.reservation_id = r.id)
        <> (SELECT COUNT(*) FROM seat_lock l WHERE l.reservation_id = r.id))$$);
SELECT pg_temp.expect_true('Pendientes y expiradas no tienen tickets',
  $$SELECT NOT EXISTS (SELECT 1 FROM ticket t JOIN reservation r ON r.id = t.reservation_id
                       WHERE r.status IN ('PENDIENTE', 'EXPIRADA'))$$);
SELECT pg_temp.expect_true('Los tickets de reservas canceladas están ANULADOS',
  $$SELECT NOT EXISTS (SELECT 1 FROM ticket t JOIN reservation r ON r.id = t.reservation_id
                       WHERE r.status = 'CANCELADA' AND t.status <> 'ANULADO')$$);
SELECT pg_temp.expect_true('Escenario 422 listo: el cliente 1 tiene exactamente 6 entradas del partido 1',
  $$SELECT COUNT(*) = 6 FROM seat_lock l JOIN reservation r ON r.id = l.reservation_id
    WHERE r.client_id = 1 AND r.match_id = 1$$);

SELECT pg_temp.report();
ROLLBACK;
