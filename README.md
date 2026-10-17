# TimeToGame

Catalogo de videojuegos por genero: frontend SPA (React + MSAL),
microservicios Spring Boot validando JWT de Microsoft Entra ID, base de datos
MySQL y mensajeria asincrona con RabbitMQ (colas, exchanges, reintentos y DLQ).

```
timetogame-frontend/        SPA en React (Vite), login con MSAL
timetogame-backend/         API REST del catalogo (Resource Server OAuth2) +
                            productor/consumidores de notificaciones
timetogame-rabbitmq-admin/  Microservicio administrador de RabbitMQ (crear/
                            eliminar colas, exchanges y bindings) + auditoria
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

## Mensajeria asincrona (RabbitMQ)

Cada microservicio tiene su propio dominio de mensajeria, con productor,
consumidores con ACK manual, DLQ y logs. La configuracion (beans `Queue`,
`Exchange`, `Binding`) vive en clases `@Configuration` del paquete
`messaging.config`; los servicios de negocio no conocen RabbitMQ: publican un
evento de aplicacion de Spring y un productor de la capa de mensajeria lo
reenvia al broker.

### Notificaciones de catalogo (`timetogame-backend`)

Cada alta/edicion/baja de un videojuego o genero genera una notificacion,
sin demorar ni arriesgar la operacion original:

```
VideojuegoService / GeneroService
   └─ publishEvent(CatalogoEvent)            (evento de Spring, sin RabbitMQ)
        └─ CatalogoEventProducer             (@TransactionalEventListener AFTER_COMMIT)
             └─(catalogo.<entidad>.<tipo>)─> [topic] timetogame.catalogo.events
                  └─(catalogo.#)─> [queue] timetogame.catalogo.notificaciones
                                        └─> NotificacionCatalogoConsumer -> tabla notificaciones

Error recuperable:  consumer ─> [direct] timetogame.catalogo.retry
                                  └─> [queue] ...notificaciones.retry (TTL 5s, sin consumidores)
                                        └─ al expirar vuelve a la cola principal (max 3 veces)
Error no recuperable o reintentos agotados (basicNack sin requeue):
    [queue] ...notificaciones ─(DLX)─> [direct] timetogame.catalogo.dlx
         └─> [queue] timetogame.catalogo.notificaciones.dlq ─> NotificacionDlqConsumer
                                                  └─ log ERROR + tabla mensajes_fallidos
```

- Se publica **despues del commit**: si la transaccion hace rollback, no se
  anuncia un cambio que no ocurrio.
- Si el broker esta caido, el CRUD del catalogo sigue funcionando: el error de
  publicacion se registra en el log y no se propaga al usuario.
- El consumidor es **idempotente** (`event_id` unico): RabbitMQ entrega "al
  menos una vez", asi que una entrega duplicada se confirma sin reprocesar.

### Auditoria de administracion (`timetogame-rabbitmq-admin`)

Cada cambio de topologia hecho por la API del administrador se publica en
`[topic] timetogame.admin.auditoria` y lo consume `AuditoriaConsumer`, que lo
escribe en `logs/auditoria-topologia.log`. Los mensajes invalidos van a
`timetogame.admin.auditoria.dlq` y `AuditoriaDlqConsumer` los deja en el log.

### Politica de confirmacion (ACK manual)

| Resultado del consumidor | Accion | Destino |
|---|---|---|
| Procesado correctamente | `basicAck` | - |
| Entrega duplicada (ya procesado) | `basicAck` | - |
| Mensaje invalido (JSON mal formado, campos faltantes) | `basicNack(requeue=false)` | DLQ inmediata: reintentar no lo arregla |
| Error recuperable (BD caida, timeout) con reintentos disponibles | copia a la cola de reintento + `basicAck` | vuelve tras el TTL |
| Error recuperable con reintentos agotados | `basicNack(requeue=false)` | DLQ |

Nunca se usa `requeue=true` en la cola principal: reencolar al instante
produce un bucle caliente mientras la causa siga presente. Los consumidores
reciben el cuerpo crudo y deserializan dentro de su propio `try/catch`; si lo
hiciera el contenedor, un JSON invalido fallaria antes de llegar al listener
y en la DLQ el manejador por defecto de Spring lo descartaria en silencio.

### Microservicio administrador (`timetogame-rabbitmq-admin`, puerto 8001)

Todas las rutas exigen JWT de Entra ID con rol `ADMIN`. La logica vive en
`RabbitAdminService` (escrituras via `AmqpAdmin`, lecturas via la API HTTP de
management, la unica que permite listar); los controladores solo llaman a ese
servicio.

| Metodo y ruta | Respuestas |
|---|---|
| `GET /api/admin/queues` | 200 |
| `GET /api/admin/queues/{nombre}` | 200, 404 |
| `POST /api/admin/queues` | 201 + `Location`, 400, 409 |
| `DELETE /api/admin/queues/{nombre}?soloSiSinConsumidores=&soloSiVacia=` | 204, 404, 409 |
| `GET /api/admin/exchanges` | 200 |
| `GET /api/admin/exchanges/{nombre}` | 200, 404 |
| `POST /api/admin/exchanges` | 201 + `Location`, 400, 409 |
| `DELETE /api/admin/exchanges/{nombre}?soloSiSinBindings=` | 204, 404, 409 |
| `GET /api/admin/bindings` | 200 |
| `POST /api/admin/bindings` | 201, 400, 404, 409 |
| `DELETE /api/admin/bindings/{exchange}/{cola}?routingKey=` | 204, 404, 409 |

Ejemplo de cuerpo para crear una cola:

```json
{ "nombre": "pedidos", "tipo": "CLASSIC", "durable": true,
  "deadLetterExchange": "pedidos.dlx", "ttlMensajesMs": 60000, "maxMensajes": 1000 }
```

Validaciones (DTOs con Bean Validation + reglas contra el estado real del
broker): nombres no vacios, solo caracteres seguros, sin el prefijo reservado
`amq.`; TTL y maximo de mensajes en rango; una cola `QUORUM` debe ser durable
y no auto-delete; `deadLetterRoutingKey` exige `deadLetterExchange` y este
debe existir; comodines `*`/`#` rechazados en exchanges `direct`; duplicados
-> 409. La topologia del sistema (`timetogame.catalogo.*`,
`timetogame.admin.*`) y los recursos `amq.*` estan protegidos contra
eliminacion (`RABBIT_ADMIN_RECURSOS_PROTEGIDOS`).

En el frontend, la pagina **Mensajeria** (solo ADMIN) usa esta API y muestra
ademas los mensajes fallidos de la DLQ; el **Panel** muestra las
notificaciones.

### Como demostrar cada flujo

Con todo levantado, desde la UI de management (`http://localhost:15672`,
guest/guest) > Exchanges > `timetogame.catalogo.events` > *Publish message*:

1. **Flujo normal**: crear/editar un videojuego desde la app y pulsar
   *Actualizar* en las notificaciones del Panel.
2. **No recuperable -> DLQ**: publicar con routing key
   `catalogo.genero.creado` el payload `{esto no es json`. El log del backend
   muestra el `basicNack` y el `[DLQ]`, y el mensaje aparece en Mensajeria >
   Mensajes fallidos con motivo `rejected` y 0 reintentos.
3. **Recuperable -> reintentos -> DLQ**: detener MySQL (o renombrar la tabla
   `notificaciones`), publicar un evento valido y observar en el log los
   reintentos 1/3, 2/3, 3/3 cada 5 s y el envio final a la DLQ. Si la BD
   vuelve dentro de esa ventana, el reintento lo procesa sin pasar por la DLQ.

### Pruebas

```bash
# backend: politica de ack/nack/reintento y lectura de mensajes (sin broker)
cd timetogame-backend && ./mvnw test -Dtest='*Consumer*Test,*Reader*Test,*Handler*Test'

# admin: endpoints, validaciones y seguridad por rol (sin broker)
cd timetogame-rabbitmq-admin && ./mvnw test -Dtest=AdminControllersTest

# admin contra un RabbitMQ real en localhost (opt-in)
cd timetogame-rabbitmq-admin && ./mvnw test -Dtest=RabbitAdminServiceBrokerIT -Drabbit.it=true
```

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
| Origen permitido por CORS | Backend y admin | `CORS_ALLOWED_ORIGIN` |
| Datos de conexion a la base de datos | Backend | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` |
| URL del microservicio administrador | Frontend | `VITE_RABBIT_ADMIN_API_URL` |
| Conexion a RabbitMQ (default localhost / guest) | Backend y admin | `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `RABBITMQ_VHOST` |
| Reintentos de notificaciones (default 3 y 5s) | Backend | `NOTIFICACIONES_MAX_REINTENTOS`, `NOTIFICACIONES_RETRASO_REINTENTO` |
| API HTTP de management de RabbitMQ | Admin | `RABBITMQ_MANAGEMENT_URL` |
| Prefijos protegidos contra eliminacion | Admin | `RABBIT_ADMIN_RECURSOS_PROTEGIDOS` |

Los archivos con valores locales (`.env` en el frontend,
`application-local.properties` en backend y admin) estan gitignorados; cada
carpeta trae su `.example` como plantilla. `.env.production` si se versiona:
solo tiene URLs publicas e IDs de la SPA, y `npm run build` lo aplica solo
por encima de `.env` (no hay que copiarlo).

## Levantar todo en local

1. Crear los App registrations en Microsoft Entra ID (seccion Autenticacion
   mas arriba) y completar `application-local.properties` (backend y admin) y
   `.env` con los IDs reales.
2. **RabbitMQ** (4.3.x, requiere Erlang 27-28). En Windows sin Docker:
   - `winget install --id Erlang.ErlangOTP --version 28.5.0.6 --exact`
   - Descargar y descomprimir `rabbitmq-server-windows-4.3.6.zip` desde
     https://github.com/rabbitmq/rabbitmq-server/releases
   - Con `ERLANG_HOME=C:\Program Files\Erlang OTP`:
     `sbin\rabbitmq-plugins.bat enable --offline rabbitmq_management` y luego
     `sbin\rabbitmq-server.bat`.
   - AMQP en `5672`, UI de management en `http://localhost:15672` (guest/guest).
3. `timetogame-backend/` - `./mvnw spring-boot:run` (puerto 8000). Declara su
   topologia en el broker al conectar.
4. `timetogame-rabbitmq-admin/` - `./mvnw spring-boot:run` (puerto 8001).
5. `timetogame-frontend/` - `npm install && npm run dev` (puerto 5173).

Si el broker se cae, ambos microservicios siguen respondiendo y se reconectan
solos (y re-declaran su topologia) cuando vuelve.
