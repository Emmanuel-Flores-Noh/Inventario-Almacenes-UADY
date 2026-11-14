# Dependencias locales de Mermaid

Esta carpeta contiene la instalación local de Mermaid CLI para poder procesar
los diagramas sin depender de Docker.

El contenido de `node_modules/` no se versiona. Para instalarlo ejecuta desde
la raíz del proyecto:

```bash
./setup-local.sh
```

En Windows:

```bat
setup-local.cmd
```

El instalador usa `pnpm` si está disponible. Si no lo tienes, puedes activarlo
con `corepack enable pnpm` o instalarlo con `npm install -g pnpm`.

El archivo `pnpm-workspace.yaml` autoriza el script de instalación de Puppeteer
para que pnpm no lo bloquee. Después, `setup-local` se encarga de descargar el
navegador que necesita Mermaid.

Los scripts de Python buscan `mmdc` en este orden:

1. La variable de entorno `MMDC_PATH`.
2. El comando `mmdc` disponible en el `PATH` del sistema.
3. `bin/mmdc` o `bin/node_modules/.bin/mmdc` dentro del proyecto.
