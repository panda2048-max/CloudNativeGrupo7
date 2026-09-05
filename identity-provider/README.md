# Identity Provider (Keycloak)

TimeToGame delega toda la autenticacion a Keycloak via OAuth 2.0 / OpenID
Connect. Ni el backend ni el frontend manejan contrasenas: el backend solo
valida tokens (Resource Server), y el frontend solo redirige al IdP
(Authorization Code + PKCE).

## Levantar Keycloak localmente (sin Docker)

1. Descargar la distribucion standalone (Quarkus) desde
   https://github.com/keycloak/keycloak/releases - version usada en este
   proyecto: 26.7.3. Descomprimir donde sea (no se versiona en este repo).
2. Arrancar en modo desarrollo:

   ```bash
   export KC_BOOTSTRAP_ADMIN_USERNAME=admin
   export KC_BOOTSTRAP_ADMIN_PASSWORD=admin123
   bin/kc.sh start-dev --http-port=8080
   ```

   (`bin/kc.bat` en Windows). Requiere JDK 17+.

3. Desde la carpeta de la distribucion, correr el script de este repo para
   crear el realm, roles, cliente SPA y usuarios de prueba:

   ```bash
   bash /ruta/a/este/repo/identity-provider/setup-keycloak.sh
   ```

## Que configura el script

- **Realm** `timetogame`.
- **Roles** de realm `USER` (lectura autenticada) y `ADMIN` (alta, edicion,
  borrado) - los dos niveles de acceso que exige el dominio.
- **Cliente** `timetogame-spa`: publico (sin `client_secret` - una SPA
  nunca deberia tenerlo), `standardFlowEnabled=true` con PKCE (S256)
  obligatorio, `directAccessGrantsEnabled=false` (no se acepta
  usuario/contrasena directo, solo Authorization Code).
- Un **protocol mapper de audience** que agrega `timetogame-api` al claim
  `aud` del access token, para que el backend pueda exigir esa audiencia
  especifica.
- Dos **usuarios de demo** (`user`/`user123` con rol USER, `admin`/`admin123`
  con USER+ADMIN). No son secretos reales: son datos de prueba de un
  Keycloak que solo corre en `localhost` para desarrollo/evaluacion, igual
  que cualquier tutorial que usa `admin/admin`. Ningun ambiente real
  deberia reutilizar estas cuentas.

## Como validan backend y frontend

- **Backend** (`timetogame-backend`): Spring Security como OAuth2 Resource
  Server (`spring-boot-starter-oauth2-resource-server`). Valida contra
  `spring.security.oauth2.resourceserver.jwt.issuer-uri` (firma via JWKS
  del realm, `iss`, expiracion) y agrega un validador propio de `aud`
  (`app.oidc.audience`). Los roles de realm (`realm_access.roles`) se
  mapean a `ROLE_*` de Spring Security en
  `security/KeycloakRealmRoleConverter.java`.
- **Frontend** (`timetogame-frontend`): `oidc-client-ts` maneja el flujo
  Authorization Code + PKCE completo (`src/auth/oidcUserManager.js`). El
  `ID Token` solo identifica al usuario en la UI; el `Access Token` es lo
  unico que se envia como `Authorization: Bearer` al backend
  (`src/api/httpClient.js`).

## Escenarios verificados

- Sin token -> `401` (incluye token con firma alterada).
- Token valido, rol insuficiente (`USER` en una operacion que exige
  `ADMIN`) -> `403`.
- Token valido, rol correcto -> `200`/`201`/`204` segun la operacion.
