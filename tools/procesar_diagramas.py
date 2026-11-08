import os
import subprocess
import glob
import json
from sys import argv
from os import path
import shutil

def faltan_dependencias():
    return shutil.which("mmdc") is None

def preparar_directorios(directorios: list[str]):
    if len(directorios) == 0:
        return
    for directorio in directorios:
        os.makedirs(directorio, exist_ok=True)

def preparar_puppeteer(docs_dir):
    """Crea la configuración para que mmdc use el Chromium nativo de linux"""
    config_path = os.path.join(docs_dir, "puppeteer-config.json")
    config_data = {
        "executablePath": "/usr/bin/chromium",
        "args": ["--no-sandbox", "--disable-setuid-sandbox"]
    }
    with open(config_path, "w", encoding="utf-8") as f:
        json.dump(config_data, f, indent=4)
    return config_path

def procesar_diagramas(DIR_DIAGRAMAS, OUT_PNG, OUT_PDF, config_puppeteer):
    archivos_mmd = glob.glob(os.path.join(DIR_DIAGRAMAS, "*.mmd"))
    
    if not archivos_mmd:
        print(f"No se encontraron archivos .mmd en '{DIR_DIAGRAMAS}'.")
        return

    for archivo in archivos_mmd:
        nombre_base = os.path.basename(archivo).replace(".mmd", "")
        ruta_png = os.path.join(OUT_PNG, f"{nombre_base}.png")
        ruta_pdf = os.path.join(OUT_PDF, f"{nombre_base}.pdf")

        print(f"Convirtiendo diagrama: {nombre_base}.mmd ...")
        
        try:
            # Generar PNG (agregamos el -p con la ruta de configuración)
            subprocess.run(["mmdc", "-p", config_puppeteer, "-i", archivo, "-o", ruta_png, "-b", "transparent"], check=True)
            # Generar PDF
            subprocess.run(["mmdc", "-p", config_puppeteer, "-i", archivo, "-o", ruta_pdf, "-b", "transparent"], check=True)
            
            print(f"    -> Guardados en {OUT_PNG} y {OUT_PDF}")
        except subprocess.CalledProcessError as e:
            print(f"    [Error] Falló la conversión de {nombre_base}.mmd: {e}")

def main():
    if faltan_dependencias():
        print("Se necesita mermaid CLI para procesar los diagramas.")
        print("Instálalo con el comando: sudo npm install -g @mermaid-js/mermaid-cli")
        return

    if len(argv) < 2:
        print("Error: No se ha proporcionado la carpeta principal de los documentos.")
        print("Uso: python procesar_diagramas.py <RUTA_DOCS>")
        return

    DOCS_DIR = argv[1]

    if not os.path.exists(DOCS_DIR):
        print(f"La carpeta especificada no existe: {DOCS_DIR}")
        return

    DIR_DIAGRAMAS = os.path.join(DOCS_DIR, "diagramas")

    if not path.exists(DIR_DIAGRAMAS):
        print(f"No hay diagramas para procesar en: {DIR_DIAGRAMAS}")
        return

    OUT_PNG = path.join(DOCS_DIR, "out/diagramas/png")
    OUT_PDF = path.join(DOCS_DIR, "out/diagramas/pdf")

    preparar_directorios([OUT_PNG, OUT_PDF])
    
    # Generar config para esquivar el error de chrome-headless-shell
    config_puppeteer = preparar_puppeteer(DOCS_DIR)
    
    procesar_diagramas(DIR_DIAGRAMAS, OUT_PNG, OUT_PDF, config_puppeteer)

if __name__ == "__main__":
    main()
