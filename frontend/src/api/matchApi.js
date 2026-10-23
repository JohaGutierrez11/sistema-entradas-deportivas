import { request } from './http';

/** match-service (8082). Catálogo y layout son públicos: sin token, así nunca dan 401 por un token vencido. */
export const matchApi = {
  searchMatches: ({ date, name } = {}) =>
    request('/match-api/v1/matches', { query: { date, name }, auth: false }),

  getMatch: (matchId) =>
    request(`/match-api/v1/matches/${matchId}`, { auth: false }),

  getStadiumLayout: (stadiumId) =>
    request(`/match-api/v1/stadiums/${stadiumId}/layout`, { auth: false }),

  // ---- Administración (HU-28): con token de ADMIN; el backend responde 403 a cualquier otro rol ----

  listStadiums: () => request('/match-api/v1/stadiums'),

  getStadium: (stadiumId) => request(`/match-api/v1/stadiums/${stadiumId}`),

  createStadium: (stadium) => request('/match-api/v1/stadiums', { method: 'POST', body: stadium }),

  updateStadium: (stadiumId, stadium) =>
    request(`/match-api/v1/stadiums/${stadiumId}`, { method: 'PUT', body: stadium }),

  /** 409 si el estadio tiene secciones o partidos: el mensaje del backend dice cuántos. */
  deleteStadium: (stadiumId) => request(`/match-api/v1/stadiums/${stadiumId}`, { method: 'DELETE' }),

  listSections: (stadiumId) => request(`/match-api/v1/stadiums/${stadiumId}/sections`),

  createSection: (stadiumId, section) =>
    request(`/match-api/v1/stadiums/${stadiumId}/sections`, { method: 'POST', body: section }),

  /** Ubica o mueve la sección en el plano: { x, y, width, height } en % (HU-38). */
  updateSectionBox: (sectionId, box) =>
    request(`/match-api/v1/sections/${sectionId}/box`, { method: 'PUT', body: box }),
};
