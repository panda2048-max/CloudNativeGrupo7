import { useEffect, useState } from "react";
import {
  createVideojuego,
  deleteVideojuego,
  fetchVideojuegos,
  updateVideojuego,
} from "../../api/videojuegosApi";
import { StatusMessage, variantForStatus } from "../../components/StatusMessage";
import { LoadingIndicator } from "../../components/LoadingIndicator";

const emptyForm = {
  titulo: "",
  descripcion: "",
  precio: "",
  fechaLanzamiento: "",
  generoId: "",
};

export function AdminVideojuegosSection({ generos }) {
  const [videojuegos, setVideojuegos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);

  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [feedback, setFeedback] = useState(null);

  const loadVideojuegos = () => {
    setLoading(true);
    setLoadError(null);
    return fetchVideojuegos()
      .then(setVideojuegos)
      .catch((err) => setLoadError(err.message))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadVideojuegos();
  }, []);

  const startEdit = (videojuego) => {
    setEditingId(videojuego.id);
    setForm({
      titulo: videojuego.titulo,
      descripcion: videojuego.descripcion ?? "",
      precio: videojuego.precio,
      fechaLanzamiento: videojuego.fechaLanzamiento ?? "",
      generoId: videojuego.generoId,
    });
    setFeedback(null);
  };

  const cancelEdit = () => {
    setEditingId(null);
    setForm(emptyForm);
  };

  const updateField = (field) => (event) =>
    setForm((prev) => ({ ...prev, [field]: event.target.value }));

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setFeedback(null);

    const payload = {
      titulo: form.titulo,
      descripcion: form.descripcion || null,
      precio: Number(form.precio),
      fechaLanzamiento: form.fechaLanzamiento || null,
      generoId: Number(form.generoId),
    };

    try {
      if (editingId) {
        await updateVideojuego(editingId, payload);
        setFeedback({ variant: "success", text: "Videojuego actualizado." });
      } else {
        await createVideojuego(payload);
        setFeedback({ variant: "success", text: "Videojuego creado." });
      }
      setForm(emptyForm);
      setEditingId(null);
      await loadVideojuegos();
    } catch (err) {
      setFeedback({ variant: variantForStatus(err.status), text: err.message });
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (videojuego) => {
    setFeedback(null);
    try {
      await deleteVideojuego(videojuego.id);
      setFeedback({ variant: "success", text: `"${videojuego.titulo}" eliminado.` });
      await loadVideojuegos();
    } catch (err) {
      setFeedback({ variant: variantForStatus(err.status), text: err.message });
    }
  };

  return (
    <div className="admin-section">
      <h2>Videojuegos</h2>

      {loading && <LoadingIndicator label="Cargando videojuegos..." />}
      <StatusMessage variant="error">{loadError}</StatusMessage>

      {!loading && !loadError && (
        <table className="table">
          <thead>
            <tr>
              <th>Titulo</th>
              <th>Genero</th>
              <th>Precio</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {videojuegos.map((videojuego) => (
              <tr key={videojuego.id}>
                <td>{videojuego.titulo}</td>
                <td>{videojuego.generoNombre}</td>
                <td>USD {videojuego.precio}</td>
                <td className="table__actions">
                  <button type="button" onClick={() => startEdit(videojuego)}>
                    Editar
                  </button>
                  <button type="button" className="button--danger" onClick={() => handleDelete(videojuego)}>
                    Eliminar
                  </button>
                </td>
              </tr>
            ))}
            {videojuegos.length === 0 && (
              <tr>
                <td colSpan={4}>Sin videojuegos registrados.</td>
              </tr>
            )}
          </tbody>
        </table>
      )}

      <form className="form" onSubmit={handleSubmit}>
        <label>
          Titulo
          <input value={form.titulo} onChange={updateField("titulo")} required />
        </label>

        <label>
          Descripcion
          <textarea value={form.descripcion} onChange={updateField("descripcion")} />
        </label>

        <label>
          Precio (USD)
          <input
            type="number"
            step="0.01"
            min="0"
            value={form.precio}
            onChange={updateField("precio")}
            required
          />
        </label>

        <label>
          Fecha de lanzamiento
          <input type="date" value={form.fechaLanzamiento} onChange={updateField("fechaLanzamiento")} />
        </label>

        <label>
          Genero
          <select value={form.generoId} onChange={updateField("generoId")} required>
            <option value="" disabled>
              Selecciona un genero
            </option>
            {generos.map((genero) => (
              <option key={genero.id} value={genero.id}>
                {genero.nombre}
              </option>
            ))}
          </select>
        </label>

        <div className="form__actions">
          <button type="submit" disabled={submitting}>
            {editingId ? "Guardar cambios" : "Crear videojuego"}
          </button>
          {editingId && (
            <button type="button" onClick={cancelEdit}>
              Cancelar
            </button>
          )}
        </div>
      </form>

      <StatusMessage variant={feedback?.variant} onClose={() => setFeedback(null)}>
        {feedback?.text}
      </StatusMessage>
    </div>
  );
}
