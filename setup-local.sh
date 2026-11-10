#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"

PYTHON_BIN="${PYTHON:-python3}"
VENV_DIR="$ROOT/.venv"
VENV_PY="$VENV_DIR/bin/python"

echo "========================================"
echo " Configurando entorno local del proyecto"
echo "========================================"

if ! command -v "$PYTHON_BIN" >/dev/null 2>&1; then
    echo "[Error] No se encontró '$PYTHON_BIN'. Instala Python 3 y vuelve a intentarlo."
    exit 1
fi

if [ ! -d "$VENV_DIR" ]; then
    echo ""
    echo "1. Creando entorno virtual en .venv ..."
    if ! "$PYTHON_BIN" -m venv "$VENV_DIR"; then
        echo "[Error] No se pudo crear el entorno virtual."
        echo "En Debian/Ubuntu puede que necesites instalar: sudo apt install python3-venv"
        exit 1
    fi
else
    echo ""
    echo "1. El entorno virtual .venv ya existe."
fi

echo ""
echo "2. Instalando dependencias de Python ..."
"$VENV_PY" -m pip install --upgrade pip
"$VENV_PY" -m pip install -r "$ROOT/requirements.txt"

if ! "$VENV_PY" -c "import weasyprint" >/dev/null 2>&1; then
    echo ""
    echo "[Advertencia] WeasyPrint se instaló, pero no pudo importarse."
    echo "Probablemente falten librerías del sistema. En Debian/Ubuntu:"
    echo "  sudo apt install libpango-1.0-0 libpangoft2-1.0-0 libcairo2 libffi-dev"
fi

echo ""
echo "3. Instalando Mermaid CLI local en bin/ ..."
if command -v npm >/dev/null 2>&1; then
    npm install --prefix "$ROOT/bin" --no-audit --no-fund

    PUPPETEER_BIN="$ROOT/bin/node_modules/.bin/puppeteer"
    if [ -x "$PUPPETEER_BIN" ]; then
        echo ""
        echo "4. Verificando navegador para Puppeteer ..."
        if ! (cd "$ROOT/bin" && "$PUPPETEER_BIN" browsers install chrome); then
            echo "[Advertencia] No se pudo descargar Chrome automáticamente."
            echo "Si Mermaid falla, define PUPPETEER_EXECUTABLE_PATH con la ruta de un navegador."
        fi
    fi
else
    echo "[Advertencia] No se encontró Node.js/npm."
    echo "Mermaid CLI no se instalará localmente. Instala Node.js y vuelve a ejecutar este script,"
    echo "o instala Mermaid globalmente con: sudo npm install -g @mermaid-js/mermaid-cli"
fi

echo ""
echo "========================================"
echo " Entorno local listo"
echo "========================================"
echo "Procesa todos los documentos con:"
echo "  ./procesar-docs-local.sh"
echo ""
echo "O un documento específico con:"
echo "  ./procesar-docs-local.sh docs/markdown/Proyecto_A"
