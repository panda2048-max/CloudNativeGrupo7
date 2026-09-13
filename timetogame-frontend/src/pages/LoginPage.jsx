import { useLocation } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function LoginPage() {
  const { login } = useAuth();
  const location = useLocation();
  const from = location.state?.from ?? "/panel";

  return (
    <section className="page page--narrow">
      <h1>Iniciar sesion</h1>
      <p>
        El inicio de sesion lo maneja Microsoft Entra ID (Azure AD), no este
        frontend: al continuar seras redirigido a su pantalla de login
        mediante Authorization Code + PKCE.
      </p>
      <button type="button" onClick={() => login(from)}>
        Continuar con Microsoft
      </button>
    </section>
  );
}
