#!/bin/bash
set -euo pipefail

APP_DIR="/home/ubuntu/deploy/be"

docker compose -f "${APP_DIR}/docker-compose.yml" down --remove-orphans || true
docker rm -f be-api alloy || true
