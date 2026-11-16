import { useCallback, useEffect, useState } from "react";
import { fetchGrupo, fetchGrupos } from "../../../api/streamingApi";
import { StatusMessage } from "../../../components/StatusMessage";

const INTERVALO_MS = 2000;

/**
 * Consumer groups con su lag. Con "Auto" encendido se refresca cada 2 s: al
 * simular carga se ve el lag subir y luego bajar a medida que los
 * consumidores (uno por particion) lo procesan.
 */
export function KafkaGruposSection() {
  const [grupos, setGrupos] = useState([]);
  const [detalles, setDetalles] = useState({});
  const [seleccionado, setSeleccionado] = useState(null);
  const [auto, setAuto] = useState(true);
  const [error, setError] = useState(null);

  const cargar = useCallback(async () => {
    try {
      const lista = await fetchGrupos();
      const pares = await Promise.all(
        lista.map((g) =>
          fetchGrupo(g.groupId)
            .then((d) => [g.groupId, d])
            .catch(() => [g.groupId, null])
        )
      );
      setGrupos(lista);
      setDetalles(Object.fromEntries(pares));
      setError(null);
    } catch (err) {
      setError(err.message);
    }
  }, []);

  useEffect(() => {
    cargar();
    if (!auto) return undefined;
    const id = setInterval(cargar, INTERVALO_MS);
    return () => clearInterval(id);
  }, [auto, cargar]);

  const detalle = seleccionado ? detalles[seleccionado] : null;

  return (
    <div className="admin-section">
      <div className="section-header">
        <h2>Consumer groups ({grupos.length})</h2>
        <label className="checkbox">
          <input type="checkbox" checked={auto} onChange={(e) => setAuto(e.target.checked)} />
          Auto (2 s)
        </label>
      </div>
      <StatusMessage variant="error">{error}</StatusMessage>

      <table className="table">
        <thead>
          <tr>
            <th>Grupo</th>
            <th>Estado</th>
            <th>Consumidores</th>
            <th>Lag total</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {grupos.map((g) => {
            const d = detalles[g.groupId];
            return (
              <tr key={g.groupId}>
                <td>{g.groupId}</td>
                <td>{g.estado}</td>
                <td>{d ? d.miembros.length : "-"}</td>
                <td>
                  <strong>{d ? d.lagTotal : "-"}</strong>
                </td>
                <td className="table__actions">
                  <button type="button" onClick={() => setSeleccionado(g.groupId)}>
                    Detalle
                  </button>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>

      {detalle && (
        <div className="admin-section">
          <div className="section-header">
            <h3>
              {detalle.groupId} ({detalle.estado}, asignador {detalle.asignador || "-"})
            </h3>
            <button type="button" onClick={() => setSeleccionado(null)}>
              Cerrar
            </button>
          </div>

          <h4>Reparto de particiones</h4>
          <table className="table">
            <thead>
              <tr>
                <th>Consumidor</th>
                <th>Host</th>
                <th>Particiones asignadas</th>
              </tr>
            </thead>
            <tbody>
              {detalle.miembros.map((m) => (
                <tr key={m.consumerId}>
                  <td>{m.clientId}</td>
                  <td>{m.host}</td>
                  <td>{m.particiones.join(", ") || "ninguna"}</td>
                </tr>
              ))}
              {detalle.miembros.length === 0 && (
                <tr>
                  <td colSpan={3}>Sin consumidores activos.</td>
                </tr>
              )}
            </tbody>
          </table>

          <h4>Offsets y lag</h4>
          <table className="table">
            <thead>
              <tr>
                <th>Topico-particion</th>
                <th>Offset consumido</th>
                <th>Fin del log</th>
                <th>Lag</th>
              </tr>
            </thead>
            <tbody>
              {detalle.offsets.map((o) => (
                <tr key={`${o.topico}-${o.particion}`}>
                  <td>
                    {o.topico}-{o.particion}
                  </td>
                  <td>{o.offsetConsumido}</td>
                  <td>{o.offsetFinal}</td>
                  <td>
                    <strong>{o.lag}</strong>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
