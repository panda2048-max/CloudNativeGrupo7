import { useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { StatusMessage } from "../components/StatusMessage";
import { AdminGenerosSection } from "./admin/AdminGenerosSection";
import { AdminVideojuegosSection } from "./admin/AdminVideojuegosSection";

export function AdminPage() {
  const { hasRole } = useAuth();
  const [generos, setGeneros] = useState([]);
  const isAdmin = hasRole("ROLE_ADMIN");

  return (
    <section className="page">
      <h1>Administracion</h1>
      <p>
        Alta, edicion y eliminacion de generos y videojuegos (
        <code>POST</code>, <code>PUT</code>, <code>DELETE</code>). El backend
        exige rol <strong>ADMIN</strong> para estas operaciones.
      </p>

      {!isAdmin && (
        <StatusMessage variant="warning">
          Tu cuenta no tiene el rol ADMIN: podras ver los formularios, pero el
          backend rechazara cualquier intento de crear, editar o eliminar con
          un error 403.
        </StatusMessage>
      )}

      <AdminGenerosSection onGenerosChange={setGeneros} />
      <AdminVideojuegosSection generos={generos} />
    </section>
  );
}
