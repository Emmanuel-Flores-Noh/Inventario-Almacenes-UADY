# Template para proyectos Markdown y Mermaid

Este es un template automatizado para generar documentos en formato PDF a partir de archivos Markdown y diagramas de Mermaid. 

Está diseñado para funcionar de manera **100% portable** a través de Docker, con una tipografía limpia (Inter) y una paleta de colores profesional (Slate). Además, compila tus documentos automáticamente cada vez que haces un commit gracias a sus Git Hooks.

## Características

- **Cero dependencias locales**: Todo corre dentro de un contenedor Docker (incluye Python, Node.js, Chromium y WeasyPrint).
- **Soporte nativo para Mermaid**: Escribe diagramas en `.mmd` y el sistema los convierte a PNG y PDF de forma transparente.
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
│   └── procesar_markdown.py  # Script compilador de WeasyPrint
├── procesar-docs.cmd         # Lanzador para Windows
└── procesar-docs.sh          # Lanzador para Linux/macOS
```

## Requisitos

- Docker
- Git

## Configuracion

### Git hook
Activa el Git Hook automático para que los documentos se compilen solos cada vez que hagas un commit, ejecuta este comando por única vez en tu terminal:

```Bash
git config core.hooksPath .githooks
```


