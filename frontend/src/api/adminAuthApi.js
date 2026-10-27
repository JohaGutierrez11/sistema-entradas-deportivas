import { request } from './http';

/** auth-service (8081), solo ADMIN: usuarios y roles (HU-28). */
export const adminAuthApi = {
  listUsers: () => request('/auth-api/v1/users'),

  /** ACTIVO o INACTIVO. BLOQUEADO lo pone el sistema; activar también desbloquea. */
  updateUserStatus: (userId, status) =>
    request(`/auth-api/v1/users/${userId}/status`, { method: 'PATCH', body: { status } }),

  listRoles: () => request('/auth-api/v1/roles'),

  /** Nombre, usuario, correo y últimos 4 de la cédula de varios clientes en UNA petición (HU-44). */
  getClients: (ids) => request('/auth-api/v1/clients', { query: { ids: ids.join(',') } }),

  updateRoleStatus: (roleId, status) =>
    request(`/auth-api/v1/roles/${roleId}/status`, { method: 'PATCH', body: { status } }),
};
