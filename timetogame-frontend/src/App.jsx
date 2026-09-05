import { Route, Routes } from "react-router-dom";
import { NavBar } from "./components/NavBar";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { StatusMessage } from "./components/StatusMessage";
import { useAuth } from "./auth/AuthContext";
import { AdminPage } from "./pages/AdminPage";
import { CallbackPage } from "./pages/CallbackPage";
import { CatalogoPage } from "./pages/CatalogoPage";
import { HomePage } from "./pages/HomePage";
import { LoginPage } from "./pages/LoginPage";
import { NotFoundPage } from "./pages/NotFoundPage";
import { PanelPage } from "./pages/PanelPage";
import { VideojuegoDetallePage } from "./pages/VideojuegoDetallePage";

export function App() {
  const { sessionMessage, clearSessionMessage } = useAuth();

  return (
    <div className="app">
      <NavBar />

      <main className="app__main">
        <StatusMessage variant="warning" onClose={clearSessionMessage}>
          {sessionMessage}
        </StatusMessage>

        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/catalogo" element={<CatalogoPage />} />
          <Route path="/catalogo/:id" element={<VideojuegoDetallePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/callback" element={<CallbackPage />} />
          <Route
            path="/panel"
            element={
              <ProtectedRoute>
                <PanelPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin"
            element={
              <ProtectedRoute>
                <AdminPage />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </main>
    </div>
  );
}
