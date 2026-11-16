/** Calificacion de 1 a 5 como estrellas, con texto accesible. */
export function Estrellas({ valor }) {
  const redondeado = Math.round(valor);
  return (
    <span className="estrellas" role="img" aria-label={`${valor} de 5 estrellas`}>
      {"★".repeat(redondeado)}
      <span className="estrellas__vacias">{"★".repeat(5 - redondeado)}</span>
    </span>
  );
}
