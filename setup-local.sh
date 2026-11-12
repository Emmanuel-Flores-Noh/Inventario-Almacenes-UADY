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
echo "3. Instalando Mermaid CLI local en bin/ con pnpm ..."
PNPM_MODE=""
if command -v pnpm >/dev/null 2>&1; then
    PNPM_MODE="pnpm"
elif command -v corepack >/dev/null 2>&1; then
    echo "   pnpm no está instalado; intentando activarlo con corepack ..."
    corepack enable pnpm >/dev/null 2>&1 || true
    if command -v pnpm >/dev/null 2>&1; then
        PNPM_MODE="pnpm"
    else
        PNPM_MODE="corepack"
    fi
fi

if [ -n "$PNPM_MODE" ]; then
    if [ "$PNPM_MODE" = "corepack" ]; then
        (cd "$ROOT/bin" && corepack pnpm install)
    else
        (cd "$ROOT/bin" && pnpm install)
    fi

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
    echo "[Advertencia] No se encontró pnpm."
    echo "Mermaid CLI no se instalará localmente. Instala pnpm con:"
    echo "  corepack enable pnpm"
    echo "o, si no tienes corepack:"
    echo "  npm install -g pnpm"
    echo "Después vuelve a ejecutar este script."
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
