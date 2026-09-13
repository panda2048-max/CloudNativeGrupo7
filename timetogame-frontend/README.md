# TimeToGame - Frontend

SPA en React (Vite) que consume la API REST del backend `timetogame-backend`.

## Requisitos

- Node.js 18+ y npm
- Backend corriendo (por defecto en `http://localhost:8000`)

## Configuracion

La URL del backend y los datos de Microsoft Entra ID (Azure AD) se leen de
variables de entorno, nunca estan hardcodeadas en el codigo. Copia
`.env.example` a `.env` y completa los valores:

```
VITE_API_BASE_URL=http://localhost:8000/api

# App registrations creados en https://entra.microsoft.com
VITE_AZURE_TENANT_ID=<TENANT_ID>
VITE_AZURE_CLIENT_ID_SPA=<CLIENT_ID_SPA>
VITE_AZURE_REDIRECT_URI=http://localhost:5173/callback
VITE_AZURE_POST_LOGOUT_REDIRECT_URI=http://localhost:5173/
VITE_AZURE_API_SCOPE=api://<CLIENT_ID_API>/access_as_user
```

`VITE_AZURE_TENANT_ID` y `VITE_AZURE_CLIENT_ID_SPA` salen del App
registration `timetogame-spa`; `VITE_AZURE_API_SCOPE` es el scope expuesto
por el App registration `timetogame-api` (menu "Expose an API"). Ver el
`README.md` de la raiz del repo para el detalle de como crear ambos.

Para apuntar a otro entorno (backend local en otro puerto, o un API
Manager/Gateway), solo hace falta cambiar `VITE_API_BASE_URL` y reiniciar
`npm run dev` (o reconstruir con `npm run build`). No requiere tocar codigo
fuente.

## Uso

```bash
npm install
npm run dev
```

La app queda disponible en `http://localhost:5173` (puerto configurado en
`vite.config.js`, y el mismo que el backend tiene habilitado en CORS).

## Usuarios de prueba

Se definen y se asignan roles en Microsoft Entra ID, no en este frontend ni
en el backend: crea usuarios en **Entra admin center > Users**, y asignales
el App role `USER` o `ADMIN` en **Enterprise applications > timetogame-api
> Users and groups**.

## Estructura

- `src/api`: cliente HTTP y funciones por recurso (`authApi`, `videojuegosApi`, `generosApi`).
- `src/auth`: integracion con MSAL (`msalConfig.js`), contexto de
  autenticacion (`AuthContext.jsx`) y decodificacion de roles del access
  token (`jwt.js`). El token vive en el cache interno de MSAL
  (`sessionStorage`), no se maneja manualmente.
- `src/components`: navegacion, rutas protegidas, mensajes de estado.
- `src/pages`: vistas (inicio, catalogo publico, detalle, login, panel autenticado, administracion).
