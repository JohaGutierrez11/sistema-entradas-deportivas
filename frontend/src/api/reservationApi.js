import { request } from './http';

/** reservation-service (8083). */
export const reservationApi = {
  /** Pública: solo los asientos ocupados del partido y cada cuántos segundos refrescar. */
  getAvailability: (matchId) =>
    request(`/reservation-api/v1/matches/${matchId}/availability`, { auth: false }),
};