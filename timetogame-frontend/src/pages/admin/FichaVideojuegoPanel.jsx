import { useCallback, useEffect, useRef, useState } from "react";
import { fetchFichaVideojuego, solicitarFichaVideojuego } from "../../api/videojuegosApi";
import { LoadingIndicator } from "../../components/LoadingIndicator";
import { StatusMessage, variantForStatus } from "../../components/StatusMessage";

const INTERVALO_CONSULTA_MS = 1500;
const MAX_CONSULTAS = 10;

/**
 * Ficha tecnica de un videojuego, generada de forma asincrona: POST responde
 * 202 al instante y el consumidor de la cola timetogame.documentos.fichas la
 * escribe despues. Tras solicitarla se consulta hasta ver una version nueva.
 */
export function FichaVideojuegoPanel({ videojuego, onClose }) {
  const [ficha, setFicha] = useState(null);
  const [loading, setLoading] = useState(true);
  const [estado, setEstado] = useState(null);
  const [solicitando, setSolicitando] = useState(false);
  const temporizador = useRef(null);

  const cargar = useCallback(async () => {
    try {
      const data = await fetchFichaVideojuego(videojuego.id);
      setFicha(data);
      return data;
    } catch (err) {
      if (err.status === 404) {
        setFicha(null);
        return null;
      }
      throw err;
    }
  }, [videojuego.id]);

  useEffect(() => {
    setLoading(true);
    setEstado(null);
    cargar()
      .catch((err) => setEstado({ variant: variantForStatus(err.status), text: err.message }))
      .finally(() => setLoading(false));
    return () => clearTimeout(temporizador.current);
  }, [cargar]);

  const esperarNuevaVersion = (versionAnterior, intento = 1) => {
    temporizador.current = setTimeout(async () => {
      try {
        const data = await cargar();
        if (data && data.version > versionAnterior) {
          setEstado({ variant: "success", text: `Ficha generada (version ${data.version}).` });
          setSolicitando(false);
        } else if (intento >= MAX_CONSULTAS) {
          setEstado({
            variant: "warning",
            text: "La solicitud sigue en cola. Revisa la DLQ en Mensajeria si no aparece.",
          });
          setSolicitando(false);
        } else {
          esperarNuevaVersion(versionAnterior, intento + 1);
        }
      } catch (err) {
        setEstado({ variant: variantForStatus(err.status), text: err.message });
        setSolicitando(false);
      }
    }, INTERVALO_CONSULTA_MS);
  };

  const handleSolicitar = async () => {
    clearTimeout(temporizador.current);
    setSolicitando(true);
    try {
      const solicitud = await solicitarFichaVideojuego(videojuego.id);
      setEstado({
        variant: "info",
        text: `Solicitud ${solicitud.solicitudId.slice(0, 8)} en cola (202 Accepted). Generando...`,
      });
      esperarNuevaVersion(ficha?.version ?? 0);
    } catch (err) {
      setEstado({ variant: variantForStatus(err.status), text: err.message });
      setSolicitando(false);
    }
  };

  return (
    <div className="admin-section">
      <div className="section-header">
        <h3>Ficha tecnica: {videojuego.titulo}</h3>
        <div className="form__actions">
          <button type="button" onClick={handleSolicitar} disabled={solicitando}>
            {ficha ? "Regenerar ficha" : "Generar ficha"}
          </button>
          <button type="button" onClick={onClose}>
            Cerrar
          </button>
        </div>
      </div>

      {loading && <LoadingIndicator label="Cargando ficha..." />}
      {!loading && !ficha && <p className="hint">La ficha de este videojuego aun no se ha generado.</p>}
      {ficha && <pre className="ficha">{ficha.contenido}</pre>}

      <StatusMessage variant={estado?.variant} onClose={() => setEstado(null)}>
        {estado?.text}
      </StatusMessage>
    </div>
  );
}
