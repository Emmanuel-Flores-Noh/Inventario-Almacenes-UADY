#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VENV_PY="$ROOT/.venv/bin/python"
TARGET="${1:-$ROOT/docs}"

if [ ! -x "$VENV_PY" ]; then
    echo "[Error] No se encontró el entorno virtual .venv."
    echo "Ejecuta primero: ./setup-local.sh"
    exit 1
fi

# Permite encontrar mmdc instalado localmente en bin/ o en bin/node_modules/.bin/
export PATH="$ROOT/bin:$ROOT/bin/node_modules/.bin:$PATH"

if [ -d "$TARGET/diagramas" ]; then
    echo "========================================"
    echo " Procesando diagramas ..."
    echo "========================================"
    "$VENV_PY" "$ROOT/tools/procesar_diagramas.py" "$TARGET"
    echo ""
fi

echo "========================================"
echo " Procesando documentos ..."
echo "========================================"
"$VENV_PY" "$ROOT/tools/procesar_markdown.py" "$TARGET"

echo ""
echo "¡Proceso finalizado!"
