import { request } from './http';

/** auth-service (8081). El login y el registro NO envían token. */
export const authApi = {
  login: (username, password) =>
    request('/auth-api/v1/auth/login', { method: 'POST', body: { username, password }, auth: false }),

  register: (data) =>
    request('/auth-api/v1/auth/register', { method: 'POST', body: data, auth: false }),

  changePassword: (currentPassword, newPassword) =>
    request('/auth-api/v1/auth/change-password', { method: 'POST', body: { currentPassword, newPassword } }),
};