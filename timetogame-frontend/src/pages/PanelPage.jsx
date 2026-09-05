import { useEffect, useState } from "react";
import { fetchGeneros } from "../api/generosApi";
import { fetchVideojuego, fetchVideojuegos } from "../api/videojuegosApi";
import { useAuth } from "../auth/AuthContext";
import { LoadingIndicator } from "../components/LoadingIndicator";
import { StatusMessage } from "../components/StatusMessage";

export function PanelPage() {
  const { user } = useAuth();
  const [videojuegos, setVideojuegos] = useState([]);
  const [generos, setGeneros] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [searchId, setSearchId] = useState("");
  const [searchResult, setSearchResult] = useState(null);
  const [searchError, setSearchError] = useState(null);
  const [searching, setSearching] = useState(false);

  useEffect(() => {
    let active = true;

    Promise.all([fetchVideojuegos(), fetchGeneros()])
      .then(([videojuegosData, generosData]) => {
        if (!active) return;
        setVideojuegos(videojuegosData);
        setGeneros(generosData);
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

  const handleSearch = async (event) => {
    event.preventDefault();
    setSearching(true);
    setSearchError(null);
    setSearchResult(null);
    try {
      const data = await fetchVideojuego(searchId);
      setSearchResult(data);
    } catch (err) {
      setSearchError(err);
    } finally {
      setSearching(false);
    }
  };

  return (
    <section className="page">
      <h1>Panel autenticado</h1>
      <p>
        Hola <strong>{user?.username}</strong>. Esta vista requiere sesion
        iniciada y consulta los endpoints protegidos del backend
        (<code>GET /api/videojuegos</code> y <code>GET /api/generos</code>).
      </p>

      {loading && <LoadingIndicator label="Cargando datos protegidos..." />}
      <StatusMessage variant="error">{error}</StatusMessage>

      {!loading && !error && (
        <div className="panel-grid">
          <div>
            <h2>Generos ({generos.length})</h2>
            <ul>
              {generos.map((genero) => (
                <li key={genero.id}>{genero.nombre}</li>
              ))}
            </ul>
          </div>

          <div>
            <h2>Videojuegos ({videojuegos.length})</h2>
            <ul>
              {videojuegos.map((videojuego) => (
                <li key={videojuego.id}>
                  {videojuego.titulo} - {videojuego.generoNombre}
                </li>
              ))}
            </ul>
          </div>
        </div>
      )}

      <div className="panel-search">
        <h2>Buscar videojuego por id</h2>
        <form className="form form--inline" onSubmit={handleSearch}>
          <input
            type="number"
            min="1"
            placeholder="Id del videojuego"
            value={searchId}
            onChange={(event) => setSearchId(event.target.value)}
            required
          />
          <button type="submit" disabled={searching}>
            {searching ? "Buscando..." : "Buscar"}
          </button>
        </form>

        {searchError && (
          <StatusMessage variant={searchError.status === 404 ? "warning" : "error"}>
            {searchError.status === 404
              ? `No existe un videojuego con id ${searchId}.`
              : searchError.message}
          </StatusMessage>
        )}

        {searchResult && (
          <StatusMessage variant="success">
            Encontrado: {searchResult.titulo} ({searchResult.generoNombre})
          </StatusMessage>
        )}
      </div>
    </section>
  );
}
