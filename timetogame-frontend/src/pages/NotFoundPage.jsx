import { Link } from "react-router-dom";

export function NotFoundPage() {
  return (
    <section className="page">
      <h1>404</h1>
      <p>La pagina que buscas no existe.</p>
      <Link to="/">Volver al inicio</Link>
    </section>
  );
}
