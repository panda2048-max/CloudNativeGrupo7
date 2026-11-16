# TimeToGame

Catalogo de videojuegos por genero: frontend SPA (React + MSAL),
microservicios Spring Boot validando JWT de Microsoft Entra ID, base de datos
MySQL y mensajeria asincrona con RabbitMQ (colas, exchanges, reintentos y DLQ).

```
timetogame-frontend/        SPA en React (Vite), login con MSAL
timetogame-backend/         API REST del catalogo (Resource Server OAuth2) +
                            productores/consumidores de notificaciones y fichas
timetogame-rabbitmq-admin/  Microservicio administrador de RabbitMQ (crear/
                            eliminar colas, exchanges y bindings) + auditoria
infra/rabbitmq/             docker-compose del cluster RabbitMQ de 2 nodos
infra/kafka/                docker-compose de Kafka: 3 Zookeeper + 3 brokers
infra/kafka-ui/             docker-compose de Kafka UI
infra/aws/                  runbook y scripts para recrear el despliegue en AWS
```

## Autenticacion (Microsoft Entra ID / Azure AD)

El IDaaS es Microsoft Entra ID. Se necesitan dos App registrations creados
en Azure

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

En local el frontend habla directo con los microservicios; en AWS pasa por
un API Gateway (HTTP API). Solo cambian `VITE_API_BASE_URL` y
`VITE_RABBIT_ADMIN_API_URL`:

```
Local                                  AWS
Frontend (React)                       Frontend (EC2, Nginx + HTTPS)
   │  localhost:8000 / :8001              │  https://<api-id>.execute-api.<region>.amazonaws.com
   ▼                                      ▼
Backend / Admin                        API Gateway + JWT authorizer (Entra ID)
                                          │  /api/admin/*  -> EC2 :8001 (admin)
                                          │  /api/public/* -> EC2 :8000 (sin authorizer)
                                          │  resto         -> EC2 :8000 (backend)
                                          ▼
                                       EC2 backend: backend, admin y cluster RabbitMQ (Docker)
```

Rutas del API Gateway: `GET|POST|PUT|DELETE|PATCH /{proxy+}` y
`GET|POST|DELETE /api/admin/{proxy+}` llevan el JWT authorizer `entra-id`
(identity source `$request.header.Authorization`, issuer
`https://login.microsoftonline.com/<TENANT_ID>/v2.0`, audience
`<CLIENT_ID_API>`); `GET /api/public/{proxy+}` y las rutas `OPTIONS` (preflight
CORS) no. Sin token o con un token invalido el Gateway responde 401
`{"message":"Unauthorized"}` sin llegar a la EC2. El CORS lo resuelve Spring
(el Gateway no tiene CORS propio).

Ademas, backend y admin validan cada request de forma independiente (firma,
issuer, audience, expiracion y rol contra Microsoft Entra ID) como OAuth2
Resource Server: el Gateway corta en el borde lo que no trae un JWT valido y
Spring Security autoriza por rol.

## Mensajeria asincrona (RabbitMQ)

Tres casos de uso, cada uno con su cola principal, reintento diferido o DLQ,
productor, consumidores con ACK manual y logs:

| Caso de uso | Microservicio | Exchange principal | Cola | DLQ |
|---|---|---|---|---|
| Notificaciones de catalogo | backend | `timetogame.catalogo.events` (topic) | `timetogame.catalogo.notificaciones` | `timetogame.catalogo.notificaciones.dlq` |
| Generacion de fichas tecnicas | backend | `timetogame.documentos` (direct) | `timetogame.documentos.fichas` | `timetogame.documentos.fichas.dlq` |
| Auditoria de administracion | admin | `timetogame.admin.auditoria` (topic) | `timetogame.admin.auditoria.registro` | `timetogame.admin.auditoria.dlq` |

Los nombres de colas, exchanges, routing keys y headers estan centralizados
en una sola clase por microservicio (`messaging.config.TopologiaMensajeria`
en el backend, `messaging.config.AuditoriaTopologia` en el admin). Los beans
`Queue`, `Exchange` y `Binding` se declaran en una clase `@Configuration`
por caso de uso (`NotificacionesMessagingConfig`,
`DocumentosMessagingConfig`, `AuditoriaMessagingConfig`). Los servicios de
negocio no conocen RabbitMQ: publican un evento de aplicacion de Spring y un
productor de la capa de mensajeria lo reenvia al broker. Los consumidores se
agrupan por dominio (`messaging.consumer.notificaciones`,
`.documentos`, `.auditoria`) y comparten la politica de errores de
`messaging.consumer.support` (`ReintentoDiferidoHandler`,
`RegistroDlqHandler`, `MensajeJsonReader`).

Todas las colas son **quorum**: en el cluster de 2 nodos quedan replicadas en
ambos (ver "Cluster RabbitMQ" mas abajo).

### Notificaciones de catalogo (`timetogame-backend`)

Cada alta/edicion/baja de un videojuego o genero genera una notificacion,
sin demorar ni arriesgar la operacion original:

```
VideojuegoService / GeneroService
   └─ publishEvent(CatalogoEvent)            (evento de Spring, sin RabbitMQ)
        └─ CatalogoEventProducer             (@TransactionalEventListener AFTER_COMMIT)
             └─(catalogo.<entidad>.<tipo>)─> [topic] timetogame.catalogo.events
                  └─(catalogo.#)─> [quorum] timetogame.catalogo.notificaciones
                                        └─> NotificacionCatalogoConsumer -> tabla notificaciones

Error recuperable:  consumer ─> [direct] timetogame.catalogo.retry
                                  └─> [quorum] ...notificaciones.retry (TTL 5s, sin consumidores)
                                        └─ al expirar vuelve a la cola principal (max 3 veces)
Error no recuperable o reintentos agotados (basicNack sin requeue):
    [quorum] ...notificaciones ─(DLX)─> [direct] timetogame.catalogo.dlx
         └─> [quorum] timetogame.catalogo.notificaciones.dlq ─> NotificacionDlqConsumer
                                                  └─ log ERROR + tabla mensajes_fallidos
```

- Se publica **despues del commit**: si la transaccion hace rollback, no se
  anuncia un cambio que no ocurrio.
- Si el broker esta caido, el CRUD del catalogo sigue funcionando: el error de
  publicacion se registra en el log y no se propaga al usuario.
- El consumidor es **idempotente** (`event_id` unico): RabbitMQ entrega "al
  menos una vez", asi que una entrega duplicada se confirma sin reprocesar.

### Generacion de fichas tecnicas (`timetogame-backend`)

Tarea asincrona tipo "generacion de documentos": la ficha tecnica de cada
videojuego se redacta en segundo plano y se guarda en `fichas_videojuego`.

```
Origen 1: alta/edicion/baja de un videojuego (CatalogoEvent)
Origen 2: POST /api/videojuegos/{id}/ficha  -> 202 Accepted (SolicitudFicha)
   └─ SolicitudFichaProducer                 (@TransactionalEventListener AFTER_COMMIT)
        └─(ficha.generar | ficha.eliminar)─> [direct] timetogame.documentos
             └─> [quorum] timetogame.documentos.fichas ─> FichaVideojuegoConsumer
Reintento:  ─> [direct] timetogame.documentos.retry ─> [quorum] ...fichas.retry (TTL 5s)
DLQ:        ─(DLX)─> [direct] timetogame.documentos.dlx ─> [quorum] timetogame.documentos.fichas.dlq
                                                             └─> FichaDlqConsumer
```

- Exchange **direct**: cada accion tiene su routing key exacta y ambas se
  enlazan a la misma cola de trabajo.
- Videojuego inexistente o mensaje invalido -> DLQ sin reintentar. Error de
  BD o conflicto de concurrencia -> reintento diferido (3 x 5 s) y luego DLQ.
- Idempotente: la ficha guarda el id de la ultima solicitud aplicada; una
  entrega duplicada se confirma sin regenerar.
- `GET /api/videojuegos/{id}/ficha` la devuelve (404 mientras no exista). En
  el frontend: **Administracion > Videojuegos > Ficha**.

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
`timetogame.documentos.*`, `timetogame.admin.*`) y los recursos `amq.*` estan
protegidos contra eliminacion (`RABBIT_ADMIN_RECURSOS_PROTEGIDOS`).

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
4. **Fichas**: en Administracion > Videojuegos > *Ficha* > *Generar ficha*
   (202 y luego aparece la ficha). Para la DLQ, publicar en
   `timetogame.documentos` con routing key `ficha.generar` una solicitud con
   un `videojuegoId` inexistente.

### Pruebas

```bash
# backend: politica de ack/nack/reintento, lectura de mensajes y fichas (sin broker)
cd timetogame-backend && ./mvnw test

# admin: endpoints, validaciones y seguridad por rol (sin broker)
cd timetogame-rabbitmq-admin && ./mvnw test -Dtest=AdminControllersTest

# admin contra un RabbitMQ real en localhost (opt-in)
cd timetogame-rabbitmq-admin && ./mvnw test -Dtest=RabbitAdminServiceBrokerIT -Drabbit.it=true
```

### Cluster RabbitMQ (`infra/rabbitmq`, docker-compose)

En AWS el broker corre como un cluster de **2 nodos** (`rabbitmq-1`,
`rabbitmq-2`) levantado con docker-compose en la EC2 del backend:

```bash
cd infra/rabbitmq
cp .env.example .env            # Erlang cookie y usuario administrador
docker compose up -d            # rabbitmq-2 arranca cuando rabbitmq-1 esta sano y se une al cluster
docker compose exec rabbitmq-1 rabbitmqctl cluster_status
echo -n "<pass>" | ./crear-usuario.sh timetogame-backend
echo -n "<pass>" | ./crear-usuario.sh timetogame-admin management
```

- El cluster se forma solo (`cluster_formation.peer_discovery_backend =
  classic_config`); ambos nodos comparten la Erlang cookie.
- Puertos publicados solo en `127.0.0.1`: nodo 1 en 5672/15672, nodo 2 en
  5673/15673. Backend y admin usan `RABBITMQ_ADDRESSES=localhost:5672,localhost:5673`
  (si un nodo cae, el cliente reconecta al otro) y el admin consulta la API de
  management de cualquiera de los dos (`RABBITMQ_MANAGEMENT_URLS`).
- Las colas quorum se replican en ambos nodos. Con 2 nodos la mayoria es 2 de
  2: hay replicacion pero no tolerancia a la caida de un nodo (para eso se
  necesita un tercer nodo).
- La UI de management se ve con un tunel SSH:
  `ssh -i <llave>.pem -L 15672:localhost:15672 ubuntu@<ip-backend>` y luego
  `http://localhost:15672` (pestaña *Overview* > *Nodes* muestra los dos).

## Streaming de eventos (Kafka)

Kafka se agrega **al lado** de RabbitMQ, sin reemplazarlo: RabbitMQ sigue con
las tareas (notificaciones, fichas, auditoria) y Kafka lleva los **flujos de
eventos** que varios consumidores leen a su ritmo. El caso de uso es
**Tendencias en vivo**: cada ficha que se abre es un evento; un consumidor
cuenta las vistas en una ventana deslizante de 10 minutos y el ranking llega
al navegador por **Server-Sent Events**, sin recargar ni consultar cada X
segundos.

```
GET /api/public/videojuegos/{id} ─> ActividadService ─ publishEvent ─> ActividadProducer (@Async)
   ─(VideojuegoVistoEvent, key=videojuegoId)─> [timetogame.actividad]  3 particiones x 3 replicas
        ├─ group "tendencias" (3 hilos, 1 por particion) ─> TendenciasService ─> SSE /api/public/stream/tendencias
        └─ group "historial"                             ─> HistorialService  ─> tabla historial_vistas

Alta/edicion/baja de videojuego ─ CatalogoEvent (el mismo de RabbitMQ) ─> CatalogoCambioProducer (AFTER_COMMIT)
   ─(CatalogoCambioEvent, key=videojuegoId)─> [timetogame.catalogo.cambios]  compactado, 3 x 3
        ├─ group "tendencias"    ─> actualiza titulos / saca del ranking al eliminado
        └─ group "catalogo-live" ─> SSE: aviso en vivo "X se actualizo"

Error no recuperable o 3 reintentos (1 s, 2 s, 4 s) agotados ─> <topico>.DLT (misma particion)
```

### Topicos y por que cada parametro

| Topico | Key | Particiones / replicas | Limpieza y retencion | Por que |
|---|---|---|---|---|
| `timetogame.actividad` | videojuegoId | 3 / 3, `min.insync.replicas=2` | `delete`, 7 dias | Flujo de eventos: lo que paso. 7 dias permiten reprocesar o sumar un consumidor nuevo desde el inicio |
| `timetogame.catalogo.cambios` | videojuegoId | 3 / 3, `min.insync.replicas=2` | `compact` | Estado: Kafka conserva al menos el ultimo evento de cada juego; un consumidor nuevo reconstruye el catalogo |
| `*.DLT` | la original | 3 / 3 | `delete`, 14 dias | Mensajes que no se pudieron procesar, con headers de la causa |

- **Particiones = paralelismo**: el grupo `tendencias` corre 3 hilos y Kafka
  le da una particion a cada uno. Con la key = videojuegoId, todas las vistas
  de un juego caen en la misma particion y se procesan en orden.
- **Replicas = disponibilidad**: cada particion vive en los 3 brokers. Si cae
  uno, otra replica del ISR pasa a ser lider. Con `acks=all` y
  `min.insync.replicas=2`, una escritura confirmada ya esta en 2 brokers.
- **Consumer groups**: `tendencias` e `historial` leen el mismo topico y cada
  uno recibe **todos** los eventos (offsets propios). Dentro de un grupo, las
  particiones se reparten: con mas hilos o instancias se procesa mas rapido
  (hasta 3, una por particion).
- **Lag**: si el lag del grupo crece sostenidamente, el consumidor no da
  abasto y hay que escalar. `POST /api/streaming/simulacion?vistas=N` (ADMIN)
  genera carga; con `TENDENCIAS_COSTO_PROCESAMIENTO=20ms` el lag se ve subir
  y bajar en Kafka UI o en Mensajeria > Kafka.

### Codigo (backend, paquete `streaming`)

| Pieza | Clase |
|---|---|
| Nombres de topicos y grupos | `config.TopologiaKafka` |
| Topicos en codigo (`NewTopic`/`TopicBuilder`) | `config.KafkaTopicConfig` |
| Errores: `DefaultErrorHandler` + backoff exponencial + `DeadLetterPublishingRecoverer` | `config.KafkaConfig` |
| Cliente (bootstrap, serializers, `acks=all`, `retries`, idempotencia, `group-id` por listener) | `application.properties` (`spring.kafka.*`) |
| Eventos tipados (DTO) | `event.VideojuegoVistoEvent`, `event.CatalogoCambioEvent` |
| Productores (`KafkaTemplate`, `JacksonJsonSerializer`) | `producer.ActividadProducer`, `producer.CatalogoCambioProducer` |
| Consumidores (`@KafkaListener` que delega en un service) | `consumer.tendencias`, `consumer.historial`, `consumer.catalogo` |
| SSE | `sse.StreamHub`, `controller.StreamController` |

Un JSON invalido no bota al consumidor: `ErrorHandlingDeserializer` lo
convierte en error, `DefaultErrorHandler` no lo reintenta (tampoco un evento
que no pasa la validacion del DTO) y lo publica en el DLT; el consumidor
sigue con el siguiente offset.

### Administracion (microservicio admin)

| Metodo y ruta | Que hace |
|---|---|
| `GET /api/admin/kafka/cluster` | Brokers y controlador |
| `GET /api/admin/kafka/topics` | Topicos con particiones y replicas |
| `GET /api/admin/kafka/topics/{nombre}` | Por particion: lider, replicas, ISR; configuracion |
| `POST /api/admin/kafka/topics` | Crear (nombre, particiones, replicas, politica, retencion, min.insync) |
| `PATCH /api/admin/kafka/topics/{nombre}` | Aumentar particiones, cambiar retencion, cleanup.policy o min.insync |
| `DELETE /api/admin/kafka/topics/{nombre}` | Eliminar (los `timetogame.*` e internos estan protegidos: 409) |
| `GET /api/admin/kafka/consumer-groups` | Grupos y estado |
| `GET /api/admin/kafka/consumer-groups/{id}` | Miembros, particiones asignadas, offsets y lag |
| `DELETE /api/admin/kafka/consumer-groups/{id}` | Eliminar un grupo sin consumidores activos |

Todo con `AdminClient` dentro de `KafkaAdminService`. En el frontend:
**Mensajeria > Kafka** (con refresco automatico del lag).

### Infraestructura

- `infra/kafka/docker-compose.yml`: 3 Zookeeper (ensemble, mayoria 2 de 3) +
  3 brokers (Confluent 7.7), replicacion 3 y `min.insync.replicas=2` por
  defecto, `unclean.leader.election` desactivado, creacion automatica de
  topicos desactivada.
- `infra/kafka-ui/docker-compose.yml`: Kafka UI en `127.0.0.1:8080` (tunel SSH).
- En AWS corre en una EC2 aparte (`infra/aws/kafka/setup-kafka.sh`); backend
  y admin usan `KAFKA_BOOTSTRAP_SERVERS=<ip-privada>:9092,9093,9094`.
- En local sin Docker se puede usar un Kafka 3.9 nativo con 1 broker y
  `KAFKA_TOPICOS_REPLICAS=1`.

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
| Conexion a RabbitMQ (default localhost:5672 / guest) | Backend y admin | `RABBITMQ_ADDRESSES` (nodos separados por coma), `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `RABBITMQ_VHOST` |
| Reintentos de notificaciones (default 3 y 5s) | Backend | `NOTIFICACIONES_MAX_REINTENTOS`, `NOTIFICACIONES_RETRASO_REINTENTO` |
| Reintentos de fichas (default 3 y 5s) | Backend | `DOCUMENTOS_MAX_REINTENTOS`, `DOCUMENTOS_RETRASO_REINTENTO` |
| API HTTP de management de RabbitMQ (una URL por nodo) | Admin | `RABBITMQ_MANAGEMENT_URLS` |
| Brokers de Kafka (separados por coma) | Backend y admin | `KAFKA_BOOTSTRAP_SERVERS` |
| Particiones y replicas de los topicos (default 3 y 3; en local con 1 broker, replicas 1) | Backend | `KAFKA_TOPICOS_PARTICIONES`, `KAFKA_TOPICOS_REPLICAS` |
| Ventana, hilos y costo artificial de tendencias (default 10m, 3, 0ms) | Backend | `TENDENCIAS_VENTANA`, `TENDENCIAS_CONSUMIDORES`, `TENDENCIAS_COSTO_PROCESAMIENTO` |
| URL del stream SSE de tendencias | Frontend | `VITE_STREAM_URL` |
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
3. **Kafka** (con Docker: `infra/kafka` con `KAFKA_HOST=localhost`). En
   Windows sin Docker, 1 Zookeeper + 1 broker nativos:
   - Descargar `kafka_2.13-3.9.1.tgz` (https://archive.apache.org/dist/kafka/3.9.1/)
     y un JRE 21 (Kafka 3.9 no funciona bien con Java 25).
   - En `config/server.properties`: `listeners=PLAINTEXT://localhost:9092`,
     `auto.create.topics.enable=false` y `log.dirs` en una ruta corta.
   - Con `JAVA_HOME=<jre21>` y `KAFKA_HEAP_OPTS=-Xmx512m` (sin esta variable el
     script usa `wmic`, que ya no existe en Windows 11):
     `bin\windows\zookeeper-server-start.bat config\zookeeper.properties` y
     `bin\windows\kafka-server-start.bat config\server.properties`.
   - En `application-local.properties` del backend: `KAFKA_TOPICOS_REPLICAS=1`.
4. `timetogame-backend/` - `./mvnw spring-boot:run` (puerto 8000). Declara su
   topologia en RabbitMQ y crea sus topicos en Kafka al conectar.
5. `timetogame-rabbitmq-admin/` - `./mvnw spring-boot:run` (puerto 8001).
6. `timetogame-frontend/` - `npm install && npm run dev` (puerto 5173).

Si el broker se cae, ambos microservicios siguen respondiendo y se reconectan
solos (y re-declaran su topologia) cuando vuelve.

Si el broker local tiene colas de una version anterior declaradas como
`classic`, el arranque falla con `PRECONDITION_FAILED - inequivalent arg
'x-queue-type'`: borrarlas (vacias) desde la UI de management y volver a
arrancar, los microservicios las declaran de nuevo como quorum.
