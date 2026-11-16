import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { STREAM_URL } from "../config";
import { Estrellas } from "./Estrellas";

const MAX_AVISOS = 5;
const MAX_RESENAS = 5;

const ESTADO = {
  conectando: { texto: "Conectando...", clase: "en-vivo en-vivo--conectando" },
  abierto: { texto: "En vivo", clase: "en-vivo en-vivo--abierto" },
  error: { texto: "Reconectando...", clase: "en-vivo en-vivo--error" },
};

/**
 * Ranking de los videojuegos mas vistos (ultimos 10 minutos), resenas
 * recientes y avisos del catalogo, empujados por el backend con Server-Sent
 * Events: la pantalla se actualiza sola, sin recargar ni consultar
 * periodicamente. EventSource reconecta solo si se corta la conexion.
 */
export function TendenciasEnVivo() {
  const [ranking, setRanking] = useState([]);
  const [resenas, setResenas] = useState([]);
  const [nuevas, setNuevas] = useState(new Set());
  const [avisos, setAvisos] = useState([]);
  const [estado, setEstado] = useState("conectando");

  useEffect(() => {
    const fuente = new EventSource(STREAM_URL);
    fuente.onopen = () => setEstado("abierto");
    fuente.onerror = () => setEstado("error");
    fuente.addEventListener("tendencias", (evento) => setRanking(JSON.parse(evento.data)));
    // Al conectarse llega la lista inicial; despues, una "resena" por cada nueva.
    fuente.addEventListener("resenas", (evento) => setResenas(JSON.parse(evento.data)));
    fuente.addEventListener("resena", (evento) => {
      const resena = { ...JSON.parse(evento.data), clave: crypto.randomUUID() };
      setResenas((previas) => [resena, ...previas].slice(0, MAX_RESENAS));
      setNuevas((previas) => new Set(previas).add(resena.clave));
    });
    fuente.addEventListener("catalogo", (evento) => {
      const aviso = JSON.parse(evento.data);
      setAvisos((previos) => [aviso, ...previos].slice(0, MAX_AVISOS));
    });
    return () => fuente.close();
  }, []);

  const maximo = Math.max(1, ...ranking.map((t) => t.vistas));

  return (
    <div className="admin-section tendencias">
      <div className="section-header">
        <h2>Tendencias en vivo</h2>
        <span className={ESTADO[estado].clase}>{ESTADO[estado].texto}</span>
      </div>
      <p className="hint">Los mas vistos de los ultimos 10 minutos (Kafka + Server-Sent Events).</p>

      {ranking.length === 0 ? (
        <p className="hint">Aun no hay vistas recientes. Abre la ficha de un juego para que aparezca aqui.</p>
      ) : (
        <ol className="ranking">
          {ranking.map((t) => (
            <li key={t.videojuegoId}>
              <Link to={`/catalogo/${t.videojuegoId}`}>{t.titulo}</Link>
              <span className="ranking__barra" style={{ width: `${(t.vistas / maximo) * 100}%` }} />
              <span className="ranking__vistas">{t.vistas}</span>
            </li>
          ))}
        </ol>
      )}

      <h3>Resenas recientes</h3>
      {resenas.length === 0 ? (
        <p className="hint">Todavia no hay resenas. Deja la primera desde la ficha de un juego.</p>
      ) : (
        <ul className="resenas">
          {resenas.map((r) => {
            const clave = r.clave ?? `bd-${r.id}`;
            return (
              <li key={clave} className={nuevas.has(clave) ? "resenas__nueva" : undefined}>
                <div>
                  <Estrellas valor={r.calificacion} /> <strong>{r.usuario}</strong> sobre{" "}
                  <Link to={`/catalogo/${r.videojuegoId}`}>{r.tituloVideojuego}</Link>
                </div>
                <p>{r.comentario}</p>
              </li>
            );
          })}
        </ul>
      )}

      {avisos.length > 0 && (
        <ul className="avisos">
          {avisos.map((a) => (
            <li key={a.eventId}>
              <span className={`tag tag--${a.tipo.toLowerCase()}`}>{a.tipo}</span> {a.mensaje}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
