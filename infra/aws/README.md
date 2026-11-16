# Despliegue en AWS (respaldo y runbook)

Inventario de lo que corre hoy en AWS y pasos para recrearlo desde cero en
otra cuenta (por ejemplo, un nuevo modulo de AWS Academy). Aqui no hay
secretos: las contrasenas viven solo en las EC2 (`/etc/timetogame`,
`infra/rabbitmq/.env`) y en el respaldo local fuera del repo.

## Inventario actual (cuenta original, us-east-1)

| Recurso | Configuracion |
|---|---|
| EC2 backend | `t3.small` (2 vCPU, 2 GB), Ubuntu 26.04 LTS (`ami-0b6d9d3d33ba97d99`), 8 GB de disco, `us-east-1b`, SG `launch-wizard-1`, IP elastica `52.4.101.123`. Corre backend (:8000), admin (:8001) y el cluster RabbitMQ en Docker (solo `127.0.0.1`) |
| EC2 frontend | `t3.micro`, Ubuntu 26.04 LTS (misma AMI), 8 GB, `us-east-1b`, SG `launch-wizard-2`, Nginx + Let's Encrypt, IP elastica `54.234.201.101`, dominio `54.234.201.101.nip.io` |
| RDS | MySQL 8.4.9, `db.t3.micro`, 20 GB gp3, identificador `database-1`, usuario maestro `timetogame`, base `timetogame`, **sin acceso publico**, misma VPC que el backend (`vpc-01e86485831b168ff`, la por defecto) |
| API Gateway | HTTP API `7rqni1tm5d`, stage `$default` con auto-deploy, **sin CORS propio** |
| Llave SSH | Par de llaves `cloudkey` (`cloudkey.pem`) |
| EC2 Kafka (nueva, EP5/EP6) | `t3.large` recomendado (8 GB; minimo `t3.medium` + swap), Ubuntu 26.04, 16 GB de disco. Corre 3 Zookeeper + 3 brokers + Kafka UI en Docker (`infra/kafka`, `infra/kafka-ui`) |

### Security Groups

| SG | Entrada | Origen |
|---|---|---|
| Backend | 22 (SSH) | Tu IP (`/32`) |
| Backend | 8000, 8001 (TCP) | `0.0.0.0/0` (el API Gateway llega por IP publica) |
| Frontend | 22 (SSH) | Tu IP (`/32`) |
| Frontend | 80, 443 (HTTP/HTTPS) | `0.0.0.0/0` |
| RDS | 3306 (MySQL) | El **SG del backend** (referencia al SG, no una IP) |
| Kafka | 22 (SSH) | Tu IP (`/32`) |
| Kafka | 9092-9094 (TCP) | El **SG del backend** (backend y admin se conectan por IP privada) |

### API Gateway (HTTP API)

Integraciones (todas *HTTP URI*, metodo ANY):

| Integracion | URL |
|---|---|
| backend | `http://<IP_BACKEND>:8000/{proxy}` |
| admin | `http://<IP_BACKEND>:8001/api/admin/{proxy}` |
| publico | `http://<IP_BACKEND>:8000/api/public/{proxy}` |

Rutas (sin ruta `ANY` y sin `$default`):

| Ruta | Integracion | Authorizer |
|---|---|---|
| `GET`, `POST`, `PUT`, `DELETE`, `PATCH` `/{proxy+}` | backend | `entra-id` |
| `OPTIONS /{proxy+}` | backend | ninguno (preflight CORS) |
| `GET`, `POST`, `PATCH`, `DELETE` `/api/admin/{proxy+}` | admin | `entra-id` (PATCH: editar topicos Kafka) |
| `OPTIONS /api/admin/{proxy+}` | admin | ninguno |
| `GET /api/public/{proxy+}` | publico | ninguno |

JWT authorizer `entra-id`: identity source `$request.header.Authorization`,
issuer `https://login.microsoftonline.com/e199e31e-ed32-476b-983e-152fa84d6507/v2.0`,
audience `3aea2387-eabd-41de-87da-1503cf447475`.

### Lo que NO esta en AWS (no cambia al cambiar de cuenta)

- Microsoft Entra ID: tenant, App registrations `timetogame-api` y
  `timetogame-spa`, roles y usuarios.
- El codigo en GitHub.

## Recrear todo en una cuenta nueva

Orden recomendado. `<IP_BACKEND>` y `<IP_FRONTEND>` son las IP elasticas nuevas.

1. **Llave SSH**: EC2 > Key pairs > crear `cloudkey` (o importar la publica
   de la actual) y guardar el `.pem`.
2. **Security Groups**: crear los cuatro de la tabla de arriba, en la VPC
   por defecto.
3. **RDS**: MySQL 8, Free tier, `db.t3.micro`, 20 GB, usuario `timetogame`,
   misma VPC, *Public access: No*, SG de RDS. Anotar el endpoint.
4. **EC2 Kafka**: Ubuntu, `t3.large`, SG Kafka, 16 GB. Por SSH (al terminar
   imprime el `KAFKA_BOOTSTRAP` para el paso siguiente):
   ```bash
   curl -fsSLO https://raw.githubusercontent.com/panda2048-max/CloudNativeGrupo7/main/infra/aws/kafka/setup-kafka.sh
   bash setup-kafka.sh
   ```
5. **EC2 backend**: Ubuntu, `t3.small`, SG backend, 8 GB o mas. Asociar una
   IP elastica. Luego, por SSH:
   ```bash
   curl -fsSLO https://raw.githubusercontent.com/panda2048-max/CloudNativeGrupo7/main/infra/aws/backend/setup-backend.sh
   RDS_ENDPOINT=<endpoint> ORIGEN_FRONTEND=https://<IP_FRONTEND>.nip.io \n   KAFKA_BOOTSTRAP=<ip-privada-kafka>:9092,<ip-privada-kafka>:9093,<ip-privada-kafka>:9094 \n   bash setup-backend.sh
   ```
6. **Restaurar los datos** (respaldo local `timetogame-rds-*.sql`, fuera del repo):
   ```bash
   scp -i cloudkey.pem timetogame-rds-AAAA-MM-DD.sql ubuntu@<IP_BACKEND>:/tmp/
   # en la EC2:
   mysql -h <endpoint> -u timetogame -p timetogame < /tmp/timetogame-rds-AAAA-MM-DD.sql
   ```
7. **EC2 frontend**: Ubuntu, `t3.micro`, SG frontend, IP elastica. Por SSH
   (`IP_BACKEND` es para el proxy de las tendencias en vivo, que no pasa por el Gateway):
   ```bash
   curl -fsSLO https://raw.githubusercontent.com/panda2048-max/CloudNativeGrupo7/main/infra/aws/frontend/setup-frontend.sh
   IP_ELASTICA=<IP_FRONTEND> IP_BACKEND=<IP_BACKEND> CORREO=<tu correo> bash setup-frontend.sh
   ```
8. **API Gateway**: crear la HTTP API con las integraciones, rutas y el
   authorizer de las tablas de arriba. Anotar la Invoke URL.
9. **Microsoft Entra ID**: en `timetogame-spa` > Authentication, agregar
   `https://<IP_FRONTEND>.nip.io/callback` y `https://<IP_FRONTEND>.nip.io/`
   como redirect URIs (Single-page application).
10. **Frontend**: en el PC, actualizar `timetogame-frontend/.env.production`
   (URL del API Gateway y las dos URL de redirect), compilar y subir:
   ```bash
   cd timetogame-frontend && npm run build
   scp -i cloudkey.pem -r dist/* ubuntu@<IP_FRONTEND>:/tmp/dist/
   ssh -i cloudkey.pem ubuntu@<IP_FRONTEND> "sudo rm -rf /var/www/timetogame/* && sudo cp -r /tmp/dist/* /var/www/timetogame/"
   ```
11. **Verificar**:
    - `curl -i https://<api>.execute-api.us-east-1.amazonaws.com/api/videojuegos`
      debe dar 401 `{"message":"Unauthorized"}`;
    - `/api/public/videojuegos` debe dar 200;
    - login en `https://<IP_FRONTEND>.nip.io`;
    - la portada muestra "Tendencias en vivo" con el indicador "En vivo" (SSE por Nginx);
    - Kafka UI por tunel: `ssh -i cloudkey.pem -L 8080:localhost:8080 ubuntu@<ip-kafka>` y `http://localhost:8080`.

Al apagar y encender las instancias no hay que repetir nada: Docker,
RabbitMQ, Kafka, los dos servicios y Nginx arrancan solos y las IP elasticas no
cambian. Si cambia tu IP publica, actualizar la regla SSH de los SG.
