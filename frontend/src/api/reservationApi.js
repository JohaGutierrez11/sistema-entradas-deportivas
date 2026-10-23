import { request } from './http';

const BASE = '/reservation-api/v1';

/** reservation-service (8083). */
export const reservationApi = {
  /** Pública: solo los asientos ocupados del partido y cada cuántos segundos refrescar. */
  getAvailability: (matchId) =>
    request(`${BASE}/matches/${matchId}/availability`, { auth: false }),

  /** Crea la reserva PENDIENTE: retiene los asientos unos minutos (HU-16). */
  createReservation: (matchId, seatIds) =>
    request(`${BASE}/reservations`, { method: 'POST', body: { matchId, seatIds } }),

  getReservation: (publicId) => request(`${BASE}/reservations/${publicId}`),

  /** Confirmar = pagar (simulado): emite un ticket con QR por asiento (HU-18). */
  confirmReservation: (publicId) =>
    request(`${BASE}/reservations/${publicId}/confirm`, { method: 'POST' }),

  cancelReservation: (publicId) =>
    request(`${BASE}/reservations/${publicId}/cancel`, { method: 'POST' }),

  /** Imagen PNG del QR de un ticket (Blob). */
  getTicketQr: (ticketPublicId) => request(`${BASE}/tickets/${ticketPublicId}/qr`),

  /** Historial del cliente del token: [{ reservation, match }] (HU-20). */
  getMyReservations: () => request(`${BASE}/reservations/me`),

  /** El mismo historial en PDF (Blob). */
  getMyReservationsPdf: () => request(`${BASE}/reservations/me/pdf`),

  /** PDF con una página por entrada (Blob). */
  getTicketsPdf: (publicId) => request(`${BASE}/reservations/${publicId}/tickets/pdf`),
};
