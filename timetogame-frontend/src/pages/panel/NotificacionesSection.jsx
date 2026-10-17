import { useCallback, useEffect, useState } from "react";
import { fetchNotificaciones } from "../../api/notificacionesApi";
import { LoadingIndicator } from "../../components/LoadingIndicator";
import { StatusMessage } from "../../components/StatusMessage";

const ETIQUETA_TIPO = {
  CREADO: "Creado",
  ACTUALIZADO: "Actualizado",
  ELIMINADO: "Eliminado",
};

function formatearFecha(iso) {
  return iso ? new Date(iso).toLocaleString("es-CL") : "";
}

export function NotificacionesSection() {
  const [notificaciones, setNotificaciones] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const cargar = useCallback(() => {
    setLoading(true);
    setError(null);
    return fetchNotificaciones()
      .then(setNotificaciones)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    cargar();
  }, [cargar]);

  return (
    <div className="admin-section">
      <div className="section-header">
        <h2>Notificaciones del catalogo</h2>
        <button type="button" onClick={cargar} disabled={loading}>
          Actualizar
        </button>
      </div>
      <p className="hint">
        Se generan de forma asincrona: cada cambio en el catalogo publica un
        evento en RabbitMQ y un consumidor lo registra aqui unos instantes
        despues.
      </p>

      {loading && <LoadingIndicator label="Cargando notificaciones..." />}
      <StatusMessage variant="error">{error}</StatusMessage>

      {!loading && !error && (
        <table className="table">
          <thead>
            <tr>
              <th>Evento</th>
              <th>Detalle</th>
              <th>Procesado</th>
            </tr>
          </thead>
          <tbody>
            {notificaciones.map((notificacion) => (
              <tr key={notificacion.id}>
                <td>
                  <span className={`tag tag--${notificacion.tipoEvento.toLowerCase()}`}>
                    {ETIQUETA_TIPO[notificacion.tipoEvento] ?? notificacion.tipoEvento}
                  </span>
                </td>
                <td>{notificacion.mensaje}</td>
                <td>{formatearFecha(notificacion.procesadoEn)}</td>
              </tr>
            ))}
            {notificaciones.length === 0 && (
              <tr>
                <td colSpan={3}>Todavia no hay notificaciones.</td>
              </tr>
            )}
          </tbody>
        </table>
      )}
    </div>
  );
}
