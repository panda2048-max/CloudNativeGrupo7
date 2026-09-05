import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { fetchPublicVideojuego } from "../api/videojuegosApi";
import { LoadingIndicator } from "../components/LoadingIndicator";
import { StatusMessage } from "../components/StatusMessage";

export function VideojuegoDetallePage() {
  const { id } = useParams();
  const [videojuego, setVideojuego] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);

    fetchPublicVideojuego(id)
      .then((data) => {
        if (active) setVideojuego(data);
      })
      .catch((err) => {
        if (active) setError(err);
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [id]);

  return (
    <section className="page">
      <Link to="/catalogo">&larr; Volver al catalogo</Link>

      {loading && <LoadingIndicator label="Cargando videojuego..." />}

      {error && (
        <StatusMessage variant={error.status === 404 ? "warning" : "error"}>
          {error.status === 404 ? "Ese videojuego no existe." : error.message}
        </StatusMessage>
      )}

      {videojuego && (
        <article className="detail">
          <h1>{videojuego.titulo}</h1>
          <p className="card__meta">{videojuego.generoNombre}</p>
          <p>{videojuego.descripcion}</p>
          <p className="card__price">USD {videojuego.precio}</p>
          <p>Lanzamiento: {videojuego.fechaLanzamiento ?? "sin definir"}</p>
        </article>
      )}
    </section>
  );
}
