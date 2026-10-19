#!/usr/bin/env bash
# Crea (o recrea) un usuario de aplicacion en el cluster con permisos sobre el
# vhost "/". La contrasena se lee de stdin para que no quede en el historial ni
# en la lista de procesos. Los usuarios se replican a todos los nodos.
#
#   uso: echo -n "<contrasena>" | ./crear-usuario.sh <usuario> [tag]
#   ej.: sudo sed -n 's/^RABBITMQ_PASSWORD=//p' /etc/timetogame/rabbitmq-admin.env \
#          | ./crear-usuario.sh timetogame-admin management
set -euo pipefail
cd "$(dirname "$0")"

usuario="${1:?Falta el nombre de usuario}"
tag="${2:-}"
contrasena="$(cat)"
[ -n "$contrasena" ] || { echo "La contrasena (stdin) esta vacia" >&2; exit 1; }

ctl() { docker compose exec -T rabbitmq-1 rabbitmqctl -q "$@"; }

if ctl list_users | awk '{print $1}' | grep -qx "$usuario"; then
  ctl delete_user "$usuario" >/dev/null
fi
printf '%s' "$contrasena" | ctl add_user "$usuario" >/dev/null
ctl set_permissions -p / "$usuario" ".*" ".*" ".*" >/dev/null
ctl set_user_tags "$usuario" $tag >/dev/null
echo "Usuario '$usuario' listo${tag:+ (tag: $tag)}"
