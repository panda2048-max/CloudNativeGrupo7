import { useState } from "react";
import { AdminGenerosSection } from "./admin/AdminGenerosSection";
import { AdminVideojuegosSection } from "./admin/AdminVideojuegosSection";

export function AdminPage() {
  const [generos, setGeneros] = useState([]);

  return (
    <section className="page">
      <h1>Administracion</h1>
      <p>
        Alta, edicion y eliminacion de generos y videojuegos (
        <code>POST</code>, <code>PUT</code>, <code>DELETE</code>). El backend
        exige rol <strong>ADMIN</strong> para estas operaciones.
      </p>

      <AdminGenerosSection onGenerosChange={setGeneros} />
      <AdminVideojuegosSection generos={generos} />
    </section>
  );
}
