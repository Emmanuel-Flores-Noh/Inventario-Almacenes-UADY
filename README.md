# Sistema de Trazabilidad e Inventarios UADY

Integración distribuida de dos sistemas de inventario de los almacenes de la UADY mediante el **patrón Broker**.

- **Sistema 1 (centralizado / proveedor):** registra productos y unidades, genera un código QR por unidad, registra ventas y persiste en MariaDB.
- **Sistema 2 (cliente):** aplicación de escritorio (Swing + FlatLaf) con la que un proveedor registra ventas a tiendas y consulta los QR.
- **Broker:** registra los servicios disponibles y enruta cada solicitud al proveedor correspondiente. El cliente solo conoce la dirección del Broker.

La comunicación usa Sockets TCP con mensajes JSON (Gson), atención concurrente con hilos, registro dinámico de servicios, timeouts y reintentos.

## Documentación

- 📄 [Documentos compilados a PDF](docs/02_documentacion_final/out/docs/)
  - [Portada](docs/02_documentacion_final/out/docs/06_portada.pdf)
  - [Preguntas de la actividad](docs/02_documentacion_final/out/docs/00_preguntas_actividad.pdf)
  - [Diagrama de componentes](docs/02_documentacion_final/out/docs/01_diagrama_componentes.pdf)
  - [Diagramas de secuencia](docs/02_documentacion_final/out/docs/02_diagramas_secuencia.pdf)
  - [Despliegue y estructura](docs/02_documentacion_final/out/docs/03_despliegue_estructura.pdf)
  - [Evidencias de pruebas](docs/02_documentacion_final/out/docs/04_evidencias_pruebas.pdf)
  - [Documento técnico final](docs/02_documentacion_final/out/docs/05_documento_tecnico_final.pdf)
- 🎞️ [Presentación](docs/02_documentacion_final/extras/Presentacion.pdf)

## Estructura

```text
common/                 Contrato compartido (solicitudes, respuestas, códigos de error, Gson)
broker/                 Broker: registro de servicios y enrutamiento
sistema-centralizado/   Sistema 1: lógica de negocio, QR y persistencia
sistema-cliente/        Sistema 2: interfaz de escritorio
docker/mariadb/init/    Esquema y datos iniciales de la base de datos
docs/                   Documentación (Markdown, diagramas Mermaid y PDFs)
```

## Tecnologías

Java 21 · Maven multimódulo · Gson · Swing + FlatLaf · ZXing · MariaDB (JDBC) · Docker Compose

## Ejecución

Requisitos: Docker y Docker Compose.

```bash
cp .env.example .env    # definir DB_ROOT_PASSWORD y DB_PASSWORD
docker compose up --build
```

| Componente | Puerto |
|---|---|
| MariaDB | 3306 |
| Broker | 5000 |
| Sistema 1 | 6000 |

El **Sistema 2** se ejecuta desde el IDE o como jar; también puede levantarse en Docker con un servidor X11:

```bash
docker compose --profile client up --build
```

## Pruebas

```bash
mvn test
```
