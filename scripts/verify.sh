#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
if [ -z "${DOCKER_HOST:-}" ]; then
  DOCKER_HOST="$(docker context inspect --format '{{.Endpoints.docker.Host}}')"
  export DOCKER_HOST
fi
case "$DOCKER_HOST" in
  *colima*)
    TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE="${TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE:-/var/run/docker.sock}"
    export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE
    ;;
esac
exec ./mvnw clean verify "$@"
