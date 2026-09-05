import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { login as loginRequest } from "../api/authApi";
import { onUnauthorized } from "../api/httpClient";
import { decodeJwtPayload, isTokenExpired } from "./jwt";
import { clearStoredToken, getStoredToken, setStoredToken } from "./tokenStorage";

const AuthContext = createContext(null);

function buildUserFromToken(token) {
  const payload = decodeJwtPayload(token);
  if (!payload) return null;
  return {
    username: payload.sub,
    roles: payload.roles ?? [],
  };
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(null);
  const [user, setUser] = useState(null);
  const [initializing, setInitializing] = useState(true);
  const [sessionMessage, setSessionMessage] = useState(null);

  const logout = (message) => {
    clearStoredToken();
    setToken(null);
    setUser(null);
    if (message) setSessionMessage(message);
  };

  useEffect(() => {
    const stored = getStoredToken();
    if (stored && !isTokenExpired(stored)) {
      setToken(stored);
      setUser(buildUserFromToken(stored));
    } else if (stored) {
      clearStoredToken();
    }
    setInitializing(false);
  }, []);

  useEffect(() => {
    onUnauthorized(() => {
      logout("Tu sesion expiro o no es valida. Inicia sesion nuevamente.");
    });
  }, []);

  const login = async (username, password) => {
    const response = await loginRequest(username, password);
    setStoredToken(response.token);
    setToken(response.token);
    setUser({ username: response.username, roles: response.roles });
    setSessionMessage(null);
    return response;
  };

  const hasRole = (role) => user?.roles?.includes(role) ?? false;

  const value = useMemo(
    () => ({
      token,
      user,
      isAuthenticated: Boolean(token),
      initializing,
      sessionMessage,
      clearSessionMessage: () => setSessionMessage(null),
      login,
      logout,
      hasRole,
    }),
    [token, user, initializing, sessionMessage]
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
