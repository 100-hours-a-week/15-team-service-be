#!/bin/bash
set -euo pipefail

APP_DIR="/home/ubuntu/deploy/be"
IMAGE_URI="$(cat "${APP_DIR}/image-uri.txt")"

docker pull "${IMAGE_URI}"