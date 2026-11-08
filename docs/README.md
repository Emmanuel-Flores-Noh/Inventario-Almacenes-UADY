# Los documentos van en esta carpeta

## diagramas
Todos los diagramas en formato mermaid van en esta carpeta

## markdown
Todos los documentos en formato markdown van en esta carpeta.

Cada subdirectorio es un documento. El nombre de la carpeta define el nombre del documento de salida pdf, dentro de esa carpeta tambien se incluiran todos los elementos necesarios para el markdown excepto los diagramas

Para incrustar un diagrama se puede hacer con la sintaxis de imagen de markdown

```markdown
![nombre por si no se encuentra](nombreDiagrama.mmd)
```

El script se encarga de resolver el nombre del diagrama encontrandolo en la carpeta de diagrama convirtiendo el diagrama a png y lo insertara para poder hacer la compilacion a pdf

Si se desea, se pueden incrustar varios documentos markdown para simular saltos de pagina.

### Convencion de documentos
* Si solo se tiene un archivo markdown dentro de un subdirectorio se debe de llamar README.md
* Si se poseen varios archivos markdown se debe de llamar $numeroDeDocumento$_$nombreDescriptivo$.md siendo numeroDeDocumento el numero que define en que orden se insertan, preferente nombrarlo 01, 02, etc.

## out
Los documentos y diagramas convertidos en formatos de facil lectura

Se dividiran en 2 subcarpetas:
### docs
En esta se guardaran los markdown procesados en formato pdf con el nombre que ya mencionamos

### diagramas
En esta se guardaran los diagramas en dos carpetas segun su formato

* png
* pdf

## extras
En este directorio pueden ir otros documentos externos al proyecto, por ejemplo:
* documentaciones
* imagenes / diagramas de referencia


