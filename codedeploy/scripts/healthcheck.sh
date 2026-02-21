#!/bin/bash
set -euo pipefail

URL="http://127.0.0.1:8080/actuator/health"
MAX_RETRIES=30
SLEEP_SEC=5

echo "[healthcheck] Checking ${URL}"

for i in $(seq 1 "${MAX_RETRIES}"); do
  if curl -fsS "${URL}" >/dev/null 2>&1; then
    echo "[healthcheck] SUCCESS on attempt ${i}"
    exit 0
  fi

  echo "[healthcheck] Attempt ${i}/${MAX_RETRIES} failed. Retrying in ${SLEEP_SEC}s..."
  sleep "${SLEEP_SEC}"
done

echo "[healthcheck] FAILED after ${MAX_RETRIES} attempts."
exit 1