import { useCallback, useEffect, useState } from "react";
import { createTopico, deleteTopico, fetchTopico, fetchTopicos, updateTopico } from "../../../api/streamingApi";
import { StatusMessage, messageForError, variantForStatus } from "../../../components/StatusMessage";
import { limpiarPayload } from "../formUtils";

const formVacio = {
  nombre: "",
  particiones: 3,
  replicas: 3,
  politicaLimpieza: "DELETE",
  retencionMs: "",
  minInsyncReplicas: "",
};

const cambiosVacios = { particiones: "", politicaLimpieza: "", retencionMs: "", minInsyncReplicas: "" };

const NUMERICOS = ["particiones", "replicas", "retencionMs", "minInsyncReplicas"];

function retencionLegible(ms) {
  if (ms === undefined) return "-";
  if (ms === "-1") return "infinita";
  const horas = Number(ms) / 3_600_000;
  return horas >= 24 ? `${horas / 24} dias` : `${horas} h`;
}

export function KafkaTopicosSection() {
  const [topicos, setTopicos] = useState([]);
  const [error, setError] = useState(null);
  const [form, setForm] = useState(formVacio);
  const [seleccionado, setSeleccionado] = useState(null);
  const [cambios, setCambios] = useState(cambiosVacios);
  const [feedback, setFeedback] = useState(null);

  const cargar = useCallback(() => {
    setError(null);
    return fetchTopicos()
      .then(setTopicos)
      .catch((err) => setError(err.message));
  }, []);

  useEffect(() => {
    cargar();
  }, [cargar]);

  const ejecutar = async (accion, exito) => {
    setFeedback(null);
    try {
      const valor = await accion();
      setFeedback({ variant: "success", text: exito });
      await cargar();
      return { ok: true, valor };
    } catch (err) {
      setFeedback({ variant: variantForStatus(err.status), text: messageForError(err) });
      return { ok: false };
    }
  };

  const ver = async (nombre) => {
    setCambios(cambiosVacios);
    try {
      setSeleccionado(await fetchTopico(nombre));
    } catch (err) {
      setFeedback({ variant: variantForStatus(err.status), text: messageForError(err) });
    }
  };

  const handleCrear = async (event) => {
    event.preventDefault();
    const { ok, valor } = await ejecutar(
      () => createTopico(limpiarPayload(form, NUMERICOS)),
      `Topico "${form.nombre}" creado.`
    );
    if (ok) {
      setForm(formVacio);
      setSeleccionado(valor);
    }
  };

  const handleActualizar = async (event) => {
    event.preventDefault();
    const { ok, valor } = await ejecutar(
      () => updateTopico(seleccionado.nombre, limpiarPayload(cambios, NUMERICOS)),
      `Topico "${seleccionado.nombre}" actualizado.`
    );
    if (ok) {
      setSeleccionado(valor);
      setCambios(cambiosVacios);
    }
  };

  const handleEliminar = async (nombre) => {
    const { ok } = await ejecutar(() => deleteTopico(nombre), `Topico "${nombre}" eliminado.`);
    if (ok && seleccionado?.nombre === nombre) setSeleccionado(null);
  };

  return (
    <div className="admin-section">
      <div className="section-header">
        <h2>Topicos ({topicos.length})</h2>
        <button type="button" onClick={cargar}>
          Actualizar
        </button>
      </div>
      <StatusMessage variant="error">{error}</StatusMessage>

      <table className="table">
        <thead>
          <tr>
            <th>Nombre</th>
            <th>Particiones</th>
            <th>Replicas</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {topicos.map((t) => (
            <tr key={t.nombre}>
              <td>
                {t.nombre} {t.interno && <span className="tag">interno</span>}
              </td>
              <td>{t.particiones}</td>
              <td>{t.replicas}</td>
              <td className="table__actions">
                <button type="button" onClick={() => ver(t.nombre)}>
                  Detalle
                </button>
                {!t.interno && (
                  <button type="button" className="button--danger" onClick={() => handleEliminar(t.nombre)}>
                    Eliminar
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {seleccionado && (
        <div className="admin-section">
          <div className="section-header">
            <h3>{seleccionado.nombre}</h3>
            <button type="button" onClick={() => setSeleccionado(null)}>
              Cerrar
            </button>
          </div>
          <p>
            cleanup.policy: <strong>{seleccionado.configuracion["cleanup.policy"] ?? "-"}</strong> | retencion:{" "}
            <strong>{retencionLegible(seleccionado.configuracion["retention.ms"])}</strong> | min.insync.replicas:{" "}
            <strong>{seleccionado.configuracion["min.insync.replicas"] ?? "-"}</strong>
          </p>
          <table className="table">
            <thead>
              <tr>
                <th>Particion</th>
                <th>Lider</th>
                <th>Replicas</th>
                <th>ISR (al dia)</th>
              </tr>
            </thead>
            <tbody>
              {seleccionado.particiones.map((p) => (
                <tr key={p.particion}>
                  <td>{p.particion}</td>
                  <td>broker {p.lider ?? "sin lider"}</td>
                  <td>{p.replicas.join(", ")}</td>
                  <td>{p.isr.join(", ")}</td>
                </tr>
              ))}
            </tbody>
          </table>

          <form className="form form--grid" onSubmit={handleActualizar}>
            <input
              type="number"
              placeholder={`Particiones (hoy ${seleccionado.particiones.length}, solo aumentar)`}
              value={cambios.particiones}
              onChange={(e) => setCambios({ ...cambios, particiones: e.target.value })}
            />
            <select
              value={cambios.politicaLimpieza}
              onChange={(e) => setCambios({ ...cambios, politicaLimpieza: e.target.value })}
            >
              <option value="">cleanup.policy sin cambio</option>
              <option value="DELETE">delete</option>
              <option value="COMPACT">compact</option>
            </select>
            <input
              type="number"
              placeholder="Retencion en ms (-1 = infinita)"
              value={cambios.retencionMs}
              onChange={(e) => setCambios({ ...cambios, retencionMs: e.target.value })}
            />
            <input
              type="number"
              placeholder="min.insync.replicas"
              value={cambios.minInsyncReplicas}
              onChange={(e) => setCambios({ ...cambios, minInsyncReplicas: e.target.value })}
            />
            <button type="submit">Guardar cambios</button>
          </form>
        </div>
      )}

      <form className="form form--grid" onSubmit={handleCrear}>
        <input
          placeholder="Nombre del topico"
          value={form.nombre}
          onChange={(e) => setForm({ ...form, nombre: e.target.value })}
          required
        />
        <input
          type="number"
          placeholder="Particiones"
          value={form.particiones}
          onChange={(e) => setForm({ ...form, particiones: e.target.value })}
          required
        />
        <input
          type="number"
          placeholder="Replicas"
          value={form.replicas}
          onChange={(e) => setForm({ ...form, replicas: e.target.value })}
          required
        />
        <select value={form.politicaLimpieza} onChange={(e) => setForm({ ...form, politicaLimpieza: e.target.value })}>
          <option value="DELETE">delete (por tiempo)</option>
          <option value="COMPACT">compact (ultimo por key)</option>
        </select>
        <input
          type="number"
          placeholder="Retencion en ms (opcional)"
          value={form.retencionMs}
          onChange={(e) => setForm({ ...form, retencionMs: e.target.value })}
        />
        <input
          type="number"
          placeholder="min.insync.replicas (opcional)"
          value={form.minInsyncReplicas}
          onChange={(e) => setForm({ ...form, minInsyncReplicas: e.target.value })}
        />
        <button type="submit">Crear topico</button>
      </form>

      <StatusMessage variant={feedback?.variant} onClose={() => setFeedback(null)}>
        {feedback?.text}
      </StatusMessage>
    </div>
  );
}
