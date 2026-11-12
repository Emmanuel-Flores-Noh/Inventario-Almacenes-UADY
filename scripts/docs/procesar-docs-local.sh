#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SHARED_VENV_DIR="${SHARED_VENV_DIR:-${HOME}/.venv}"
VENV_PY="$SHARED_VENV_DIR/bin/python"
PY_DIR="$ROOT/scripts/docs/procesador"
TARGET="${1:-$ROOT/docs}"

if [ ! -x "$VENV_PY" ]; then
    echo "[Error] No se encontró el entorno virtual compartido en $SHARED_VENV_DIR."
    echo "Ejecuta primero: ./scripts/docs/setup-local.sh"
    exit 1
fi

# Permite encontrar mmdc instalado localmente en bin/ o en bin/node_modules/.bin/
export PATH="$ROOT/bin:$ROOT/bin/node_modules/.bin:$PATH"

if [ -d "$TARGET/diagramas" ]; then
    echo "========================================"
    echo " Procesando diagramas ..."
    echo "========================================"
    "$VENV_PY" "$PY_DIR/procesar_diagramas.py" "$TARGET"
    echo ""
fi

echo "========================================"
echo " Procesando documentos ..."
echo "========================================"
"$VENV_PY" "$PY_DIR/procesar_markdown.py" "$TARGET"

echo ""
echo "¡Proceso finalizado!"