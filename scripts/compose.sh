#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
if docker compose version >/dev/null 2>&1 && docker buildx version >/dev/null 2>&1; then
  exec docker compose "$@"
fi
python3 scripts/bootstrap_docker.py
DOCKER_HOST="${DOCKER_HOST:-$(docker context inspect --format '{{.Endpoints.docker.Host}}')}"
export DOCKER_HOST
unset DOCKER_CONTEXT
exec docker --config "$PWD/.tools/docker" compose "$@"
