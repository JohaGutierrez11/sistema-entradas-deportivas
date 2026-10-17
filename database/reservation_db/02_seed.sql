-- =====================================================================
-- reservation_db · ms-reservas · DATOS SEMILLA
-- HU-04 · Al menos 5 registros por tabla
--
-- Requiere haber ejecutado 01_schema.sql.
-- Se puede ejecutar varias veces: vacía las tablas y reinicia los ids.
--
-- Las referencias lógicas apuntan a los ids de las otras semillas:
--   client_id → auth_db.client   (1 María, 2 Carlos, 3 Andrea, 4 Diego)
--   match_id  → match_db.sport_match
--   seat_id   → match_db.seat    (el asiento debe ser del estadio del partido)
-- Ver el mapa de seat.id en match_db/02_seed.sql.
--
-- Escenarios preparados para probar las reglas de negocio:
--   · María (cliente 1) ya tiene 6 entradas del partido 1 → una más = 422.
--   · Palco Occidental A1 del partido 1 (seat 1) está vendido → reservarlo = 409.
--   · La reserva 3 está PENDIENTE con asientos HELD → la tarea programada
--     la pasará a EXPIRADA cuando venza expires_at (10 minutos después de
--     ejecutar este script).
-- =====================================================================

SET client_encoding = 'UTF8';

TRUNCATE TABLE ticket, seat_lock, reservation, parameter RESTART IDENTITY CASCADE;

-- ---------------------------------------------------------------------
-- parameter: reglas configurables
-- ---------------------------------------------------------------------
INSERT INTO parameter (id, code, value, description) VALUES
  (1, 'MAX_TICKETS_PER_MATCH',        '6',   'Máximo de entradas por cliente y partido'),
  (2, 'HOLD_MINUTES',                 '10',  'Minutos que un asiento queda retenido antes de confirmar'),
  (3, 'EXPIRATION_JOB_SECONDS',       '60',  'Cada cuántos segundos se buscan reservas pendientes vencidas'),
  (4, 'AVAILABILITY_REFRESH_SECONDS', '5',   'Cada cuántos segundos el mapa de asientos consulta la disponibilidad'),
  (5, 'RESERVATION_CODE_PREFIX',      'RES', 'Prefijo del código legible de reserva');

-- ---------------------------------------------------------------------
-- reservation
-- ---------------------------------------------------------------------
INSERT INTO reservation (id, code, client_id, match_id, status, created_at, expires_at, confirmed_at, cancelled_at) VALUES
  -- Partido 1 · Estadio 1 (seat.id 1..122)
  (1, 'RES-2026-000001', 1, 1, 'CONFIRMADA', '2026-10-15 19:40', '2026-10-15 19:50', '2026-10-15 19:43', NULL),
  (2, 'RES-2026-000002', 2, 1, 'CONFIRMADA', '2026-10-15 20:02', '2026-10-15 20:12', '2026-10-15 20:05', NULL),
  (3, 'RES-2026-000003', 3, 1, 'PENDIENTE',  now(),              now() + interval '10 minutes', NULL, NULL),
  (4, 'RES-2026-000004', 4, 1, 'CANCELADA',  '2026-10-16 11:10', '2026-10-16 11:20', '2026-10-16 11:14', '2026-10-16 18:00'),
  -- Partido 5 (ya jugado) · Estadio 5 (seat.id 489..610)
  (5, 'RES-2026-000005', 2, 5, 'CONFIRMADA', '2026-10-05 10:00', '2026-10-05 10:10', '2026-10-05 10:04', NULL),
  -- Partido 2 · Estadio 2: el cliente nunca confirmó
  (6, 'RES-2026-000006', 3, 2, 'EXPIRADA',   '2026-10-16 10:00', '2026-10-16 10:10', NULL, NULL),
  -- Partido 3 · Estadio 4 (seat.id 367..488)
  (7, 'RES-2026-000007', 4, 3, 'CONFIRMADA', '2026-10-17 08:55', '2026-10-17 09:05', '2026-10-17 08:58', NULL);

-- ---------------------------------------------------------------------
-- seat_lock: solo asientos tomados en este momento.
-- Las reservas CANCELADA y EXPIRADA no tienen filas: sus asientos se liberaron.
-- ---------------------------------------------------------------------
INSERT INTO seat_lock (reservation_id, match_id, seat_id, status) VALUES
  -- Reserva 1 · María · Palco Occidental A1-A5 y B1 (6 entradas: el máximo)
  (1, 1,   1, 'RESERVED'), (1, 1,   2, 'RESERVED'), (1, 1,   3, 'RESERVED'),
  (1, 1,   4, 'RESERVED'), (1, 1,   5, 'RESERVED'), (1, 1,   6, 'RESERVED'),
  -- Reserva 2 · Carlos · Preferencia Occidental A1-A2
  (2, 1,  11, 'RESERVED'), (2, 1,  12, 'RESERVED'),
  -- Reserva 3 · Andrea · Tribuna Oriental A1-A3, retenidos mientras confirma
  (3, 1,  35, 'HELD'),     (3, 1,  36, 'HELD'),     (3, 1,  37, 'HELD'),
  -- Reserva 5 · Carlos · Estadio 5, Palco Occidental A1-A2 (partido ya jugado)
  (5, 5, 489, 'RESERVED'), (5, 5, 490, 'RESERVED'),
  -- Reserva 7 · Diego · Estadio 4, General Norte A1-A4 (366 + 75..78)
  (7, 3, 441, 'RESERVED'), (7, 3, 442, 'RESERVED'), (7, 3, 443, 'RESERVED'), (7, 3, 444, 'RESERVED');

-- ---------------------------------------------------------------------
-- ticket: una entrada por asiento de cada reserva confirmada.
-- qr_code: token aleatorio (UUID), nunca secuencial.
-- La reserva 4 se canceló después de confirmar: sus tickets quedan ANULADOS
-- como historial, aunque sus asientos ya se liberaron.
-- ---------------------------------------------------------------------
INSERT INTO ticket (reservation_id, seat_id, qr_code, status, issued_at, used_at)
SELECT l.reservation_id, l.seat_id, gen_random_uuid()::text, 'ACTIVO', r.confirmed_at, NULL
FROM seat_lock l
JOIN reservation r ON r.id = l.reservation_id
WHERE l.status = 'RESERVED'
ORDER BY l.reservation_id, l.seat_id;

INSERT INTO ticket (reservation_id, seat_id, qr_code, status, issued_at) VALUES
  (4, 75, gen_random_uuid()::text, 'ANULADO', '2026-10-16 11:14'),
  (4, 76, gen_random_uuid()::text, 'ANULADO', '2026-10-16 11:14');

-- Partido 5 ya jugado: Carlos ingresó con la entrada del asiento 489;
-- la del 490 nunca se usó.
UPDATE ticket SET status = 'UTILIZADO', used_at = '2026-10-10 14:20'
WHERE reservation_id = 5 AND seat_id = 489;

-- ---------------------------------------------------------------------
-- Reiniciar las secuencias de las tablas con ids explícitos.
-- ---------------------------------------------------------------------
DO $$
BEGIN
    PERFORM setval(pg_get_serial_sequence('parameter',   'id'), (SELECT MAX(id) FROM parameter));
    PERFORM setval(pg_get_serial_sequence('reservation', 'id'), (SELECT MAX(id) FROM reservation));
END $$;
