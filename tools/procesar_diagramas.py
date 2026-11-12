import os
import sys
import argparse
import subprocess
import glob
import json
from os import path
import shutil

ROOT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def es_ejecutable(ruta):
    if not os.path.isfile(ruta):
        return False
    return os.name == "nt" or os.access(ruta, os.X_OK)


def buscar_mmdc():
    """
    Busca Mermaid CLI en este orden:
      1. Variable de entorno MMDC_PATH.
      2. Comando mmdc disponible en el PATH del sistema.
      3. Carpeta bin/ del proyecto (instalación local alternativa a Docker).
    """
    candidatos = []

    ruta_env = os.environ.get("MMDC_PATH")
    if ruta_env:
        candidatos.append(ruta_env)

    ruta_sistema = shutil.which("mmdc")
    if ruta_sistema:
        candidatos.append(ruta_sistema)

    nombres_locales = ["mmdc.cmd", "mmdc.exe", "mmdc"] if os.name == "nt" else ["mmdc"]
    for nombre in nombres_locales:
        candidatos.append(os.path.join(ROOT_DIR, "bin", nombre))
    for nombre in nombres_locales:
        candidatos.append(os.path.join(ROOT_DIR, "bin", "node_modules", ".bin", nombre))

    for candidato in candidatos:
        if candidato and es_ejecutable(candidato):
            return candidato

    return None


def faltan_dependencias():
    return buscar_mmdc() is None


def buscar_navegador():
    """Busca un navegador compatible con Puppeteer/Chromium."""
    for variable in ("PUPPETEER_EXECUTABLE_PATH", "CHROME_PATH", "CHROMIUM_PATH"):
        ruta = os.environ.get(variable)
        if ruta and os.path.isfile(ruta):
            return ruta

    for nombre in (
        "chromium",
        "chromium-browser",
        "google-chrome",
        "google-chrome-stable",
        "microsoft-edge",
        "microsoft-edge-stable",
        "chrome",
    ):
        ruta = shutil.which(nombre)
        if ruta:
            return ruta

    candidatos = []

    if sys.platform == "darwin":
        candidatos.extend([
            "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
            "/Applications/Chromium.app/Contents/MacOS/Chromium",
            "/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge",
        ])
    elif os.name == "nt":
        for base in (
            os.environ.get("PROGRAMFILES"),
            os.environ.get("PROGRAMFILES(X86)"),
            os.environ.get("LOCALAPPDATA"),
        ):
            if not base:
                continue
            candidatos.extend([
                os.path.join(base, "Google", "Chrome", "Application", "chrome.exe"),
                os.path.join(base, "Chromium", "Application", "chrome.exe"),
                os.path.join(base, "Microsoft", "Edge", "Application", "msedge.exe"),
            ])
    else:
        candidatos.extend([
            "/usr/bin/chromium",
            "/usr/bin/chromium-browser",
            "/usr/bin/google-chrome",
            "/usr/bin/google-chrome-stable",
        ])

    for candidato in candidatos:
        if os.path.isfile(candidato):
            return candidato

    return None


def preparar_directorios(directorios: list[str]):
    if len(directorios) == 0:
        return
    for directorio in directorios:
        os.makedirs(directorio, exist_ok=True)


def preparar_puppeteer(docs_dir):
    """
    Crea la configuración de Puppeteer.
    Si hay un navegador del sistema lo usa; si no, deja que Puppeteer use
    el navegador que haya descargado durante la instalación local.
    """
    config_path = os.path.join(docs_dir, "puppeteer-config.json")
    config_data = {
        "args": ["--no-sandbox", "--disable-setuid-sandbox"]
    }

    navegador = buscar_navegador()
    if navegador:
        config_data["executablePath"] = navegador

    with open(config_path, "w", encoding="utf-8") as f:
        json.dump(config_data, f, indent=4)
    return config_path


def compilar_diagrama(archivo, ruta_png, ruta_pdf, config_puppeteer, mmdc=None):
    """Compila un único archivo .mmd a PNG y PDF."""
    nombre_base = os.path.basename(archivo).replace(".mmd", "")
    mmdc = mmdc or buscar_mmdc()

    if not mmdc:
        print("    [Error] No se encontró 'mmdc' ni en el PATH ni en la carpeta bin/ del proyecto.")
        return False

    print(f"Convirtiendo diagrama: {nombre_base}.mmd ...")

    try:
        # Generar PNG (agregamos el -p con la ruta de configuración)
        subprocess.run([mmdc, "-p", config_puppeteer, "-i", archivo, "-o", ruta_png, "-b", "transparent"], check=True)
        # Generar PDF
        subprocess.run([mmdc, "-p", config_puppeteer, "-i", archivo, "-o", ruta_pdf, "-b", "transparent"], check=True)

        print(f"    -> Guardados en {os.path.dirname(ruta_png)} y {os.path.dirname(ruta_pdf)}")
        return True
    except (subprocess.CalledProcessError, FileNotFoundError) as e:
        print(f"    [Error] Falló la conversión de {nombre_base}.mmd: {e}")
        return False


def procesar_diagramas(DIR_DIAGRAMAS, OUT_PNG, OUT_PDF, config_puppeteer, mmdc=None):
    archivos_mmd = glob.glob(os.path.join(DIR_DIAGRAMAS, "*.mmd"))
    
    if not archivos_mmd:
        print(f"No se encontraron archivos .mmd en '{DIR_DIAGRAMAS}'.")
        return

    for archivo in archivos_mmd:
        nombre_base = os.path.basename(archivo).replace(".mmd", "")
        ruta_png = os.path.join(OUT_PNG, f"{nombre_base}.png")
        ruta_pdf = os.path.join(OUT_PDF, f"{nombre_base}.pdf")

        compilar_diagrama(archivo, ruta_png, ruta_pdf, config_puppeteer, mmdc)


def main():
    parser = argparse.ArgumentParser(
        description="Convierte archivos Mermaid (.mmd) a PNG y PDF."
    )
    parser.add_argument(
        "docs_dir",
        help="Carpeta de documentos que contiene la subcarpeta diagramas/.",
    )
    parser.add_argument(
        "-o", "--out",
        dest="out_dir",
        default=None,
        help=(
            "Carpeta raíz de salida para los diagramas. Se generan las "
            "subcarpetas png/ y pdf/. Por defecto: <RUTA_DOCS>/out/diagramas."
        ),
    )
    args = parser.parse_args()

    DOCS_DIR = os.path.abspath(args.docs_dir)

    if not os.path.exists(DOCS_DIR):
        print(f"La carpeta especificada no existe: {DOCS_DIR}")
        return 1

    DIR_DIAGRAMAS = os.path.join(DOCS_DIR, "diagramas")

    if not path.exists(DIR_DIAGRAMAS):
        print(f"No hay diagramas para procesar en: {DIR_DIAGRAMAS}")
        return 1

    OUT_DIAGRAMAS = os.path.abspath(args.out_dir) if args.out_dir else path.join(DOCS_DIR, "out/diagramas")
    OUT_PNG = path.join(OUT_DIAGRAMAS, "png")
    OUT_PDF = path.join(OUT_DIAGRAMAS, "pdf")

    mmdc = buscar_mmdc()
    if mmdc is None:
        print("Se necesita Mermaid CLI para procesar los diagramas.")
        print("Opción local: ejecuta ./setup-local.sh (Linux/macOS) o setup-local.cmd (Windows).")
        print("Opción global: pnpm add -g @mermaid-js/mermaid-cli")
        return 1

    preparar_directorios([OUT_PNG, OUT_PDF])

    # Generar config para esquivar el error de chrome-headless-shell
    config_puppeteer = preparar_puppeteer(DOCS_DIR)
    print(f"Usando Mermaid CLI: {mmdc}")
    print(f"Carpeta de salida de diagramas: {OUT_DIAGRAMAS}")

    procesar_diagramas(DIR_DIAGRAMAS, OUT_PNG, OUT_PDF, config_puppeteer, mmdc)
    return 0


if __name__ == "__main__":
    sys.exit(main())
