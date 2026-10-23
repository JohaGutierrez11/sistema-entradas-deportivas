import { useCallback, useEffect, useMemo, useState } from 'react';
import { adminAuthApi } from '../../api/adminAuthApi';
import { useAuth } from '../../auth/AuthContext';
import StatusBadge from '../../components/StatusBadge';
import { formatDateTime, parseEcuadorDateTime } from '../../utils/format';

const TABS = [
  { id: 'users', label: 'Usuarios' },
  { id: 'roles', label: 'Roles' },
];

/** Usuarios y roles (HU-28): activar, desactivar y desbloquear. Las reglas las aplica auth-service. */
export default function UsersAdminPage() {
  const [tab, setTab] = useState('users');
  return (
    <section>
      <div className="page-header">
        <h1>Usuarios y roles</h1>
        <p className="muted">Un usuario o rol inactivo no puede iniciar sesión.</p>
      </div>
      <div className="segmented" role="tablist" aria-label="Usuarios o roles">
        {TABS.map((t) => (
          <button key={t.id} type="button" role="tab" aria-selected={tab === t.id}
                  className={tab === t.id ? 'is-active' : ''} onClick={() => setTab(t.id)}>
            {t.label}
          </button>
        ))}
      </div>
      {tab === 'users' ? <UsersTab /> : <RolesTab />}
    </section>
  );
}

/** Un BLOQUEADO cuyo lockedUntil ya pasó se libera en el próximo login: se muestra así. */
function lockInfo(user) {
  if (user.status !== 'BLOQUEADO' || !user.lockedUntil) return null;
  const until = parseEcuadorDateTime(user.lockedUntil);
  return until.getTime() > Date.now()
    ? `Hasta ${formatDateTime(user.lockedUntil)}`
    : 'Se libera en su próximo inicio de sesión';
}

function UsersTab() {
  const { session } = useAuth();
  const [users, setUsers] = useState(null);
  const [query, setQuery] = useState('');
  const [roleFilter, setRoleFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);
  const [confirmingId, setConfirmingId] = useState(null);
  const [busyId, setBusyId] = useState(null);

  const load = useCallback(() => {
    adminAuthApi.listUsers().then(setUsers).catch((err) => setError(err.message));
  }, []);
  useEffect(load, [load]);

  // Filtros en el navegador: la lista de usuarios de este sistema es corta
  const visible = useMemo(() => (users ?? []).filter((u) =>
    u.username.toLowerCase().includes(query.trim().toLowerCase())
    && (!roleFilter || u.role === roleFilter)
    && (!statusFilter || u.status === statusFilter)), [users, query, roleFilter, statusFilter]);

  async function changeStatus(user, status) {
    setError(null);
    setNotice(null);
    setBusyId(user.id);
    try {
      const saved = await adminAuthApi.updateUserStatus(user.id, status);
      const verb = status === 'INACTIVO' ? 'se desactivó' : user.status === 'BLOQUEADO' ? 'se desbloqueó' : 'se activó';
      setNotice(`La cuenta ${saved.username} ${verb}.`);
      setUsers((current) => current.map((u) => (u.id === saved.id ? saved : u)));
    } catch (err) {
      setError(err.message);   // 422: desactivarse a sí mismo o asignar BLOQUEADO
    } finally {
      setBusyId(null);
      setConfirmingId(null);
    }
  }

  return (
    <>
      {notice && <p className="alert alert-success" role="status">{notice}</p>}
      {error && <p className="alert alert-error" role="alert">{error}</p>}

      <div className="admin-toolbar">
        <input type="search" placeholder="Buscar usuario" value={query} aria-label="Buscar usuario"
               onChange={(e) => setQuery(e.target.value)} />
        <div className="toolbar-group">
          <select value={roleFilter} onChange={(e) => setRoleFilter(e.target.value)} aria-label="Rol">
            <option value="">Todos los roles</option>
            <option value="ADMIN">ADMIN</option>
            <option value="CLIENTE">CLIENTE</option>
          </select>
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} aria-label="Estado">
            <option value="">Todos los estados</option>
            <option value="ACTIVO">Activos</option>
            <option value="INACTIVO">Inactivos</option>
            <option value="BLOQUEADO">Bloqueados</option>
          </select>
        </div>
      </div>

      {users === null && !error && <p className="muted">Cargando usuarios…</p>}
      {users && visible.length === 0 && <p className="muted">Ningún usuario coincide con los filtros.</p>}

      {visible.length > 0 && (
        <div className="card table-card">
          <table className="data-table">
            <thead>
              <tr>
                <th scope="col">Usuario</th>
                <th scope="col">Rol</th>
                <th scope="col">Estado</th>
                <th scope="col">Último ingreso</th>
                <th scope="col"><span className="sr-only">Acciones</span></th>
              </tr>
            </thead>
            <tbody>
              {visible.map((user) => {
                const isMe = user.username.toLowerCase() === session?.username?.toLowerCase();
                const lock = lockInfo(user);
                return (
                  <tr key={user.id}>
                    <td>
                      <strong>{user.username}</strong>{isMe && <span className="muted small"> (tú)</span>}
                      {user.mustChangePassword && <div className="muted small">Debe cambiar su contraseña</div>}
                    </td>
                    <td>{user.role}</td>
                    <td>
                      <StatusBadge status={user.status} />
                      {lock && <div className="muted small">{lock}</div>}
                      {user.status === 'ACTIVO' && user.failedLoginAttempts > 0 && (
                        <div className="muted small">{user.failedLoginAttempts} intento(s) fallido(s)</div>
                      )}
                    </td>
                    <td className="nowrap">{user.lastLoginAt ? formatDateTime(user.lastLoginAt) : <span className="muted">Nunca</span>}</td>
                    <td>
                      <UserActions user={user} isMe={isMe} busy={busyId === user.id}
                                   confirming={confirmingId === user.id}
                                   onAsk={() => { setNotice(null); setError(null); setConfirmingId(user.id); }}
                                   onCancelAsk={() => setConfirmingId(null)}
                                   onChange={(status) => changeStatus(user, status)} />
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}

/** Desactivar pide confirmación; activar y desbloquear no (se pueden revertir sin daño). */
function UserActions({ user, isMe, busy, confirming, onAsk, onCancelAsk, onChange }) {
  if (confirming) {
    return (
      <div className="row-actions">
        <span className="small">¿Desactivar? No podrá iniciar sesión.</span>
        <button type="button" className="btn btn-small btn-danger" disabled={busy} onClick={() => onChange('INACTIVO')}>Sí</button>
        <button type="button" className="btn btn-small" disabled={busy} onClick={onCancelAsk}>No</button>
      </div>
    );
  }
  return (
    <div className="row-actions">
      {user.status === 'BLOQUEADO' && (
        <button type="button" className="btn btn-small" disabled={busy} onClick={() => onChange('ACTIVO')}>Desbloquear</button>
      )}
      {user.status === 'INACTIVO' && (
        <button type="button" className="btn btn-small" disabled={busy} onClick={() => onChange('ACTIVO')}>Activar</button>
      )}
      {user.status !== 'INACTIVO' && (
        <button type="button" className="btn btn-small btn-danger-outline" disabled={busy || isMe} onClick={onAsk}
                title={isMe ? 'No puedes desactivar tu propia cuenta' : undefined}>
          Desactivar
        </button>
      )}
    </div>
  );
}

function RolesTab() {
  const [roles, setRoles] = useState(null);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);
  const [confirmingId, setConfirmingId] = useState(null);
  const [busyId, setBusyId] = useState(null);

  useEffect(() => {
    adminAuthApi.listRoles().then(setRoles).catch((err) => setError(err.message));
  }, []);

  async function changeStatus(role, status) {
    setError(null);
    setNotice(null);
    setBusyId(role.id);
    try {
      const saved = await adminAuthApi.updateRoleStatus(role.id, status);
      setNotice(`El rol ${saved.name} ${status === 'ACTIVO' ? 'se activó' : 'se desactivó'}.`);
      setRoles((current) => current.map((r) => (r.id === saved.id ? saved : r)));
    } catch (err) {
      setError(err.message);   // 422: no se puede desactivar ADMIN
    } finally {
      setBusyId(null);
      setConfirmingId(null);
    }
  }

  return (
    <>
      {notice && <p className="alert alert-success" role="status">{notice}</p>}
      {error && <p className="alert alert-error" role="alert">{error}</p>}
      {roles === null && !error && <p className="muted">Cargando roles…</p>}
      {roles?.length > 0 && (
        <div className="card table-card">
          <table className="data-table">
            <thead>
              <tr>
                <th scope="col">Rol</th>
                <th scope="col">Descripción</th>
                <th scope="col">Estado</th>
                <th scope="col"><span className="sr-only">Acciones</span></th>
              </tr>
            </thead>
            <tbody>
              {roles.map((role) => (
                <tr key={role.id}>
                  <td><strong>{role.name}</strong></td>
                  <td>{role.description}</td>
                  <td><StatusBadge status={role.status} /></td>
                  <td>
                    <div className="row-actions">
                      {confirmingId === role.id ? (
                        <>
                          <span className="small">¿Desactivar? Nadie con este rol podrá iniciar sesión.</span>
                          <button type="button" className="btn btn-small btn-danger" disabled={busyId === role.id}
                                  onClick={() => changeStatus(role, 'INACTIVO')}>Sí</button>
                          <button type="button" className="btn btn-small" onClick={() => setConfirmingId(null)}>No</button>
                        </>
                      ) : role.status === 'ACTIVO' ? (
                        <button type="button" className="btn btn-small btn-danger-outline"
                                disabled={role.name === 'ADMIN'}
                                title={role.name === 'ADMIN' ? 'El sistema siempre necesita un administrador' : undefined}
                                onClick={() => { setNotice(null); setError(null); setConfirmingId(role.id); }}>
                          Desactivar
                        </button>
                      ) : (
                        <button type="button" className="btn btn-small" disabled={busyId === role.id}
                                onClick={() => changeStatus(role, 'ACTIVO')}>Activar</button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
