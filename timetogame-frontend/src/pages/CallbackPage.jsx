import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { userManager } from "../auth/oidcUserManager";
import { LoadingIndicator } from "../components/LoadingIndicator";
import { StatusMessage } from "../components/StatusMessage";

export function CallbackPage() {
  const navigate = useNavigate();
  const [error, setError] = useState(null);
  const startedRef = useRef(false);

  useEffect(() => {
    // El code de Authorization Code es de un solo uso: React StrictMode
    // monta los efectos dos veces en desarrollo, y sin esta guarda el
    // segundo intento reutiliza un code ya canjeado y Keycloak lo rechaza.
    if (startedRef.current) return;
    startedRef.current = true;

    userManager
      .signinRedirectCallback()
      .then((user) => {
        navigate(user.state?.redirectTo ?? "/panel", { replace: true });
      })
      .catch((err) => setError(err.message));
  }, [navigate]);

  return (
    <section className="page">
      {error ? (
        <StatusMessage variant="error">
          No se pudo completar el inicio de sesion: {error}
        </StatusMessage>
      ) : (
        <LoadingIndicator label="Completando inicio de sesion..." />
      )}
    </section>
  );
}
