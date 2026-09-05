#!/usr/bin/env bash
# Deja un Keycloak recien instalado listo para TimeToGame: realm, roles,
# cliente publico SPA (Authorization Code + PKCE, sin client_secret) y
# usuarios de prueba. No contiene ningun secreto real: el cliente es
# publico y las contrasenas de abajo son datos de demo de solo lectura
# local (no se usan en ningun ambiente real), documentadas a proposito
# para que quien evalue el proyecto pueda iniciar sesion.
#
# Uso:
#   1. Arrancar Keycloak en dev mode (otra terminal):
#        KC_BOOTSTRAP_ADMIN_USERNAME=admin KC_BOOTSTRAP_ADMIN_PASSWORD=admin123 \
#        bin/kc.sh start-dev --http-port=8080
#   2. Desde la carpeta de la distribucion de Keycloak:
#        bash /ruta/a/identity-provider/setup-keycloak.sh
set -euo pipefail

KC_URL="${KC_URL:-http://localhost:8080}"
KC_BOOTSTRAP_ADMIN_USERNAME="${KC_BOOTSTRAP_ADMIN_USERNAME:-admin}"
KC_BOOTSTRAP_ADMIN_PASSWORD="${KC_BOOTSTRAP_ADMIN_PASSWORD:-admin123}"

REALM=timetogame
CLIENT_ID=timetogame-spa
AUDIENCE=timetogame-api
SPA_ORIGIN="${SPA_ORIGIN:-http://localhost:5173}"

DEMO_USER_PASSWORD="${DEMO_USER_PASSWORD:-user123}"
DEMO_ADMIN_PASSWORD="${DEMO_ADMIN_PASSWORD:-admin123}"

kcadm() { bin/kcadm.sh "$@"; }

kcadm config credentials --server "$KC_URL" --realm master \
  --user "$KC_BOOTSTRAP_ADMIN_USERNAME" --password "$KC_BOOTSTRAP_ADMIN_PASSWORD"

echo "== Realm =="
kcadm create realms -s realm=$REALM -s enabled=true -s sslRequired=none

echo "== Roles =="
kcadm create roles -r $REALM -s name=USER -s 'description=Lectura autenticada'
kcadm create roles -r $REALM -s name=ADMIN -s 'description=Administracion (alta/edicion/borrado)'

echo "== Cliente SPA publico (Authorization Code + PKCE) =="
kcadm create clients -r $REALM \
  -s clientId=$CLIENT_ID \
  -s publicClient=true \
  -s protocol=openid-connect \
  -s standardFlowEnabled=true \
  -s implicitFlowEnabled=false \
  -s directAccessGrantsEnabled=false \
  -s serviceAccountsEnabled=false \
  -s "redirectUris=[\"$SPA_ORIGIN/*\"]" \
  -s "webOrigins=[\"$SPA_ORIGIN\"]" \
  -s "attributes={\"pkce.code.challenge.method\":\"S256\",\"post.logout.redirect.uris\":\"$SPA_ORIGIN/*\"}"

CLIENT_UUID=$(kcadm get clients -r $REALM -q clientId=$CLIENT_ID --fields id --format csv --noquotes | tail -1)

echo "== Mapper de audience ($AUDIENCE) =="
kcadm create "clients/$CLIENT_UUID/protocol-mappers/models" -r $REALM \
  -s name=audience-$AUDIENCE \
  -s protocol=openid-connect \
  -s protocolMapper=oidc-audience-mapper \
  -s "config={\"included.custom.audience\":\"$AUDIENCE\",\"id.token.claim\":\"false\",\"access.token.claim\":\"true\"}"

echo "== Usuarios de prueba =="
kcadm create users -r $REALM -s username=user -s enabled=true -s emailVerified=true \
  -s email=user@timetogame.local -s firstName=User -s lastName=Timetogame
kcadm set-password -r $REALM --username user --new-password "$DEMO_USER_PASSWORD" --temporary=false
kcadm add-roles -r $REALM --uusername user --rolename USER

kcadm create users -r $REALM -s username=admin -s enabled=true -s emailVerified=true \
  -s email=admin@timetogame.local -s firstName=Admin -s lastName=Timetogame
kcadm set-password -r $REALM --username admin --new-password "$DEMO_ADMIN_PASSWORD" --temporary=false
kcadm add-roles -r $REALM --uusername admin --rolename USER --rolename ADMIN

echo "Listo. Realm '$REALM' configurado en $KC_URL."
