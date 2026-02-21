#!/bin/bash
# healthcheck.sh
set -euo pipefail
curl -fsS http://127.0.0.1:8080/actuator/health >/dev/null