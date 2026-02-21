#!/bin/bash
# stop.sh
set -euo pipefail
docker rm -f be-api || true