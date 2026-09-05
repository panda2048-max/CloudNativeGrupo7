import { useEffect, useState } from "react";
import { createGenero, deleteGenero, fetchGeneros, updateGenero } from "../../api/generosApi";
import { StatusMessage, variantForStatus } from "../../components/StatusMessage";
import { LoadingIndicator } from "../../components/LoadingIndicator";

const emptyForm = { nombre: "" };

export function AdminGenerosSection({ onGenerosChange }) {
  const [generos, setGeneros] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);

  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [feedback, setFeedback] = useState(null);

  const loadGeneros = () => {
    setLoading(true);
    setLoadError(null);
    return fetchGeneros()
      .then((data) => {
        setGeneros(data);
        onGenerosChange?.(data);
      })
      .catch((err) => setLoadError(err.message))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadGeneros();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const startEdit = (genero) => {
    setEditingId(genero.id);
    setForm({ nombre: genero.nombre });
    setFeedback(null);
  };

  const cancelEdit = () => {
    setEditingId(null);
    setForm(emptyForm);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setFeedback(null);
    try {
      if (editingId) {
        await updateGenero(editingId, form);
        setFeedback({ variant: "success", text: "Genero actualizado." });
      } else {
        await createGenero(form);
        setFeedback({ variant: "success", text: "Genero creado." });
      }
      setForm(emptyForm);
      setEditingId(null);
      await loadGeneros();
    } catch (err) {
      setFeedback({ variant: variantForStatus(err.status), text: err.message });
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (genero) => {
    setFeedback(null);
    try {
      await deleteGenero(genero.id);
      setFeedback({ variant: "success", text: `Genero "${genero.nombre}" eliminado.` });
      await loadGeneros();
    } catch (err) {
      setFeedback({ variant: variantForStatus(err.status), text: err.message });
    }
  };

  return (
    <div className="admin-section">
      <h2>Generos</h2>

      {loading && <LoadingIndicator label="Cargando generos..." />}
      <StatusMessage variant="error">{loadError}</StatusMessage>

      {!loading && !loadError && (
        <table className="table">
          <thead>
            <tr>
              <th>Nombre</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {generos.map((genero) => (
              <tr key={genero.id}>
                <td>{genero.nombre}</td>
                <td className="table__actions">
                  <button type="button" onClick={() => startEdit(genero)}>
                    Editar
                  </button>
                  <button type="button" className="button--danger" onClick={() => handleDelete(genero)}>
                    Eliminar
                  </button>
                </td>
              </tr>
            ))}
            {generos.length === 0 && (
              <tr>
                <td colSpan={2}>Sin generos registrados.</td>
              </tr>
            )}
          </tbody>
        </table>
      )}

      <form className="form form--inline" onSubmit={handleSubmit}>
        <input
          placeholder="Nombre del genero"
          value={form.nombre}
          onChange={(event) => setForm({ nombre: event.target.value })}
          required
        />
        <button type="submit" disabled={submitting}>
          {editingId ? "Guardar cambios" : "Crear genero"}
        </button>
        {editingId && (
          <button type="button" onClick={cancelEdit}>
            Cancelar
          </button>
        )}
      </form>

      <StatusMessage variant={feedback?.variant} onClose={() => setFeedback(null)}>
        {feedback?.text}
      </StatusMessage>
    </div>
  );
}
