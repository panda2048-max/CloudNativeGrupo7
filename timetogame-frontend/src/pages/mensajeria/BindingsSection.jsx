import { useState } from "react";
import { createBinding, deleteBinding } from "../../api/rabbitAdminApi";
import { StatusMessage, messageForError, variantForStatus } from "../../components/StatusMessage";

const formVacio = { exchange: "", cola: "", routingKey: "" };

export function BindingsSection({ bindings, exchanges, colas, onCambio }) {
  const [form, setForm] = useState(formVacio);
  const [enviando, setEnviando] = useState(false);
  const [feedback, setFeedback] = useState(null);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setEnviando(true);
    setFeedback(null);
    try {
      const creado = await createBinding(form);
      setFeedback({
        variant: "success",
        text: `Binding ${creado.exchange} -> ${creado.cola} ("${creado.routingKey}") creado.`,
      });
      setForm(formVacio);
      await onCambio();
    } catch (err) {
      setFeedback({ variant: variantForStatus(err.status), text: messageForError(err) });
    } finally {
      setEnviando(false);
    }
  };

  const handleDelete = async (binding) => {
    setFeedback(null);
    try {
      await deleteBinding(binding);
      setFeedback({ variant: "success", text: `Binding ${binding.exchange} -> ${binding.cola} eliminado.` });
      await onCambio();
    } catch (err) {
      setFeedback({ variant: variantForStatus(err.status), text: messageForError(err) });
    }
  };

  return (
    <div className="admin-section">
      <h2>Bindings ({bindings.length})</h2>

      <table className="table">
        <thead>
          <tr>
            <th>Exchange</th>
            <th>Cola</th>
            <th>Routing key</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {bindings.map((binding) => (
            <tr key={`${binding.exchange}|${binding.cola}|${binding.routingKey}`}>
              <td>{binding.exchange}</td>
              <td>{binding.cola}</td>
              <td>
                <code>{binding.routingKey || "(vacia)"}</code>
              </td>
              <td className="table__actions">
                <button type="button" className="button--danger" onClick={() => handleDelete(binding)}>
                  Eliminar
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <form className="form form--inline" onSubmit={handleSubmit}>
        <select
          value={form.exchange}
          onChange={(event) => setForm({ ...form, exchange: event.target.value })}
          required
        >
          <option value="">Exchange origen</option>
          {exchanges.map((exchange) => (
            <option key={exchange.nombre} value={exchange.nombre}>
              {exchange.nombre} ({exchange.tipo})
            </option>
          ))}
        </select>
        <select value={form.cola} onChange={(event) => setForm({ ...form, cola: event.target.value })} required>
          <option value="">Cola destino</option>
          {colas.map((cola) => (
            <option key={cola.nombre} value={cola.nombre}>
              {cola.nombre}
            </option>
          ))}
        </select>
        <input
          placeholder="Routing key (ej. catalogo.videojuego.*)"
          value={form.routingKey}
          onChange={(event) => setForm({ ...form, routingKey: event.target.value })}
        />
        <button type="submit" disabled={enviando}>
          Crear binding
        </button>
      </form>

      <StatusMessage variant={feedback?.variant} onClose={() => setFeedback(null)}>
        {feedback?.text}
      </StatusMessage>
    </div>
  );
}
