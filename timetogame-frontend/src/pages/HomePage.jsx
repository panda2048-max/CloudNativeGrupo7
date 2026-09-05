import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function HomePage() {
  const { isAuthenticated } = useAuth();

  return (
    <section className="page">
      <h1>Bienvenido a TimeToGame</h1>
      <p>
        Catalogo colaborativo de videojuegos organizado por genero. Explora el
        catalogo publico sin necesidad de iniciar sesion, o accede con una
        cuenta para consultar y administrar la informacion.
      </p>
      <div className="home__actions">
        <Link className="button" to="/catalogo">
          Ver catalogo publico
        </Link>
        {!isAuthenticated && (
          <Link className="button button--secondary" to="/login">
            Iniciar sesion
          </Link>
        )}
      </div>

      <div className="home__hint">
        <p>Usuarios de prueba:</p>
        <ul>
          <li>
            <strong>user / user123</strong> - lectura autenticada (rol USER)
          </li>
          <li>
            <strong>admin / admin123</strong> - lectura y administracion (rol ADMIN)
          </li>
        </ul>
      </div>
    </section>
  );
}
