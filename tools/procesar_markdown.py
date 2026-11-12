import os
import re
import glob
import sys
import argparse
from pathlib import Path
import markdown
from weasyprint import HTML
from procesar_latex import procesar_latex_en_markdown
from procesar_diagramas import (
    compilar_diagrama,
    faltan_dependencias,
    preparar_directorios,
    preparar_puppeteer,
)

PATRON_MMD = re.compile(r'!\[([^\]]*)\]\(([^)]+)\.mmd\)')
PATRON_FENCED = re.compile(r'(```[a-zA-Z0-9_-]*\r?\n[\s\S]*?\r?\n```)', re.MULTILINE)
PATRON_INLINE_CODE = re.compile(r'(`+)(.+?)\1', re.DOTALL)


def proteger_bloques_codigo(contenido):
    """Reemplaza bloques de código por marcadores para no procesar su contenido."""
    bloques_codigo = []

    def proteger_bloque_codigo(m):
        if m.group(0).startswith('```math'):
            return m.group(0)
        idx = len(bloques_codigo)
        bloques_codigo.append(m.group(0))
        return f"<!--CODE_BLOCK_IMG_{idx}-->"

    contenido_procesado = PATRON_FENCED.sub(proteger_bloque_codigo, contenido)
    contenido_procesado = PATRON_INLINE_CODE.sub(proteger_bloque_codigo, contenido_procesado)
    return contenido_procesado, bloques_codigo


def restaurar_bloques_codigo(contenido, bloques_codigo):
    for idx, bloque in enumerate(bloques_codigo):
        contenido = contenido.replace(f"<!--CODE_BLOCK_IMG_{idx}-->", bloque)
    return contenido


def reemplazar_diagramas_por_png(match, dir_png):
    alt_text = match.group(1)
    nombre_archivo = match.group(2)
    # Extraer el nombre puro del diagrama por si se incluyeron barras
    nombre_base = os.path.basename(nombre_archivo).replace('.mmd', '')

    # Construir ruta absoluta para que el motor de PDF siempre la encuentre
    ruta_png = os.path.abspath(os.path.join(dir_png, f"{nombre_base}.png"))

    if not os.path.exists(ruta_png):
        print(f"  [Advertencia] No se encontró el PNG compilado para: {nombre_base}.mmd en la ruta {ruta_png}")

    return f'![{alt_text}]({Path(ruta_png).as_uri()})'


def reemplazar_imagenes_locales(match, subcarpeta):
    alt_text = match.group(1)
    target = match.group(2).strip()

    # Si target viene delimitado por < > (ej: <./mi foto.png>) o con título
    if target.startswith('<'):
        idx_cierre = target.find('>')
        if idx_cierre != -1:
            url = target[1:idx_cierre].strip()
            resto = target[idx_cierre + 1:].strip()
            titulo = f" {resto}" if resto else ""
        else:
            url = target
            titulo = ""
    else:
        match_titulo = re.match(r'^(.*?)\s+(["\'].*?["\'])$', target)
        if match_titulo:
            url = match_titulo.group(1).strip()
            titulo = f" {match_titulo.group(2)}"
        else:
            url = target
            titulo = ""

    # Ignorar si ya es una URL externa (http, https), data URI o ya es file://
    if re.match(r'^(https?://|data:|file://)', url, re.IGNORECASE):
        return match.group(0)

    # Ignorar diagramas .mmd (se procesan como diagramas Mermaid)
    if url.lower().endswith('.mmd'):
        return match.group(0)

    # Resolver ruta relativa con respecto a la carpeta del documento (subcarpeta)
    ruta_absoluta = os.path.abspath(os.path.join(subcarpeta, url))

    if not os.path.exists(ruta_absoluta):
        print(f"  [Advertencia] No se encontró la imagen: '{url}' en la ruta '{ruta_absoluta}'")

    uri_imagen = Path(ruta_absoluta).as_uri()
    return f'![{alt_text}]({uri_imagen}{titulo})'


def encontrar_docs_dir(ruta):
    """Busca la carpeta raíz de documentos (la que contiene a 'markdown')."""
    actual = Path(ruta).expanduser().resolve()
    if actual.is_file():
        actual = actual.parent

    for candidato in [actual, *actual.parents]:
        if candidato.name == "markdown":
            return candidato.parent
        if (candidato / "markdown").is_dir():
            return candidato

    return None


def documentos_en_markdown(dir_markdown):
    """Devuelve los documentos (subcarpetas con archivos .md) de markdown/."""
    documentos = []

    with os.scandir(dir_markdown) as entradas:
        subcarpetas = sorted(
            (entrada for entrada in entradas if entrada.is_dir()),
            key=lambda entrada: entrada.name,
        )

    for subcarpeta in subcarpetas:
        archivos_md = sorted(glob.glob(os.path.join(subcarpeta.path, "*.md")))
        if not archivos_md:
            print(f"Omitiendo subcarpeta '{subcarpeta.name}' (no contiene archivos .md)")
            continue
        documentos.append((subcarpeta.path, subcarpeta.name, archivos_md))

    return documentos


def resolver_objetivo(entrada):
    """
    Interpreta el argumento recibido y devuelve:
      (docs_dir, [(carpeta_documento, nombre_salida, archivos_md), ...])
    """
    ruta = Path(entrada).expanduser().resolve()

    if not ruta.exists():
        raise FileNotFoundError(f"La ruta especificada no existe: {ruta}")

    if ruta.is_file():
        if ruta.suffix.lower() != ".md":
            raise ValueError(f"El archivo debe ser Markdown (.md): {ruta}")

        docs_dir = encontrar_docs_dir(ruta)
        if docs_dir is None:
            # Estructura alternativa: el archivo no está dentro de una carpeta 'markdown'.
            docs_dir = ruta.parent

        # Al seleccionar un archivo se renderiza únicamente ese archivo.
        return docs_dir, [(str(ruta.parent), ruta.stem, [str(ruta)])]

    if not ruta.is_dir():
        raise ValueError(f"La ruta no es un archivo ni una carpeta válida: {ruta}")

    docs_dir = encontrar_docs_dir(ruta)

    if docs_dir is not None:
        if ruta.name == "markdown":
            return docs_dir, documentos_en_markdown(ruta)

        if (ruta / "markdown").is_dir():
            return docs_dir, documentos_en_markdown(ruta / "markdown")

        archivos_md = sorted(glob.glob(os.path.join(str(ruta), "*.md")))
        if archivos_md:
            return docs_dir, [(str(ruta), ruta.name, archivos_md)]

    # Estructura alternativa: carpeta de documento fuera de una carpeta 'markdown'.
    archivos_md = sorted(glob.glob(os.path.join(str(ruta), "*.md")))
    if archivos_md:
        return ruta.parent, [(str(ruta), ruta.name, archivos_md)]

    raise ValueError(
        f"No se encontraron documentos para procesar en: {ruta}\n"
        "Pasa la carpeta de documentos (con 'markdown/'), una carpeta de documento "
        "o un archivo .md específico."
    )


def detectar_diagramas_mermaid(archivos_md):
    """Extrae las referencias .mmd usadas en los Markdown seleccionados."""
    referencias = []
    vistas = set()

    for archivo in archivos_md:
        with open(archivo, "r", encoding="utf-8") as f:
            contenido = f.read()

        # Ignorar ejemplos de diagramas dentro de bloques de código.
        contenido_protegido, _ = proteger_bloques_codigo(contenido)

        for match in PATRON_MMD.finditer(contenido_protegido):
            referencia = match.group(2).strip()
            if not referencia:
                continue
            if re.match(r'^(https?://|data:|file://)', referencia, re.IGNORECASE):
                continue
            if referencia not in vistas:
                vistas.add(referencia)
                referencias.append(referencia)

    return referencias


def resolver_ruta_diagrama(referencia, docs_dir, subcarpeta):
    """Busca el .mmd referenciado en el Markdown dentro de las rutas habituales."""
    referencia = referencia.strip()

    if re.match(r'^(https?://|data:|file://)', referencia, re.IGNORECASE):
        return None

    if referencia.startswith('<') and referencia.endswith('>'):
        referencia = referencia[1:-1].strip()

    if referencia.lower().endswith(".mmd"):
        referencia = referencia[:-4]

    referencia = referencia.replace("\\", os.sep)

    if os.path.isabs(referencia):
        candidatos = [referencia + ".mmd"]
    else:
        candidatos = [
            os.path.join(docs_dir, "diagramas", referencia + ".mmd"),
            os.path.join(subcarpeta, referencia + ".mmd"),
            os.path.join(docs_dir, referencia + ".mmd"),
            os.path.join(docs_dir, "diagramas", os.path.basename(referencia) + ".mmd"),
        ]

    vistas = set()
    for candidato in candidatos:
        candidato = os.path.abspath(os.path.normpath(candidato))
        if candidato in vistas:
            continue
        vistas.add(candidato)
        if os.path.isfile(candidato):
            return candidato

    return None


def obtener_salida_diagramas(docs_dir, diagramas_out=None):
    """Devuelve la carpeta raíz donde se guardan PNG/PDF de diagramas."""
    if diagramas_out:
        return os.path.abspath(diagramas_out)
    return os.path.join(docs_dir, "out/diagramas")


def compilar_diagramas_requeridos(archivos_md, docs_dir, subcarpeta, diagramas_out=None):
    """Compila únicamente los diagramas Mermaid referenciados por los Markdown."""
    referencias = detectar_diagramas_mermaid(archivos_md)

    if not referencias:
        return

    print(f"  Diagramas Mermaid requeridos: {', '.join(referencias)}")

    out_diagramas = obtener_salida_diagramas(docs_dir, diagramas_out)
    out_png = os.path.join(out_diagramas, "png")
    out_pdf = os.path.join(out_diagramas, "pdf")
    pendientes = []

    for referencia in referencias:
        ruta_mmd = resolver_ruta_diagrama(referencia, docs_dir, subcarpeta)

        if ruta_mmd is None:
            print(f"  [Advertencia] No se encontró el diagrama: '{referencia}.mmd'")
            continue

        nombre_base = os.path.splitext(os.path.basename(ruta_mmd))[0]
        ruta_png = os.path.join(out_png, f"{nombre_base}.png")
        ruta_pdf = os.path.join(out_pdf, f"{nombre_base}.pdf")

        # Evitar recompilar si ambos formatos ya están actualizados.
        if (
            os.path.exists(ruta_png)
            and os.path.exists(ruta_pdf)
            and os.path.getmtime(ruta_png) >= os.path.getmtime(ruta_mmd)
            and os.path.getmtime(ruta_pdf) >= os.path.getmtime(ruta_mmd)
        ):
            print(f"  Diagrama actualizado: {nombre_base}.mmd")
            continue

        pendientes.append((ruta_mmd, ruta_png, ruta_pdf))

    if not pendientes:
        return

    if faltan_dependencias():
        print("  [Advertencia] Se necesita Mermaid CLI para compilar los diagramas.")
        print("  Opción local: ejecuta ./setup-local.sh (Linux/macOS) o setup-local.cmd (Windows).")
        print("  Opción global: pnpm add -g @mermaid-js/mermaid-cli")
        return

    preparar_directorios([out_png, out_pdf])
    config_puppeteer = preparar_puppeteer(docs_dir)

    for ruta_mmd, ruta_png, ruta_pdf in pendientes:
        compilar_diagrama(ruta_mmd, ruta_png, ruta_pdf, config_puppeteer)


def procesar_documento(archivos_md, nombre_proyecto, subcarpeta, docs_dir, out_dir=None, diagramas_out=None):
    out_docs = os.path.abspath(out_dir) if out_dir else os.path.join(docs_dir, "out/docs")
    dir_png = os.path.join(obtener_salida_diagramas(docs_dir, diagramas_out), "png")

    os.makedirs(out_docs, exist_ok=True)

    print(f"Procesando documento: {nombre_proyecto} ...")

    # Detectar y compilar solo los diagramas que necesita este documento.
    compilar_diagramas_requeridos(archivos_md, docs_dir, subcarpeta, diagramas_out)

    contenido_md = ""
    archivos_ordenados = sorted(archivos_md)

    # Leer y concatenar todos los MD, añadiendo un salto de página entre archivos
    for indice, archivo in enumerate(archivos_ordenados):
        with open(archivo, "r", encoding="utf-8") as f:
            if indice > 0:
                # Forzar salto de página a partir del segundo archivo
                contenido_md += '\n\n<div style="page-break-before: always;"></div>\n\n'

            contenido_md += f.read() + "\n\n"

    # Proteger bloques de código para no alterar código de ejemplo
    contenido_procesado, bloques_codigo = proteger_bloques_codigo(contenido_md)

    # Regex: Buscar ![alt](nombre.mmd)
    contenido_procesado = PATRON_MMD.sub(
        lambda match: reemplazar_diagramas_por_png(match, dir_png),
        contenido_procesado,
    )

    # Regex: Buscar imágenes con formato markdown ![alt](url) relativas al documento
    patron_img = r'!\[([^\]]*)\]\(([^)]+)\)'
    contenido_procesado = re.sub(
        patron_img,
        lambda match: reemplazar_imagenes_locales(match, subcarpeta),
        contenido_procesado,
    )

    # Resolver también etiquetas HTML <img src="..."> si usan rutas relativas
    def reemplazar_src_html(match):
        antes = match.group(1)
        src = match.group(2).strip()
        despues = match.group(3)
        if re.match(r'^(https?://|data:|file://)', src, re.IGNORECASE):
            return match.group(0)
        ruta_absoluta = os.path.abspath(os.path.join(subcarpeta, src))
        if not os.path.exists(ruta_absoluta):
            print(f"  [Advertencia] No se encontró la imagen: '{src}' en la ruta '{ruta_absoluta}'")
        return f'{antes}{Path(ruta_absoluta).as_uri()}{despues}'

    patron_html_img = re.compile(r'(<img\b[^>]*?\bsrc=["\'])([^"\']+)(["\'][^>]*?>)', re.IGNORECASE)
    contenido_procesado = patron_html_img.sub(reemplazar_src_html, contenido_procesado)

    # Restaurar bloques de código protegidos
    contenido_procesado = restaurar_bloques_codigo(contenido_procesado, bloques_codigo)

    # Procesar fórmulas matemáticas LaTeX (inline y en bloques)
    contenido_procesado = procesar_latex_en_markdown(contenido_procesado)

    # Convertir Markdown a HTML
    html_body = markdown.markdown(contenido_procesado, extensions=['fenced_code', 'tables'])

    # Plantilla HTML con fuente Inter y paleta Slate (Tailwind)
    html_completo = f"""
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="utf-8">
        <style>
            /* Importar la fuente Inter desde Google Fonts */
            @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap');

            @page {{
                size: letter;
                margin: 1in;
            }}

            body {{
                font-family: 'Inter', system-ui, sans-serif;
                font-size: 11pt;
                line-height: 1.6;
                color: #334155; /* Párrafos */
                background-color: #ffffff;
            }}

            h1, h2, h3, h4, h5, h6 {{
                font-family: 'Inter', system-ui, sans-serif;
                page-break-after: avoid;
                margin-top: 10px;
                margin-bottom: 10px;
            }}

            h1 {{ font-size: 22pt; font-weight: 700; color: #0f172a; }}
            h2 {{ font-size: 16pt; font-weight: 600; color: #1e293b; }}
            h3 {{ font-size: 14pt; font-weight: 600; color: #1e293b; }}
            h4 {{ font-size: 12pt; font-weight: 600; color: #475569; }}

            p, ul, ol {{
                margin-top: 0;
                margin-bottom: 11pt;
                color: #334155; /* Párrafos */
            }}

            a {{
                color: #2563eb; /* Azul acorde a la paleta */
                text-decoration: none;
            }}
            a:hover {{
                text-decoration: underline;
            }}

            img {{
                max-width: 100%;
                max-height: 7.5in;
                height: auto;
                display: block;
                margin: 2em auto;
                border-radius: 8px; /* Pequeño toque estético para imágenes */
            }}

            /* Estilos para fórmulas matemáticas LaTeX (inline y bloques) */
            .math-inline {{
                display: inline;
                white-space: nowrap;
                line-height: normal;
            }}

            img.math-inline-img {{
                display: inline-block !important;
                vertical-align: -0.22em !important;
                margin: 0 0.15em !important;
                max-height: 1.6em;
                width: auto;
                border-radius: 0 !important;
            }}

            .math-block {{
                display: block;
                text-align: center;
                margin: 1.4em auto;
                page-break-inside: avoid;
            }}

            img.math-block-img {{
                display: inline-block !important;
                margin: 0 auto !important;
                max-width: 100%;
                height: auto;
                border-radius: 0 !important;
            }}

            .math-error {{
                color: #dc2626;
                background-color: #fee2e2;
                padding: 2px 4px;
                border-radius: 4px;
                font-family: "Courier New", Courier, monospace;
                font-size: 9pt;
            }}

            ul, ol {{
                padding-left: 24pt;
            }}
            li {{
                margin-bottom: 4pt;
            }}

            /* Tablas adaptadas a la paleta Slate */
            table {{
                margin-left: -70px;
                border-collapse: collapse;
                width: 750px;
                table-layout: fixed;
                margin-bottom: 1.5em;
            }}
            th, td {{
                border: 1px solid #cbd5e1; /* Borde sutil slate-300 */
                padding: 8pt 10pt;
                text-align: left;
                vertical-align: top;
                overflow-wrap: break-word;
                word-wrap: break-word;
            }}
            th {{
                background-color: #f8fafc; /* Fondo suave slate-50 */
                font-weight: 600;
                color: #0f172a;
            }}

            /* Bloques de código con fondos grises armonizados */
            pre {{
                background-color: #f1f5f9; /* slate-100 */
                border: 1px solid #e2e8f0; /* slate-200 */
                padding: 12pt;
                border-radius: 6px;
                overflow-x: auto;
                font-family: "Courier New", Courier, monospace;
                font-size: 9.5pt;
                line-height: 1.5;
            }}
            code {{
                font-family: "Courier New", Courier, monospace;
                font-size: 9.5pt;
                background-color: #f1f5f9;
                color: #0f172a;
                padding: 2px 4px;
                border-radius: 4px;
            }}
            pre code {{
                padding: 0;
                background-color: transparent;
            }}

            blockquote {{
                margin-left: 0;
                padding-left: 14pt;
                border-left: 4px solid #cbd5e1; /* slate-300 */
                color: #475569;
                font-style: italic;
            }}
        </style>
    </head>
    <body>
        {html_body}
    </body>
    </html>
    """

    ruta_pdf_salida = os.path.join(out_docs, f"{nombre_proyecto}.pdf")
    base_url = "file://" + os.path.abspath(subcarpeta) + "/"

    try:
        HTML(string=html_completo, base_url=base_url).write_pdf(ruta_pdf_salida)
        print(f"  -> Documento PDF generado exitosamente en {ruta_pdf_salida}")
    except Exception as e:
        print(f"  [Error] Falló la generación del PDF para {nombre_proyecto}: {e}")


def procesar_markdown(entrada, out_dir=None, diagramas_out=None):
    docs_dir, documentos = resolver_objetivo(entrada)

    if not documentos:
        print(f"No se encontraron documentos para procesar en: {docs_dir}")
        return

    # Si se define una salida general y no una específica para diagramas,
    # los diagramas compilados se guardan dentro de esa misma salida.
    if out_dir and not diagramas_out:
        diagramas_out = os.path.join(os.path.abspath(out_dir), "diagramas")

    print(f"Carpeta de documentos: {docs_dir}")

    for subcarpeta, nombre_proyecto, archivos_md in documentos:
        procesar_documento(
            archivos_md,
            nombre_proyecto,
            subcarpeta,
            docs_dir,
            out_dir=out_dir,
            diagramas_out=diagramas_out,
        )


def main():
    parser = argparse.ArgumentParser(
        description="Genera PDFs a partir de documentos Markdown y diagramas Mermaid.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""Ejemplos:
  python procesar_markdown.py docs
  python procesar_markdown.py docs/markdown/Proyecto_A --out salida
  python procesar_markdown.py docs/markdown/Proyecto_A/01-inicio.md -o salida
""",
    )
    parser.add_argument(
        "entrada",
        nargs="?",
        help="Carpeta de documentos, carpeta de un documento o archivo .md.",
    )
    parser.add_argument(
        "-o", "--out",
        dest="out_dir",
        default=None,
        help=(
            "Carpeta de salida para los PDFs. Por defecto se usa "
            "<RUTA_DOCS>/out/docs."
        ),
    )
    parser.add_argument(
        "--diagramas-out",
        dest="diagramas_out",
        default=None,
        help=(
            "Carpeta raíz para los diagramas compilados (subcarpetas png/ y pdf/). "
            "Por defecto: <RUTA_DOCS>/out/diagramas, o <SALIDA>/diagramas si "
            "se especifica --out."
        ),
    )
    args = parser.parse_args()

    if not args.entrada:
        parser.print_help()
        return 1

    try:
        procesar_markdown(
            args.entrada,
            out_dir=args.out_dir,
            diagramas_out=args.diagramas_out,
        )
    except (FileNotFoundError, ValueError) as e:
        print(f"Error: {e}")
        return 1

    return 0


if __name__ == "__main__":
    sys.exit(main())
