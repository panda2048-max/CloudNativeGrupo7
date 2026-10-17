import { useState } from "react";
import { createCola, deleteCola } from "../../api/rabbitAdminApi";
import { StatusMessage, messageForError, variantForStatus } from "../../components/StatusMessage";
import { limpiarPayload } from "./formUtils";

const formVacio = {
  nombre: "",
  tipo: "CLASSIC",
  durable: true,
  deadLetterExchange: "",
  deadLetterRoutingKey: "",
  ttlMensajesMs: "",
  maxMensajes: "",
};

function resumirArgumentos(argumentos = {}) {
  const partes = [];
  if (argumentos["x-dead-letter-exchange"] !== undefined) {
    partes.push(`DLX: ${argumentos["x-dead-letter-exchange"] || "(default)"}`);
  }
  if (argumentos["x-message-ttl"] !== undefined) partes.push(`TTL: ${argumentos["x-message-ttl"]} ms`);
  if (argumentos["x-max-length"] !== undefined) partes.push(`Max: ${argumentos["x-max-length"]}`);
  return partes.join(" | ");
}

export function ColasSection({ colas, exchanges, onCambio }) {
  const [form, setForm] = useState(formVacio);
  const [condiciones, setCondiciones] = useState({ soloSiSinConsumidores: true, soloSiVacia: true });
  const [enviando, setEnviando] = useState(false);
  const [feedback, setFeedback] = useState(null);

  const cambiar = (campo) => (event) => {
    const valor = event.target.type === "checkbox" ? event.target.checked : event.target.value;
    setForm({ ...form, [campo]: valor });
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setEnviando(true);
    setFeedback(null);
    try {
      const creada = await createCola(limpiarPayload(form, ["ttlMensajesMs", "maxMensajes"]));
      setFeedback({ variant: "success", text: `Cola "${creada.nombre}" creada.` });
      setForm(formVacio);
      await onCambio();
    } catch (err) {
      setFeedback({ variant: variantForStatus(err.status), text: messageForError(err) });
    } finally {
      setEnviando(false);
    }
  };

  const handleDelete = async (cola) => {
    setFeedback(null);
    try {
      await deleteCola(cola.nombre, condiciones);
      setFeedback({ variant: "success", text: `Cola "${cola.nombre}" eliminada.` });
      await onCambio();
    } catch (err) {
      setFeedback({ variant: variantForStatus(err.status), text: messageForError(err) });
    }
  };

  return (
    <div className="admin-section">
      <h2>Colas ({colas.length})</h2>

      <table className="table">
        <thead>
          <tr>
            <th>Nombre</th>
            <th>Tipo</th>
            <th>Mensajes</th>
            <th>Consumidores</th>
            <th>Configuracion</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {colas.map((cola) => (
            <tr key={cola.nombre}>
              <td>{cola.nombre}</td>
              <td>{cola.tipo}</td>
              <td>{cola.mensajes ?? "-"}</td>
              <td>{cola.consumidores ?? "-"}</td>
              <td>{resumirArgumentos(cola.argumentos)}</td>
              <td className="table__actions">
                <button type="button" className="button--danger" onClick={() => handleDelete(cola)}>
                  Eliminar
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <div className="form form--inline">
        <label className="checkbox">
          <input
            type="checkbox"
            checked={condiciones.soloSiSinConsumidores}
            onChange={(event) => setCondiciones({ ...condiciones, soloSiSinConsumidores: event.target.checked })}
          />
          Eliminar solo si no tiene consumidores
        </label>
        <label className="checkbox">
          <input
            type="checkbox"
            checked={condiciones.soloSiVacia}
            onChange={(event) => setCondiciones({ ...condiciones, soloSiVacia: event.target.checked })}
          />
          Eliminar solo si esta vacia
        </label>
      </div>

      <form className="form form--grid" onSubmit={handleSubmit}>
        <input placeholder="Nombre de la cola" value={form.nombre} onChange={cambiar("nombre")} required />
        <select value={form.tipo} onChange={cambiar("tipo")}>
          <option value="CLASSIC">classic</option>
          <option value="QUORUM">quorum</option>
        </select>
        <select value={form.deadLetterExchange} onChange={cambiar("deadLetterExchange")}>
          <option value="">Sin dead letter exchange</option>
          {exchanges.map((exchange) => (
            <option key={exchange.nombre} value={exchange.nombre}>
              DLX: {exchange.nombre}
            </option>
          ))}
        </select>
        <input
          placeholder="Dead letter routing key"
          value={form.deadLetterRoutingKey}
          onChange={cambiar("deadLetterRoutingKey")}
        />
        <input
          type="number"
          placeholder="TTL de mensajes (ms)"
          value={form.ttlMensajesMs}
          onChange={cambiar("ttlMensajesMs")}
        />
        <input
          type="number"
          placeholder="Maximo de mensajes"
          value={form.maxMensajes}
          onChange={cambiar("maxMensajes")}
        />
        <label className="checkbox">
          <input type="checkbox" checked={form.durable} onChange={cambiar("durable")} />
          Durable
        </label>
        <button type="submit" disabled={enviando}>
          Crear cola
        </button>
      </form>

      <StatusMessage variant={feedback?.variant} onClose={() => setFeedback(null)}>
        {feedback?.text}
      </StatusMessage>
    </div>
  );
}
