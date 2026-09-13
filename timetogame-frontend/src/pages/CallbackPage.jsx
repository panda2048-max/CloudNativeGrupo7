import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useMsal } from "@azure/msal-react";
import { EventType } from "@azure/msal-browser";
import { LoadingIndicator } from "../components/LoadingIndicator";
import { StatusMessage } from "../components/StatusMessage";

export function CallbackPage() {
  const { instance } = useMsal();
  const navigate = useNavigate();
  const [error, setError] = useState(null);

  useEffect(() => {
    // MsalProvider ya procesa la respuesta del redirect por su cuenta; aqui
    // solo escuchamos el resultado para decidir a donde navegar (el
    // "redirectTo" original viaja en el parametro "state" del login).
    const callbackId = instance.addEventCallback((event) => {
      if (event.eventType === EventType.LOGIN_SUCCESS) {
        navigate(event.payload?.state || "/panel", { replace: true });
      }
      if (event.eventType === EventType.LOGIN_FAILURE) {
        setError(event.error?.message ?? "Error desconocido");
      }
    });

    return () => {
      if (callbackId) instance.removeEventCallback(callbackId);
    };
  }, [instance, navigate]);

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
