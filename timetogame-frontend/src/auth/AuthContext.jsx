import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { useMsal, useIsAuthenticated } from "@azure/msal-react";
import { InteractionStatus, InteractionRequiredAuthError } from "@azure/msal-browser";
import { onUnauthorized } from "../api/httpClient";
import { extractRoles } from "./jwt";
import { loginRequest } from "./msalConfig";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const { instance, accounts, inProgress } = useMsal();
  const isMsalAuthenticated = useIsAuthenticated();
  const [accessToken, setAccessToken] = useState(null);
  const [sessionMessage, setSessionMessage] = useState(null);

  const account = accounts[0] ?? null;

  useEffect(() => {
    if (!account || inProgress !== InteractionStatus.None) return;

    instance
      .acquireTokenSilent({ ...loginRequest, account })
      .then((result) => setAccessToken(result.accessToken))
      .catch((error) => {
        setAccessToken(null);
        console.error("acquireTokenSilent fallo:", error);
        if (error instanceof InteractionRequiredAuthError) {
          // Requiere interaccion (p.ej. consentimiento revocado o MFA
          // vencido): no podemos resolverlo en silencio.
          setSessionMessage("Tu sesion expiro o no es valida. Inicia sesion nuevamente.");
        } else {
          // Cualquier otro fallo (scope/audience mal configurado, red, etc.)
          // tambien debe verse: antes se perdia en silencio y el usuario
          // quedaba "logueado" sin token, sin username ni rol visibles.
          setSessionMessage(`No se pudo obtener el token de acceso: ${error.errorCode ?? error.message}`);
        }
      });
  }, [account, inProgress, instance]);

  useEffect(() => {
    onUnauthorized(() => {
      setSessionMessage("Tu sesion expiro o no es valida. Inicia sesion nuevamente.");
      instance.setActiveAccount(null);
      setAccessToken(null);
    });
  }, [instance]);

  // Dos tokens distintos, con proposito distinto: la identidad de la cuenta
  // (account.username) identifica a la persona frente al frontend; el
  // Access Token es el unico que viaja al backend como credencial de recurso.
  const user = useMemo(() => {
    if (!account || !accessToken) return null;
    return {
      username: account.username,
      roles: extractRoles(accessToken),
    };
  }, [account, accessToken]);

  const initializing =
    inProgress === InteractionStatus.Startup || inProgress === InteractionStatus.HandleRedirect;

  const login = (redirectTo) =>
    instance.loginRedirect({ ...loginRequest, state: redirectTo }).catch((error) => {
      // Si un intento anterior quedo a medias (p.ej. se cerro la pestana en
      // medio del redirect a Microsoft), MSAL puede quedar con un flag de
      // "interaccion en progreso" en sessionStorage que bloquea el
      // siguiente intento en silencio. Lo limpiamos y avisamos para no
      // dejar el boton pareciendo que no hace nada.
      if (error.errorCode === "interaction_in_progress") {
        sessionStorage.removeItem("msal.interaction.status");
        setSessionMessage("Hubo un problema con el inicio de sesion anterior. Intenta de nuevo.");
        return;
      }
      setSessionMessage(`No se pudo iniciar sesion: ${error.message}`);
    });
  const logout = () => instance.logoutRedirect();
  const hasRole = (role) => user?.roles?.includes(role) ?? false;

  const value = useMemo(
    () => ({
      user,
      accessToken,
      isAuthenticated: isMsalAuthenticated && Boolean(user),
      initializing,
      sessionMessage,
      clearSessionMessage: () => setSessionMessage(null),
      login,
      logout,
      hasRole,
    }),
    [user, accessToken, isMsalAuthenticated, initializing, sessionMessage]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth debe usarse dentro de un AuthProvider");
  }
  return context;
}
