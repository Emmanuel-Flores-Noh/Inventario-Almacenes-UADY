import os
import re
import glob
import sys
from sys import argv
from pathlib import Path
import markdown
from weasyprint import HTML
from procesar_latex import procesar_latex_en_markdown

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

def procesar_markdown(DOCS_DIR):
    DIR_MARKDOWN = os.path.join(DOCS_DIR, "markdown")
    OUT_DOCS = os.path.join(DOCS_DIR, "out/docs")
    DIR_PNG = os.path.join(DOCS_DIR, "out/diagramas/png")
    
    os.makedirs(OUT_DOCS, exist_ok=True)
    
    if not os.path.exists(DIR_MARKDOWN):
        print(f"No se encontró la carpeta de markdown en: {DIR_MARKDOWN}")
        return
    
    # Listar solo subcarpetas dentro de markdown/
    subcarpetas = [f.path for f in os.scandir(DIR_MARKDOWN) if f.is_dir()]
    
    if not subcarpetas:
        print(f"No se encontraron subcarpetas en '{DIR_MARKDOWN}'.")
        return

    for subcarpeta in subcarpetas:
        nombre_proyecto = os.path.basename(subcarpeta)
        archivos_md = glob.glob(os.path.join(subcarpeta, "*.md"))
        
        if not archivos_md:
            print(f"Omitiendo subcarpeta '{nombre_proyecto}' (no contiene archivos .md)")
            continue
            
        print(f"Procesando documento: {nombre_proyecto} ...")
        
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
        bloques_codigo = []
        def proteger_bloque_codigo(m):
            if m.group(0).startswith('```math'):
                return m.group(0)
            idx = len(bloques_codigo)
            bloques_codigo.append(m.group(0))
            return f"<!--CODE_BLOCK_IMG_{idx}-->"

        patron_fenced = re.compile(r'(```[a-zA-Z0-9_-]*\r?\n[\s\S]*?\r?\n```)', re.MULTILINE)
        contenido_procesado = patron_fenced.sub(proteger_bloque_codigo, contenido_md)
        patron_inline_code = re.compile(r'(`+)(.+?)\1', re.DOTALL)
        contenido_procesado = patron_inline_code.sub(proteger_bloque_codigo, contenido_procesado)

        # Regex: Buscar ![alt](nombre.mmd)
        patron_mmd = r'!\[([^\]]*)\]\(([^)]+)\.mmd\)'
        contenido_procesado = re.sub(patron_mmd, lambda match: reemplazar_diagramas_por_png(match, DIR_PNG), contenido_procesado)
        
        # Regex: Buscar imágenes con formato markdown ![alt](url) relativas al documento
        patron_img = r'!\[([^\]]*)\]\(([^)]+)\)'
        contenido_procesado = re.sub(patron_img, lambda match: reemplazar_imagenes_locales(match, subcarpeta), contenido_procesado)

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
        for idx, bloque in enumerate(bloques_codigo):
            contenido_procesado = contenido_procesado.replace(f"<!--CODE_BLOCK_IMG_{idx}-->", bloque)
        
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
                    border-collapse: collapse; 
                    width: 100%; 
                    margin-bottom: 1.5em; 
                }}
                th, td {{ 
                    border: 1px solid #cbd5e1; /* Borde sutil slate-300 */
                    padding: 8pt 10pt; 
                    text-align: left;
                    vertical-align: top;
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
        
        ruta_pdf_salida = os.path.join(OUT_DOCS, f"{nombre_proyecto}.pdf")
        base_url = "file://" + os.path.abspath(subcarpeta) + "/"
        
        try:
            HTML(string=html_completo, base_url=base_url).write_pdf(ruta_pdf_salida)
            print(f"  -> Documento PDF generado exitosamente en {ruta_pdf_salida}")
        except Exception as e:
            print(f"  [Error] Falló la generación del PDF para {nombre_proyecto}: {e}")

def main():
    if len(argv) < 2:
        print("Error: No se ha proporcionado la carpeta principal de los documentos.")
        print("Uso: python procesar_markdown.py <RUTA_DOCS>")
        return

    DOCS_DIR = argv[1]
    procesar_markdown(DOCS_DIR)

if __name__ == "__main__":
    main()
