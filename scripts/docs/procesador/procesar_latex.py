import os
import re
import sys
import html
import argparse
import base64
import functools

try:
    import ziamath as zm
except ImportError:
    zm = None

def verificar_dependencias() -> bool:
    """Verifica si la librería ziamath está instalada."""
    if zm is None:
        print(
            "  [Advertencia] La librería 'ziamath' no está instalada.\n"
            "  Para renderizar fórmulas LaTeX a SVG, instálala con:\n"
            "      pip install ziamath",
            file=sys.stderr
        )
        return False
    return True

@functools.lru_cache(maxsize=2048)
def latex_a_svg(latex_str: str, inline: bool = False, font_size: float = None, color: str = "#1e293b") -> str:
    """
    Convierte una expresión LaTeX en código SVG usando ziamath.
    Retorna el string SVG generado.
    """
    if zm is None:
        raise RuntimeError("ziamath no está instalado en el entorno.")
    
    latex_limpio = latex_str.strip()
    if not latex_limpio:
        return ""
    
    # ziamath.Latex acepta inline (bool), size (pt), color (hex/css)
    renderer = zm.Latex(latex_limpio, inline=inline, size=font_size, color=color)
    return renderer.svg()

@functools.lru_cache(maxsize=2048)
def latex_a_data_uri(latex_str: str, inline: bool = False, font_size: float = None, color: str = "#1e293b") -> str:
    """
    Convierte una expresión LaTeX a una Data URI base64 en formato SVG.
    Ejemplo: data:image/svg+xml;base64,...
    """
    svg_code = latex_a_svg(latex_str, inline=inline, font_size=font_size, color=color)
    b64 = base64.b64encode(svg_code.encode("utf-8")).decode("ascii")
    return f"data:image/svg+xml;base64,{b64}"

def procesar_latex_en_markdown(
    contenido_md: str,
    inline_size: float = 11,
    block_size: float = 13,
    color: str = "#1e293b"
) -> str:
    """
    Parsea y reemplaza fórmulas matemáticas LaTeX (en línea y en bloques)
    por etiquetas HTML con imágenes SVG incrustadas como Data URI.
    
    Soporta:
      - Bloques: $$ ... $$, \\[ ... \\], y ```math ... ```
      - En línea: $ ... $ y \\( ... \\)
    Protege bloques de código preexistentes para no alterar su contenido.
    """
    if zm is None:
        verificar_dependencias()
        return contenido_md

    # 1. Proteger bloques de código (fenced blocks y código en línea)
    bloques_codigo = []

    def proteger_fenced(match):
        lenguaje = match.group(2).strip() if match.group(2) else ""
        contenido = match.group(3)
        # Si el bloque es explícitamente ```math ... ```, lo convertimos a bloque LaTeX
        if lenguaje.lower() == "math":
            return f"\n\n$${contenido}$$\n\n"
        
        idx = len(bloques_codigo)
        bloques_codigo.append(match.group(0))
        return f"<!--CODE_BLOCK_PLACEHOLDER_{idx}-->"

    def proteger_inline_code(match):
        idx = len(bloques_codigo)
        bloques_codigo.append(match.group(0))
        return f"<!--CODE_BLOCK_PLACEHOLDER_{idx}-->"

    # Regex para bloques cercados ```lang\n...\n```
    patron_fenced = re.compile(r'(```([a-zA-Z0-9_-]*)\r?\n([\s\S]*?)\r?\n```)', re.MULTILINE)
    texto = patron_fenced.sub(proteger_fenced, contenido_md)

    # Regex para código inline `code` o ``code``
    patron_inline_code = re.compile(r'(`+)(.+?)\1', re.DOTALL)
    texto = patron_inline_code.sub(proteger_inline_code, texto)

    # 2. Reemplazar bloques matemáticos LaTeX ($$ ... $$ y \[ ... \])
    def reemplazar_bloque(match):
        formula = match.group(1).strip()
        if not formula:
            return ""
        try:
            uri = latex_a_data_uri(formula, inline=False, font_size=block_size, color=color)
            alt = html.escape(formula, quote=True)
            return f'\n\n<div class="math-block"><img src="{uri}" class="math-block-img" alt="{alt}" /></div>\n\n'
        except Exception as e:
            print(f"  [Advertencia] Error al procesar bloque LaTeX '{formula[:40]}...': {e}", file=sys.stderr)
            alt = html.escape(formula, quote=True)
            return f'\n\n<div class="math-block math-error"><pre><code>{alt}</code></pre></div>\n\n'

    # Bloques $$ ... $$
    patron_bloque_dolares = re.compile(r'\$\$(.+?)\$\$', re.DOTALL)
    texto = patron_bloque_dolares.sub(reemplazar_bloque, texto)

    # Bloques \[ ... \]
    patron_bloque_corchetes = re.compile(r'\\\[(.+?)\\\]', re.DOTALL)
    texto = patron_bloque_corchetes.sub(reemplazar_bloque, texto)

    # 3. Reemplazar fórmulas inline ($ ... $ y \( ... \))
    def reemplazar_inline(match):
        formula = match.group(1).strip()
        if not formula:
            return ""
        try:
            uri = latex_a_data_uri(formula, inline=True, font_size=inline_size, color=color)
            alt = html.escape(formula, quote=True)
            return f'<span class="math-inline"><img src="{uri}" class="math-inline-img" alt="{alt}" /></span>'
        except Exception as e:
            print(f"  [Advertencia] Error al procesar LaTeX inline '{formula[:30]}...': {e}", file=sys.stderr)
            alt = html.escape(formula, quote=True)
            return f'<code class="math-error">{alt}</code>'

    # Inline $ ... $ (no precedido por \, sin espacios inmediatamente adentro)
    patron_inline_dolares = re.compile(r'(?<!\\)\$(?!\s)([^\$\r\n]+?)(?<!\s)(?<!\\)\$')
    texto = patron_inline_dolares.sub(reemplazar_inline, texto)

    # Inline \( ... \)
    patron_inline_parentesis = re.compile(r'\\\((.+?)\\\)', re.DOTALL)
    texto = patron_inline_parentesis.sub(reemplazar_inline, texto)

    # 4. Restaurar los bloques de código protegidos
    for idx, bloque in enumerate(bloques_codigo):
        texto = texto.replace(f"<!--CODE_BLOCK_PLACEHOLDER_{idx}-->", bloque)

    # 5. Desescapar signos de dólar literales (\$ -> $)
    texto = texto.replace(r"\$", "$")

    return texto


def guardar_salida(ruta, contenido):
    """Guarda contenido de texto creando la carpeta de destino si es necesario."""
    ruta = os.path.abspath(ruta)
    directorio = os.path.dirname(ruta)
    if directorio:
        os.makedirs(directorio, exist_ok=True)
    with open(ruta, "w", encoding="utf-8") as f:
        f.write(contenido)
    return ruta


def main():
    parser = argparse.ArgumentParser(
        description="Procesa fórmulas LaTeX y documentos Markdown.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""Ejemplos:
  python procesar_latex.py docs
  python procesar_latex.py documento.md --out documento-procesado.md
  python procesar_latex.py --expr "E = mc^2" --out formula.svg
""",
    )
    parser.add_argument(
        "entrada",
        nargs="?",
        help="Carpeta de documentos, archivo .md o expresión LaTeX directa.",
    )
    parser.add_argument(
        "--expr",
        dest="expr",
        default=None,
        help="Renderiza una expresión LaTeX a SVG.",
    )
    parser.add_argument(
        "--inline",
        action="store_true",
        help="Renderiza la expresión como fórmula en línea.",
    )
    parser.add_argument(
        "-o", "--out",
        dest="out",
        default=None,
        help=(
            "Archivo de salida. Para --expr guarda el SVG; para un .md guarda "
            "el Markdown procesado. Si se omite, se imprime en la salida estándar."
        ),
    )
    args = parser.parse_args()

    if args.expr is not None:
        try:
            svg = latex_a_svg(args.expr, inline=args.inline)
            if args.out:
                ruta = guardar_salida(args.out, svg)
                print(f"SVG guardado en {ruta}")
            else:
                print(svg)
        except Exception as e:
            print(f"Error renderizando LaTeX: {e}", file=sys.stderr)
            return 1
        return 0

    if not args.entrada:
        parser.print_help()
        return 1

    entrada = args.entrada

    if os.path.isfile(entrada):
        with open(entrada, "r", encoding="utf-8") as f:
            contenido = f.read()
        procesado = procesar_latex_en_markdown(contenido)

        if args.out:
            ruta = guardar_salida(args.out, procesado)
            print(f"Markdown procesado guardado en {ruta}")
        else:
            print(procesado)
        return 0

    if os.path.isdir(entrada):
        if verificar_dependencias():
            print(f"Módulo procesar_latex listo. Directorio de documentos: {entrada}")
        return 0

    # Si es una expresión directa sin comillas o simple
    if verificar_dependencias():
        try:
            svg = latex_a_svg(entrada, inline=False)
            if args.out:
                ruta = guardar_salida(args.out, svg)
                print(f"SVG guardado en {ruta}")
            else:
                print(svg)
        except Exception as e:
            print(f"Error procesando '{entrada}': {e}", file=sys.stderr)
            return 1

    return 0


if __name__ == "__main__":
    sys.exit(main())
