import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function NavBar() {
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate("/");
  };

  return (
    <nav className="navbar">
      <div className="navbar__brand">TimeToGame</div>
      <div className="navbar__links">
        <NavLink to="/" end>
          Inicio
        </NavLink>
        <NavLink to="/catalogo">Catalogo</NavLink>
        {isAuthenticated && <NavLink to="/panel">Panel</NavLink>}
        {isAuthenticated && <NavLink to="/admin">Administracion</NavLink>}
      </div>
      <div className="navbar__auth">
        {isAuthenticated ? (
          <>
            <span className="navbar__user">
              {user?.username} ({user?.roles?.join(", ")})
            </span>
            <button type="button" onClick={handleLogout}>
              Cerrar sesion
            </button>
          </>
        ) : (
          <NavLink to="/login">Iniciar sesion</NavLink>
        )}
      </div>
    </nav>
  );
}
