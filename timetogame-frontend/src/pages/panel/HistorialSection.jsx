import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { fetchHistorialReciente } from "../../api/streamingApi";
import { LoadingIndicator } from "../../components/LoadingIndicator";
import { StatusMessage } from "../../components/StatusMessage";

export function HistorialSection() {
  const [historial, setHistorial] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const cargar = useCallback(() => {
    setLoading(true);
    setError(null);
    return fetchHistorialReciente()
      .then(setHistorial)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    cargar();
  }, [cargar]);

  return (
    <div className="admin-section">
      <div className="section-header">
        <h2>Vistos recientemente</h2>
        <button type="button" onClick={cargar} disabled={loading}>
          Actualizar
        </button>
      </div>
      <p className="hint">
        Lo escribe el consumer group &quot;historial&quot; de Kafka a partir de cada ficha que
        abres en el catalogo.
      </p>

      {loading && <LoadingIndicator label="Cargando historial..." />}
      <StatusMessage variant="error">{error}</StatusMessage>

      {!loading && !error && (
        <ul>
          {historial.map((h) => (
            <li key={`${h.videojuegoId}-${h.vistoEn}`}>
              <Link to={`/catalogo/${h.videojuegoId}`}>{h.titulo}</Link>{" "}
              <span className="hint">{new Date(h.vistoEn).toLocaleString("es-CL")}</span>
            </li>
          ))}
          {historial.length === 0 && <li className="hint">Aun no has abierto ninguna ficha.</li>}
        </ul>
      )}
    </div>
  );
}
