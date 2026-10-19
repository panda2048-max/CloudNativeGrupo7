import { useCallback, useEffect, useState } from "react";
import { fetchMensajesFallidos } from "../../api/notificacionesApi";
import { LoadingIndicator } from "../../components/LoadingIndicator";
import { StatusMessage } from "../../components/StatusMessage";

export function MensajesFallidosSection() {
  const [mensajes, setMensajes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const cargar = useCallback(() => {
    setLoading(true);
    setError(null);
    return fetchMensajesFallidos()
      .then(setMensajes)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    cargar();
  }, [cargar]);

  return (
    <div className="admin-section">
      <div className="section-header">
        <h2>Mensajes fallidos (DLQ del backend)</h2>
        <button type="button" onClick={cargar} disabled={loading}>
          Actualizar
        </button>
      </div>
      <p className="hint">
        Mensajes que los consumidores de notificaciones y de fichas no pudieron
        procesar: rechazados por invalidos, o que agotaron sus reintentos. Quedan
        registrados en el log del backend y aqui.
      </p>

      {loading && <LoadingIndicator label="Cargando mensajes fallidos..." />}
      <StatusMessage variant="error">{error}</StatusMessage>

      {!loading && !error && (
        <table className="table">
          <thead>
            <tr>
              <th>Registrado</th>
              <th>Cola origen</th>
              <th>Routing key</th>
              <th>Motivo</th>
              <th>Reintentos</th>
              <th>Ultimo error / payload</th>
            </tr>
          </thead>
          <tbody>
            {mensajes.map((mensaje) => (
              <tr key={mensaje.id}>
                <td>{new Date(mensaje.registradoEn).toLocaleString("es-CL")}</td>
                <td>
                  <code>{mensaje.colaOrigen ?? "-"}</code>
                </td>
                <td>
                  <code>{mensaje.routingKey}</code>
                </td>
                <td>{mensaje.motivo}</td>
                <td>{mensaje.reintentos}</td>
                <td className="cell-wrap">
                  {mensaje.ultimoError && <div>{mensaje.ultimoError}</div>}
                  <code>{mensaje.payload}</code>
                </td>
              </tr>
            ))}
            {mensajes.length === 0 && (
              <tr>
                <td colSpan={6}>No hay mensajes fallidos.</td>
              </tr>
            )}
          </tbody>
        </table>
      )}
    </div>
  );
}
