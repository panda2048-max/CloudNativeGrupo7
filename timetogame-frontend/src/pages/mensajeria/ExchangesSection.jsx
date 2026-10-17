import { useState } from "react";
import { createExchange, deleteExchange } from "../../api/rabbitAdminApi";
import { StatusMessage, messageForError, variantForStatus } from "../../components/StatusMessage";
import { limpiarPayload } from "./formUtils";

const formVacio = { nombre: "", tipo: "TOPIC", durable: true };

export function ExchangesSection({ exchanges, onCambio }) {
  const [form, setForm] = useState(formVacio);
  const [soloSiSinBindings, setSoloSiSinBindings] = useState(true);
  const [enviando, setEnviando] = useState(false);
  const [feedback, setFeedback] = useState(null);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setEnviando(true);
    setFeedback(null);
    try {
      const creado = await createExchange(limpiarPayload(form));
      setFeedback({ variant: "success", text: `Exchange "${creado.nombre}" (${creado.tipo}) creado.` });
      setForm(formVacio);
      await onCambio();
    } catch (err) {
      setFeedback({ variant: variantForStatus(err.status), text: messageForError(err) });
    } finally {
      setEnviando(false);
    }
  };

  const handleDelete = async (exchange) => {
    setFeedback(null);
    try {
      await deleteExchange(exchange.nombre, { soloSiSinBindings });
      setFeedback({ variant: "success", text: `Exchange "${exchange.nombre}" eliminado.` });
      await onCambio();
    } catch (err) {
      setFeedback({ variant: variantForStatus(err.status), text: messageForError(err) });
    }
  };

  return (
    <div className="admin-section">
      <h2>Exchanges ({exchanges.length})</h2>

      <table className="table">
        <thead>
          <tr>
            <th>Nombre</th>
            <th>Tipo</th>
            <th>Durable</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {exchanges.map((exchange) => (
            <tr key={exchange.nombre}>
              <td>{exchange.nombre}</td>
              <td>{exchange.tipo}</td>
              <td>{exchange.durable ? "Si" : "No"}</td>
              <td className="table__actions">
                <button type="button" className="button--danger" onClick={() => handleDelete(exchange)}>
                  Eliminar
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <label className="checkbox">
        <input
          type="checkbox"
          checked={soloSiSinBindings}
          onChange={(event) => setSoloSiSinBindings(event.target.checked)}
        />
        Eliminar solo si no tiene bindings
      </label>

      <form className="form form--inline" onSubmit={handleSubmit}>
        <input
          placeholder="Nombre del exchange"
          value={form.nombre}
          onChange={(event) => setForm({ ...form, nombre: event.target.value })}
          required
        />
        <select value={form.tipo} onChange={(event) => setForm({ ...form, tipo: event.target.value })}>
          <option value="TOPIC">topic</option>
          <option value="DIRECT">direct</option>
          <option value="FANOUT">fanout</option>
          <option value="HEADERS">headers</option>
        </select>
        <label className="checkbox">
          <input
            type="checkbox"
            checked={form.durable}
            onChange={(event) => setForm({ ...form, durable: event.target.checked })}
          />
          Durable
        </label>
        <button type="submit" disabled={enviando}>
          Crear exchange
        </button>
      </form>

      <StatusMessage variant={feedback?.variant} onClose={() => setFeedback(null)}>
        {feedback?.text}
      </StatusMessage>
    </div>
  );
}
