import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { useRef } from 'react';

/** El menú muestra solo lo que el rol puede usar (HU-29). */
export default function Navbar() {
  const { session, isAdmin, isClient, logout } = useAuth();
  const navigate = useNavigate();
  const menuRef = useRef(null);
  const closeMenu = () => { if (menuRef.current) menuRef.current.open = false; };

  function handleLogout() {
    logout();
    navigate('/login', { replace: true });
  }

  return (
    <header className="navbar">
      <nav className="navbar-inner" aria-label="Menú principal">
        <NavLink to={isAdmin ? '/admin' : '/'} className="brand">
          <span className="brand-mark" aria-hidden="true">E</span>
          Entradas<span className="brand-light">deportivas</span>
        </NavLink>

        <div className="nav-links">
          {!isAdmin && <NavLink to="/" end>Partidos</NavLink>}
          {isClient && <NavLink to="/reservas">Mis reservas</NavLink>}
          {isAdmin && (
            <>
              <NavLink to="/admin/estadios">Escenarios</NavLink>
              <NavLink to="/admin/partidos">Partidos</NavLink>
              <NavLink to="/admin/usuarios">Usuarios</NavLink>
              <NavLink to="/admin/reportes">Reportes</NavLink>
              <NavLink to="/admin/configuracion">Configuración</NavLink>
            </>
          )}
        </div>

        <div className="nav-user">
          {session ? (
            // <details> es un desplegable nativo del navegador: sin JavaScript y accesible con el teclado
            <details className="user-menu" ref={menuRef}>
              <summary>
                <span className="avatar" aria-hidden="true">{session.username.charAt(0).toUpperCase()}</span>
                <span className="user-name">{session.username}</span>
                <span className="role-badge">{session.role}</span>
              </summary>
              <div className="user-menu-panel" onClick={closeMenu}>
                <NavLink to="/cambiar-contrasena">Cambiar contraseña</NavLink>
                <button type="button" onClick={handleLogout}>Cerrar sesión</button>
              </div>
            </details>
          ) : (
            <>
              <NavLink to="/login">Iniciar sesión</NavLink>
              <NavLink to="/registro" className="btn btn-small btn-solid">Crear cuenta</NavLink>
            </>
          )}
        </div>
      </nav>
    </header>
  );
}