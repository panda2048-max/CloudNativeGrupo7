import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { onUnauthorized } from "../api/httpClient";
import { extractRealmRoles } from "./jwt";
import { userManager } from "./oidcUserManager";

const AuthContext = createContext(null);

function toAuthState(oidcUser) {
  if (!oidcUser || oidcUser.expired) return { user: null, idToken: null, accessToken: null };

  return {
    user: {
      username: oidcUser.profile.preferred_username ?? oidcUser.profile.sub,
      roles: extractRealmRoles(oidcUser.access_token),
    },
    // Dos tokens distintos, con proposito distinto: el ID Token identifica
    // a la persona frente al frontend; el Access Token es el unico que
    // viaja al backend como credencial de recurso.
    idToken: oidcUser.id_token,
    accessToken: oidcUser.access_token,
  };
}

export function AuthProvider({ children }) {
  const [{ user, accessToken }, setState] = useState({ user: null, idToken: null, accessToken: null });
  const [initializing, setInitializing] = useState(true);
  const [sessionMessage, setSessionMessage] = useState(null);

  useEffect(() => {
    userManager
      .getUser()
      .then((oidcUser) => setState(toAuthState(oidcUser)))
      .finally(() => setInitializing(false));

    const onLoaded = (oidcUser) => setState(toAuthState(oidcUser));
    const onUnloaded = () => setState(toAuthState(null));

    userManager.events.addUserLoaded(onLoaded);
    userManager.events.addUserUnloaded(onUnloaded);
    userManager.events.addSilentRenewError(() => {
      setSessionMessage("No se pudo renovar la sesion. Inicia sesion nuevamente.");
    });

    return () => {
      userManager.events.removeUserLoaded(onLoaded);
      userManager.events.removeUserUnloaded(onUnloaded);
    };
  }, []);

  useEffect(() => {
    onUnauthorized(() => {
      setSessionMessage("Tu sesion expiro o no es valida. Inicia sesion nuevamente.");
      userManager.removeUser();
    });
  }, []);

  const login = (redirectTo) => userManager.signinRedirect({ state: { redirectTo } });
  const logout = () => userManager.signoutRedirect();
  const hasRole = (role) => user?.roles?.includes(role) ?? false;

  const value = useMemo(
    () => ({
      user,
      accessToken,
      isAuthenticated: Boolean(user),
      initializing,
      sessionMessage,
      clearSessionMessage: () => setSessionMessage(null),
      login,
      logout,
      hasRole,
    }),
    [user, accessToken, initializing, sessionMessage]
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
