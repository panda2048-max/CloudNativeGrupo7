// Decodifica el payload de un JWT solo para lectura en la UI (roles,
// usuario). No es una validacion de firma: la unica validacion que importa
// (firma, issuer, audience, expiracion) la hace el backend como Resource
// Server. Aqui es unicamente para decidir que mostrar en pantalla.
export function decodeJwtPayload(token) {
  try {
    const base64Url = token.split(".")[1];
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const json = decodeURIComponent(
      atob(base64)
        .split("")
        .map((c) => "%" + c.charCodeAt(0).toString(16).padStart(2, "0"))
        .join("")
    );
    return JSON.parse(json);
  } catch {
    return null;
  }
}

export function extractRealmRoles(accessToken) {
  const payload = decodeJwtPayload(accessToken);
  return payload?.realm_access?.roles ?? [];
}
