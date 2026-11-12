# Template para proyectos Markdown y Mermaid

Este es un template automatizado para generar documentos en formato PDF a partir de archivos Markdown y diagramas de Mermaid. 

Está diseñado para funcionar de manera **100% portable**, ya sea con Docker o con un entorno local aislado, con una tipografía limpia (Inter) y una paleta de colores profesional (Slate). Además, compila tus documentos automáticamente cada vez que haces un commit gracias a sus Git Hooks.

## Características

- **Dos formas de ejecución**: Docker con todo incluido, o un entorno local aislado con `.venv` y Mermaid CLI instalado dentro del proyecto.
- **Soporte nativo para Mermaid**: Escribe diagramas en `.mmd` y el sistema los convierte a PNG y PDF de forma transparente.
- **Soporte para fórmulas LaTeX**: Soporte para ecuaciones matemáticas en línea (`$formula$`) y en bloque (`$$formula$$`, `\[formula\]` o ````math````) convertidas a vectores SVG nítidos.
- **Diseño profesional**: Los PDFs generados usan la fuente *Inter* de Google Fonts y colores de la paleta Slate de Tailwind.
- **Multidocumento y saltos de página**: Puedes dividir un documento en múltiples archivos `.md` y el sistema los unirá insertando un salto de página entre cada uno.
- **Git Hooks integrados**: Se compila automáticamente antes de cada `git commit`, asegurando que el código y los PDFs finales estén siempre sincronizados.
- **Multiplataforma**: Funciona en Windows, Linux y macOS.

---

## Estructura del Repositorio

Para que el compilador funcione correctamente, debes respetar la siguiente estructura de carpetas:

```text
/
├── .githooks/                # Scripts de automatización de Git
├── .venv/                    # Entorno virtual local de Python (generado)
├── bin/                      # Mermaid CLI local (generado por setup-local)
├── diagramas/                # Archivos de Mermaid (.mmd) van aquí
├── markdown/                 # Documentos
│   └── Proyecto_A/           # El nombre de esta subcarpeta será el nombre del PDF final
│       ├── 01-inicio.md      # Usa numeración para mantener el orden
│       ├── 02-cuerpo.md      # Cada archivo genera un salto de página automático
│       └── imagen.jpg        # Recursos locales del documento
├── out/                      # Generados automaticamente
│   ├── diagramas/
│   │   ├── png/
│   │   └── pdf/
│   └── docs/                 # Aquí aparecerán tus PDFs finales
├── tools/                    # Las herramientas necesarias para los documentos
│   ├── Dockerfile            # Configuracion del contenedor
│   ├── procesar_diagramas.py # Script compilador de Mermaid
│   ├── procesar_latex.py     # Script y módulo para procesamiento de LaTeX
│   └── procesar_markdown.py  # Script compilador de WeasyPrint
├── requirements.txt          # Dependencias Python para el entorno local
├── setup-local.cmd           # Instalador local para Windows
├── setup-local.sh            # Instalador local para Linux/macOS
├── procesar-docs.cmd         # Lanzador Docker para Windows
├── procesar-docs.sh          # Lanzador Docker para Linux/macOS
├── procesar-docs-local.cmd   # Lanzador local para Windows
└── procesar-docs-local.sh    # Lanzador local para Linux/macOS
```

## Requisitos

### Opción Docker

- Docker
- Git

### Opción local

- Python 3
- Node.js 18.19 o superior y pnpm
- Git
- En Linux, las librerías del sistema que requiere WeasyPrint (Pango, Cairo y FFI)

## Instalación

### Opción 1: Docker (recomendada)

```bash
./procesar-docs.sh
```

En Windows:

```bat
procesar-docs.cmd
```

### Opción 2: Entorno local aislado

Esta alternativa no usa Docker. Crea un `.venv` de Python e instala Mermaid CLI
dentro de la carpeta `bin/` del proyecto.

En Linux/macOS:

```bash
./setup-local.sh
./procesar-docs-local.sh
```

En Windows:

```bat
setup-local.cmd
procesar-docs-local.cmd
```

También puedes procesar un solo documento:

```bash
./procesar-docs-local.sh docs/markdown/Proyecto_A
./procesar-docs-local.sh docs/markdown/Proyecto_A/01-inicio.md
```

`setup-local` instala las dependencias de `requirements.txt` en `.venv`, instala
`@mermaid-js/mermaid-cli` en `bin/node_modules` usando `pnpm` y verifica el
navegador que usará Puppeteer. Los scripts de Python buscan `mmdc` primero en el
`PATH` del sistema, luego en `bin/mmdc` y finalmente en `bin/node_modules/.bin/mmdc`.

Si no tienes `pnpm`, puedes activarlo con Corepack o instalarlo con npm:

```bash
corepack enable pnpm
# o
npm install -g pnpm
```

Puedes forzar una ruta específica con:

```bash
export MMDC_PATH=/ruta/a/mmdc
export PUPPETEER_EXECUTABLE_PATH=/ruta/a/chrome
```

### Rutas de salida

Los scripts aceptan `-o` / `--out` para elegir dónde generar sus archivos:

```bash
# PDFs de los documentos
python tools/procesar_markdown.py docs --out salida/pdf

# Diagramas PNG y PDF (se crean salida/diagramas/png y salida/diagramas/pdf)
python tools/procesar_diagramas.py docs --out salida/diagramas

# SVG de una fórmula o Markdown procesado
python tools/procesar_latex.py --expr "E = mc^2" --out salida/formula.svg
python tools/procesar_latex.py documento.md --out salida/documento-procesado.md
```

En `procesar_markdown.py`, `--out` controla los PDFs finales. Los diagramas que
compile ese script se guardan por defecto en `<SALIDA>/diagramas`; puedes
cambiarlos de forma independiente con `--diagramas-out`.

### Documento de demostración

El repositorio incluye `docs/markdown/Demo_Funcionalidades/`, un documento de
ejemplo que reúne las funcionalidades principales: Markdown, tablas, código,
imágenes locales, fórmulas LaTeX y diagramas Mermaid. También incluye una lista
de comprobación para usarlo como prueba después de cambiar el proyecto.

```bash
./procesar-docs-local.sh docs/markdown/Demo_Funcionalidades
```

El PDF se genera en `docs/out/docs/Demo_Funcionalidades.pdf`.

Si WeasyPrint falla al importarse en Debian/Ubuntu, instala las librerías del
sistema con:

```bash
sudo apt install libpango-1.0-0 libpangoft2-1.0-0 libcairo2 libffi-dev
```

## Configuracion

### Git hook
Activa el Git Hook automático para que los documentos se compilen solos cada vez que hagas un commit, ejecuta este comando por única vez en tu terminal:

```Bash
git config core.hooksPath .githooks
```


