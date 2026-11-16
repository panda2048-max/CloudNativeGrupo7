import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { crearResena, fetchResenas } from "../../api/resenasApi";
import { useAuth } from "../../auth/AuthContext";
import { Estrellas } from "../../components/Estrellas";
import { LoadingIndicator } from "../../components/LoadingIndicator";
import { StatusMessage, messageForError, variantForStatus } from "../../components/StatusMessage";

const MAX_COMENTARIO = 500;
const formVacio = { calificacion: 5, comentario: "" };

/**
 * Resenas de un videojuego. Al publicar, la resena se guarda y viaja por
 * Kafka (topico de actividad) hasta la portada, donde aparece en vivo.
 */
export function ResenasSection({ videojuegoId }) {
  const { isAuthenticated } = useAuth();
  const [resumen, setResumen] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [form, setForm] = useState(formVacio);
  const [enviando, setEnviando] = useState(false);
  const [feedback, setFeedback] = useState(null);

  const cargar = useCallback(() => {
    setError(null);
    return fetchResenas(videojuegoId)
      .then(setResumen)
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [videojuegoId]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setEnviando(true);
    setFeedback(null);
    try {
      await crearResena(videojuegoId, { calificacion: Number(form.calificacion), comentario: form.comentario });
      setFeedback({ variant: "success", text: "Resena publicada. Ya aparece en la portada." });
      setForm(formVacio);
      await cargar();
    } catch (err) {
      setFeedback({ variant: variantForStatus(err.status), text: messageForError(err) });
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div className="admin-section">
      <div className="section-header">
        <h2>Resenas</h2>
        {resumen?.promedio != null && (
          <span>
            <Estrellas valor={resumen.promedio} /> {resumen.promedio} ({resumen.total})
          </span>
        )}
      </div>

      {loading && <LoadingIndicator label="Cargando resenas..." />}
      <StatusMessage variant="error">{error}</StatusMessage>

      {isAuthenticated ? (
        <form className="form" onSubmit={handleSubmit}>
          <label>
            Calificacion
            <select
              value={form.calificacion}
              onChange={(e) => setForm({ ...form, calificacion: e.target.value })}
            >
              {[5, 4, 3, 2, 1].map((n) => (
                <option key={n} value={n}>
                  {"★".repeat(n)} ({n})
                </option>
              ))}
            </select>
          </label>
          <label>
            Comentario
            <textarea
              value={form.comentario}
              maxLength={MAX_COMENTARIO}
              onChange={(e) => setForm({ ...form, comentario: e.target.value })}
              placeholder="Que te parecio el juego?"
              required
            />
          </label>
          <p className="hint">
            {form.comentario.length}/{MAX_COMENTARIO}
          </p>
          <div className="form__actions">
            <button type="submit" disabled={enviando}>
              Publicar resena
            </button>
          </div>
        </form>
      ) : (
        <p className="hint">
          <Link to="/login">Inicia sesion</Link> para dejar tu resena.
        </p>
      )}

      <StatusMessage variant={feedback?.variant} onClose={() => setFeedback(null)}>
        {feedback?.text}
      </StatusMessage>

      {resumen && (
        <ul className="resenas">
          {resumen.resenas.map((r) => (
            <li key={r.id}>
              <div>
                <Estrellas valor={r.calificacion} /> <strong>{r.usuario}</strong>{" "}
                <span className="hint">{new Date(r.creadaEn).toLocaleString("es-CL")}</span>
              </div>
              <p>{r.comentario}</p>
            </li>
          ))}
          {resumen.resenas.length === 0 && <li className="hint">Aun no hay resenas. Se el primero.</li>}
        </ul>
      )}
    </div>
  );
}
