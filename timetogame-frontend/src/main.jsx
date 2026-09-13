import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { MsalProvider } from "@azure/msal-react";
import { App } from "./App";
import { AuthProvider } from "./auth/AuthContext";
import { msalInstance } from "./auth/msalConfig";
import "./index.css";

// msal-browser v3 exige inicializar la instancia (carga estado de cache,
// resuelve el navegador) antes de renderizar cualquier componente que
// dependa de ella.
async function bootstrap() {
  await msalInstance.initialize();

  createRoot(document.getElementById("root")).render(
    <StrictMode>
      <MsalProvider instance={msalInstance}>
        <BrowserRouter>
          <AuthProvider>
            <App />
          </AuthProvider>
        </BrowserRouter>
      </MsalProvider>
    </StrictMode>
  );
}

bootstrap();
