import { request } from './http';

/** match-service (8082). Catálogo y layout son públicos: sin token, así nunca dan 401 por un token vencido. */
export const matchApi = {
  searchMatches: ({ date, name } = {}) =>
    request('/match-api/v1/matches', { query: { date, name }, auth: false }),

  getMatch: (matchId) =>
    request(`/match-api/v1/matches/${matchId}`, { auth: false }),

  getStadiumLayout: (stadiumId) =>
    request(`/match-api/v1/stadiums/${stadiumId}/layout`, { auth: false }),
};