import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { fetchPublicVideojuegos } from "../api/videojuegosApi";
import { LoadingIndicator } from "../components/LoadingIndicator";
import { StatusMessage } from "../components/StatusMessage";

export function CatalogoPage() {
  const [videojuegos, setVideojuegos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let active = true;

    fetchPublicVideojuegos()
      .then((data) => {
        if (active) setVideojuegos(data);
      })
      .catch((err) => {
        if (active) setError(err.message);
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  return (
    <section className="page">
      <h1>Catalogo publico</h1>
      <p>Informacion obtenida en vivo desde el backend (sin necesidad de iniciar sesion).</p>

      {loading && <LoadingIndicator label="Cargando catalogo..." />}
      <StatusMessage variant="error">{error}</StatusMessage>

      {!loading && !error && videojuegos.length === 0 && (
        <StatusMessage variant="info">Todavia no hay videojuegos cargados.</StatusMessage>
      )}

      <ul className="card-list">
        {videojuegos.map((videojuego) => (
          <li key={videojuego.id} className="card">
            <h2>{videojuego.titulo}</h2>
            <p className="card__meta">{videojuego.generoNombre}</p>
            <p>{videojuego.descripcion}</p>
            <p className="card__price">USD {videojuego.precio}</p>
            <Link to={`/catalogo/${videojuego.id}`}>Ver detalle</Link>
          </li>
        ))}
      </ul>
    </section>
  );
}
