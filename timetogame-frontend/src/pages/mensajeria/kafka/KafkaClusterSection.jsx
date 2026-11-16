import { useEffect, useState } from "react";
import { fetchKafkaCluster, simularVistas } from "../../../api/streamingApi";
import { StatusMessage, messageForError, variantForStatus } from "../../../components/StatusMessage";

/** Brokers del cluster y generador de carga para la demo de lag. */
export function KafkaClusterSection() {
  const [cluster, setCluster] = useState(null);
  const [error, setError] = useState(null);
  const [vistas, setVistas] = useState(500);
  const [simulando, setSimulando] = useState(false);
  const [feedback, setFeedback] = useState(null);

  useEffect(() => {
    fetchKafkaCluster()
      .then(setCluster)
      .catch((err) => setError(err.message));
  }, []);

  const handleSimular = async (event) => {
    event.preventDefault();
    setSimulando(true);
    setFeedback(null);
    try {
      const resultado = await simularVistas(vistas);
      const resumen = Object.entries(resultado.porVideojuego)
        .map(([titulo, n]) => `${titulo}: ${n}`)
        .join(", ");
      setFeedback({
        variant: "success",
        text: `${resultado.vistasGeneradas} vistas encoladas en timetogame.actividad (${resumen}). Mira el lag abajo.`,
      });
    } catch (err) {
      setFeedback({ variant: variantForStatus(err.status), text: messageForError(err) });
    } finally {
      setSimulando(false);
    }
  };

  return (
    <div className="admin-section">
      <h2>Cluster Kafka</h2>
      <StatusMessage variant="error">{error}</StatusMessage>
      {cluster && (
        <p>
          {cluster.brokers.length} broker(s):{" "}
          {cluster.brokers.map((b) => (
            <span key={b.id} className="tag" title={`${b.host}:${b.puerto}`}>
              broker {b.id}
              {b.controlador ? " (controlador)" : ""}
            </span>
          ))}
        </p>
      )}

      <form className="form form--inline" onSubmit={handleSimular}>
        <label>
          Vistas a simular
          <input
            type="number"
            min="1"
            max="5000"
            value={vistas}
            onChange={(event) => setVistas(event.target.value)}
            required
          />
        </label>
        <button type="submit" disabled={simulando}>
          Simular carga
        </button>
      </form>
      <p className="hint">
        Publica vistas sinteticas en timetogame.actividad. El ranking de la portada se actualiza en vivo
        y el grupo &quot;tendencias&quot; muestra el lag mientras las procesa.
      </p>
      <StatusMessage variant={feedback?.variant} onClose={() => setFeedback(null)}>
        {feedback?.text}
      </StatusMessage>
    </div>
  );
}
