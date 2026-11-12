# 2. Funcionalidades de Markdown

## 2.1 Texto y énfasis

Este párrafo incluye **texto en negrita**, *texto en cursiva*,
***texto en negrita y cursiva***, `código en línea` y un
[enlace externo a la guía de Markdown](https://www.markdownguide.org/).

También se pueden usar caracteres especiales como á, é, í, ó, ú, ñ, ¿ y ¡.
Primera línea con un salto de línea HTML.<br>
Segunda línea después del salto.

### Encabezado de nivel 3

#### Encabezado de nivel 4

##### Encabezado de nivel 5

###### Encabezado de nivel 6

Los seis niveles de encabezado están disponibles para organizar el contenido.

---

## 2.2 Listas

### Lista desordenada

- Elemento principal uno.
  - Subelemento anidado.
  - Otro subelemento anidado.
- Elemento principal dos.
- Elemento principal tres.

### Lista ordenada

1. Preparar el contenido.
2. Compilar los diagramas.
3. Generar el PDF.
    1. Revisar el resultado.
    2. Ajustar los estilos.
4. Publicar el documento.

## 2.3 Citas

> **Cita de ejemplo:** el contenido se escribe una sola vez en Markdown y se
> reutiliza para generar documentos PDF con un formato consistente.
>
> Las citas pueden ocupar varias líneas y conservar el estilo del template.

## 2.4 Tablas

| Funcionalidad | Estado | Observaciones |
| :--- | :---: | :--- |
| Texto con énfasis | Listo | Negrita, cursiva y código en línea |
| Listas anidadas | Listo | Ordenadas y desordenadas |
| Tablas | Listo | Alineación por columnas |
| Enlaces | Listo | Enlaces externos e internos |
| Imágenes | Listo | PNG y SVG locales |

## 2.5 Bloques de código

Ejemplo de Python:

```python
def saludar(nombre: str) -> str:
    return f"Hola, {nombre}"


print(saludar("mundo"))
```

Ejemplo de Bash:

```bash
python scripts/docs/procesador/procesar_markdown.py docs --out salida/pdf
```

Ejemplo de JSON:

```json
{
  "documento": "Demo_Funcionalidades",
  "diagramas": true,
  "latex": true,
  "salida": "PDF"
}
```

## 2.6 HTML dentro del Markdown

<div style="border-left: 5px solid #2563eb; background-color: #eff6ff; padding: 12px 16px; margin: 1.5em 0;">
<strong>Bloque HTML:</strong> este recuadro se inserta directamente en el
Markdown y conserva sus estilos al generar el PDF.
</div>

## 2.7 Salto de página manual

Además del salto automático que se produce entre archivos, es posible forzar un
salto de página con HTML:

<div style="page-break-before: always;"></div>

## 2.8 Contenido después del salto

Este apartado debería aparecer en una página nueva del PDF. El salto manual es
útil cuando se quiere separar una sección concreta sin crear otro archivo
Markdown.
