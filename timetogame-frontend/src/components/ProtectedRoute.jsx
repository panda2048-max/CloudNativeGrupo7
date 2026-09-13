import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export function ProtectedRoute({ children, requiredRole }) {
  const { isAuthenticated, initializing, hasRole } = useAuth();
  const location = useLocation();

  if (initializing) return null;

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }

  if (requiredRole && !hasRole(requiredRole)) {
    return <Navigate to="/panel" replace />;
  }

  return children;
}
