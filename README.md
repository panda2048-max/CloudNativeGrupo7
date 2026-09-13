# TimeToGame

Proyecto para Evaluacion 1: catalogo de videojuegos por genero, backend
monolito (Spring Boot) + frontend SPA (React) + Identity Provider (Microsoft
Entra ID / Azure AD).

```
timetogame-frontend/   SPA en React (Vite), login con MSAL
timetogame-backend/    API REST en Spring Boot (Resource Server OAuth2)
identity-provider/     Setup de Keycloak usado en una iteracion anterior;
                        superado por Microsoft Entra ID (ver seccion
                        Autenticacion mas abajo). Se conserva como referencia.
```

## Autenticacion (Microsoft Entra ID / Azure AD)

El IDaaS es Microsoft Entra ID. Se necesitan dos App registrations creados
en https://entra.microsoft.com:

1. **`timetogame-api`** (el backend, como recurso protegido):
   - "Expose an API" -> setear el Application ID URI (default `api://<client-id>`)
     -> agregar el scope `access_as_user`.
   - "App roles" -> crear `ADMIN` y `USER` (allowed member types: Users/Groups).
   - **Manifest** -> `api.requestedAccessTokenVersion` debe quedar en `2`.
     Si queda en `null`/`1`, Azure emite tokens v1.0 (`iss` =
     `https://sts.windows.net/<tenant>/`) que no coinciden con el
     `issuer-uri` v2.0 que usa el backend, y todo falla con 401.
   - Anotar el **Application (client) ID** y el **Directory (tenant) ID**.
2. **`timetogame-spa`** (el frontend, cliente publico):
   - Platform: **Single-page application**, redirect URI
     `http://localhost:5173/callback` (agregar tambien la URL de produccion
     cuando exista).
   - "API permissions" -> agregar el scope `access_as_user` de `timetogame-api`
     -> **Grant admin consent for `<tenant>`** (sin esto, el login falla con
     `AADSTS65001` pidiendo aprobacion de un admin).

Los App roles no se autoasignan: en **Enterprise applications >
timetogame-api > Users and groups** hay que asignar `ADMIN`/`USER` a cada
usuario de prueba. Y los usuarios deben pertenecer a este mismo tenant (un
`@outlook.com`/`@hotmail.com` personal no sirve salvo que se invite como
guest).

Con esos IDs, completar:
- `timetogame-frontend/.env` (ver `timetogame-frontend/README.md`).
- `timetogame-backend/src/main/resources/application-local.properties`:
  - `OIDC_ISSUER_URI=https://login.microsoftonline.com/<TENANT_ID>/v2.0`
  - `OIDC_AUDIENCE=<CLIENT_ID_API>` (el **Client ID puro, sin el prefijo
    `api://`**: para un scope personalizado, asi es como Azure AD llena el
    claim `aud` en tokens v2.0, aunque el Application ID URI expuesto sea
    `api://<client-id>`).

Cada carpeta tiene su propio README con el detalle de esa capa. Este archivo
cubre como encajan entre si y como se configuran por ambiente.

## Arquitectura

Hoy el frontend habla directo con el backend; la variable de configuracion
que separa ambas capas (`API_BASE_URL`) es lo unico que hay que cambiar
para meter un API Manager/Gateway en el medio sin tocar el codigo:

```
Frontend (React)                       Frontend (React)
   │  VITE_API_BASE_URL                    │  VITE_API_BASE_URL
   ▼  = http://localhost:8000/api          ▼  = https://gateway.dominio/timetogame
Backend (Spring Boot)                  API Manager / Gateway
                                            │  (routing, politicas, CORS,
                                            │   observabilidad, etc.)
                                            ▼
                                        Backend (Spring Boot)
```

El backend valida cada request de forma independiente (firma, issuer,
audience, expiracion y rol/scope contra Microsoft Entra ID) como OAuth2
Resource Server: pasar por un gateway no reemplaza esa autorizacion, solo se
le suman responsabilidades de borde (routing, politicas, observabilidad...)
encima.

## Configuracion por ambiente

Nada de esto esta hardcodeado en logica de negocio: todo se lee de
variables de entorno, con defaults de conveniencia solo para desarrollo
local.

| Valor | Donde vive | Variable |
|---|---|---|
| URL del backend (o del gateway, cuando exista) | Frontend | `VITE_API_BASE_URL` |
| Tenant ID de Entra ID | Frontend | `VITE_AZURE_TENANT_ID` |
| Client ID del App registration `timetogame-spa` | Frontend | `VITE_AZURE_CLIENT_ID_SPA` |
| Scope del App registration `timetogame-api` | Frontend | `VITE_AZURE_API_SCOPE` |
| Redirect URI (Authorization Code) | Frontend | `VITE_AZURE_REDIRECT_URI` |
| Post-logout redirect URI | Frontend | `VITE_AZURE_POST_LOGOUT_REDIRECT_URI` |
| Issuer de Entra ID (para validar tokens) | Backend | `OIDC_ISSUER_URI` |
| Audience esperada en el access token (Client ID de `timetogame-api`, sin `api://`) | Backend | `OIDC_AUDIENCE` |
| Origen permitido por CORS | Backend | `CORS_ALLOWED_ORIGIN` |
| Datos de conexion a la base de datos | Backend | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` |

Ninguno de estos archivos con valores reales se versiona (`.env` en el
frontend, `application-local.properties` en el backend estan
gitignorados); cada carpeta trae un `.env.example` /
`application-local.properties.example` como plantilla.

## Levantar todo en local

1. Crear los App registrations en Microsoft Entra ID (seccion Autenticacion
   mas arriba) y completar `application-local.properties` / `.env` con los
   IDs reales.
2. `timetogame-backend/` - `./mvnw spring-boot:run` (puerto 8000).
3. `timetogame-frontend/` - `npm install && npm run dev` (puerto 5173).
