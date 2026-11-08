#!/bin/bash

echo "========================================"
echo " 1. Preparando imagen de Docker..."
echo "========================================"
# Construye la imagen. Si ya existe, Docker usará la caché y será casi instantáneo.
docker build -t docs-processor ./tools

echo ""
echo "========================================"
echo " 2. Procesando diagramas y documentos..."
echo "========================================"
# Ejecuta el contenedor montando el directorio actual
docker run --rm -v "$(pwd)/docs:/docs" docs-processor /docs

echo ""
echo "¡Proceso finalizado!"
