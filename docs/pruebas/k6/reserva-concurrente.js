// Prueba de concurrencia y carga · Sistema de gestión de entradas
// Escenario 1 (concurrencia): 50 usuarios virtuales intentan reservar EL MISMO asiento a la vez.
//   Esperado: exactamente 1 reserva creada (201) y el resto rechazadas (409). Lo garantiza UNIQUE(match_id, seat_id).
// Escenario 2 (carga): 20 usuarios consultan la disponibilidad durante 30 s. Esperado: p95 < 500 ms y 0 % de errores.
// Ejecutar:  k6 run docs/pruebas/k6/reserva-concurrente.js
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const AUTH = __ENV.AUTH_URL || 'http://localhost:8081';
const RES = __ENV.RES_URL || 'http://localhost:8083';
const MATCH_ID = 4;                       // partido PROGRAMADO de las semillas (estadio 3)
const SEATS = [16921, 25380];             // rango de asientos del estadio 3
const CLIENTS = ['mlopez', 'cvera', 'aandrade', 'dmorales'];   // clientes activos de las semillas
const PASSWORD = __ENV.CLIENT_PASSWORD || 'Cliente2026*';

const creadas = new Counter('reservas_creadas');
const rechazadas = new Counter('reservas_rechazadas_409');
const inesperadas = new Counter('respuestas_inesperadas');

export const options = {
  scenarios: {
    concurrencia_mismo_asiento: {
      executor: 'per-vu-iterations', vus: 50, iterations: 1, maxDuration: '30s', exec: 'reservarMismoAsiento',
    },
    carga_disponibilidad: {
      executor: 'constant-vus', vus: 20, duration: '30s', startTime: '10s', exec: 'consultarDisponibilidad',
    },
  },
  thresholds: {
    reservas_creadas: ['count==1'],
    respuestas_inesperadas: ['count==0'],
    'http_req_duration{scenario:carga_disponibilidad}': ['p(95)<500'],
    'http_req_failed{scenario:carga_disponibilidad}': ['rate==0'],
  },
};

const json = { headers: { 'Content-Type': 'application/json' } };
const bearer = (t) => ({ headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${t}` } });

export function setup() {
  const tokens = CLIENTS.map((u) => {
    const r = http.post(`${AUTH}/auth-api/v1/auth/login`, JSON.stringify({ username: u, password: PASSWORD }), json);
    if (r.status !== 200) throw new Error(`No se pudo iniciar sesión con ${u}: ${r.status}`);
    return r.json('accessToken');
  });
  // Un asiento libre del partido, elegido al azar para poder repetir la prueba
  const a = http.get(`${RES}/reservation-api/v1/matches/${MATCH_ID}/availability`, bearer(tokens[0]));
  const ocupados = new Set(a.json('occupiedSeats').map((s) => s.seatId));
  let seat;
  do { seat = SEATS[0] + Math.floor(Math.random() * (SEATS[1] - SEATS[0])); } while (ocupados.has(seat));
  console.log(`Asiento disputado: ${seat}`);
  return { tokens, seat };
}

export function reservarMismoAsiento(data) {
  const token = data.tokens[(__VU - 1) % data.tokens.length];
  const r = http.post(`${RES}/reservation-api/v1/reservations`,
    JSON.stringify({ matchId: MATCH_ID, seatIds: [data.seat] }), bearer(token));
  if (r.status === 201) creadas.add(1);
  else if (r.status === 409) rechazadas.add(1);
  else { inesperadas.add(1); console.warn(`Respuesta inesperada ${r.status}: ${r.body}`); }
  check(r, { '201 o 409': (x) => x.status === 201 || x.status === 409 });
}

export function consultarDisponibilidad(data) {
  const r = http.get(`${RES}/reservation-api/v1/matches/${MATCH_ID}/availability`, bearer(data.tokens[__VU % data.tokens.length]));
  check(r, { 'disponibilidad 200': (x) => x.status === 200 });
}
