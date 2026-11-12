#!/bin/bash

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

echo "========================================"
echo " 1. Preparando imagen de Docker..."
echo "========================================"
# Construye la imagen. Si ya existe, Docker usará la caché y será casi instantáneo.
docker build -t docs-processor "$ROOT/scripts/docs/procesador"

echo ""
echo "========================================"
echo " 2. Procesando diagramas y documentos..."
echo "========================================"
# Ejecuta el contenedor montando el directorio actual
docker run --rm -v "$ROOT/docs:/docs" docs-processor /docs

echo ""
echo "¡Proceso finalizado!"