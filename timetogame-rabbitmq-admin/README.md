# TimeToGame - RabbitMQ Admin

Microservicio administrador de la topologia de RabbitMQ: crea y elimina
colas, exchanges y bindings mediante una API REST protegida (JWT de Microsoft
Entra ID con rol `ADMIN`), y registra cada cambio de forma asincrona en un log
de auditoria.

Ver el `README.md` de la raiz para la tabla de endpoints, validaciones y la
politica de mensajeria comun a todo el sistema.

## Estructura

- `controller/`: `QueueAdminController`, `ExchangeAdminController`,
  `BindingAdminController`. Solo traducen HTTP <-> `RabbitAdminService`.
- `service/`: `RabbitAdminService` y su implementacion, unica clase que habla
  con el broker (escrituras con `AmqpAdmin`, lecturas con la API HTTP de
  management a traves de `client/RabbitManagementClient`).
- `dto/`: requests con Bean Validation (`CrearColaRequest`,
  `CrearExchangeRequest`, `CrearBindingRequest`) y responses.
- `messaging/`: auditoria asincrona. `config/` declara los beans de la
  topologia, `producer/` publica cada operacion y `consumer/auditoria/` la
  registra (con DLQ para mensajes invalidos).
- `security/`: Resource Server con la misma validacion que el backend.

## Uso

```bash
cp src/main/resources/application-local.properties.example src/main/resources/application-local.properties
# completar OIDC_ISSUER_URI y OIDC_AUDIENCE
./mvnw spring-boot:run          # puerto 8001
```

Requiere RabbitMQ con el plugin `rabbitmq_management` habilitado (puerto 15672
para la API HTTP). El registro de auditoria queda en `logs/auditoria-topologia.log`
(`LOG_DIR` para cambiar la carpeta).

## Pruebas

```bash
./mvnw test                                                    # endpoints, validacion y seguridad
./mvnw test -Dtest=RabbitAdminServiceBrokerIT -Drabbit.it=true # contra un RabbitMQ real en localhost
```
