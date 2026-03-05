#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [[ ! -f ".env.docker" ]]; then
  echo "[INFO] .env.docker not found. Copying from .env.docker.example"
  cp .env.docker.example .env.docker
  echo "[INFO] Fill .env.docker (JWT_SECRET required, GEMINI_API_KEY optional) and run again."
  exit 1
fi

docker compose up -d --build
echo "[INFO] backend: http://localhost:8080"
echo "[INFO] swagger: http://localhost:8080/swagger-ui/index.html"
