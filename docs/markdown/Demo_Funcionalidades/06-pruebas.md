# 6. Pruebas y verificación

Este archivo comienza en una página nueva gracias al sistema multidocumento.
Su objetivo es servir como lista de comprobación después de generar el PDF.

## 6.1 Lista de comprobación

| Funcionalidad | Ejemplo | Qué debe verse |
| :--- | :--- | :--- |
| Encabezados | Secciones 1 a 6 | Jerarquía y tamaños consistentes |
| Texto con énfasis | Sección 2.1 | Negrita, cursiva y código en línea |
| Listas | Sección 2.2 | Listas ordenadas y anidadas |
| Citas | Sección 2.3 | Bloque con borde lateral |
| Tablas | Sección 2.4 | Bordes y alineación por columnas |
| Código | Sección 2.5 | Bloques con fondo y fuente monoespaciada |
| HTML | Sección 2.6 | Recuadro con borde azul |
| Salto de página | Sección 2.7 | Contenido nuevo en otra página |
| LaTeX en línea | Sección 3.1 | Fórmulas integradas en el texto |
| LaTeX en bloque | Secciones 3.2 a 3.5 | Fórmulas centradas y nítidas |
| LaTeX en tablas | Sección 3.6 | Fórmulas dentro de celdas |
| Código protegido | Sección 3.7 | Sintaxis visible como texto |
| Mermaid | Sección 4 | Diagramas convertidos a imagen |
| Imagen Markdown | Sección 5.1 | Tarjeta PNG |
| Imagen HTML | Sección 5.2 | Logo SVG dentro de figure |
| Enlaces | Sección 5.3 | Enlaces clicables en el PDF |

## 6.2 Comandos de prueba

Generar únicamente esta demo:

```bash
python tools/procesar_markdown.py docs/markdown/Demo_Funcionalidades \
  --out docs/out/docs \
  --diagramas-out docs/out/diagramas
```

Generar todos los diagramas del proyecto:

```bash
python tools/procesar_diagramas.py docs --out docs/out/diagramas
```

Exportar una fórmula a SVG:

```bash
python tools/procesar_latex.py --expr "E = mc^2" --out docs/out/formulas/e-mc2.svg
```

Con el entorno local del proyecto:

```bash
./procesar-docs-local.sh docs/markdown/Demo_Funcionalidades
```

Con Docker:

```bash
./procesar-docs.sh
```

## 6.3 Resultado esperado

Si se usa la configuración por defecto, los archivos se generan en:

- `docs/out/docs/Demo_Funcionalidades.pdf`
- `docs/out/diagramas/png/`
- `docs/out/diagramas/pdf/`

Si se usa `--out`, el PDF se guarda en la carpeta indicada. Los diagramas se
guardan en `--diagramas-out` o, si no se especifica, en `<SALIDA>/diagramas`.

## 6.4 Notas

- Los enlaces externos necesitan conexión a internet para abrirse, pero el PDF
  se genera correctamente sin conexión.
- Las fórmulas se renderizan con `ziamath`; si falta la dependencia, el documento
  muestra una advertencia en la consola.
- Los diagramas necesitan `mmdc` en el `PATH` o instalado en `bin/`.
