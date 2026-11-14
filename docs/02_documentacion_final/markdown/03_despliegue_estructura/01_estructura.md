# Estructura del proyecto

El repositorio es un proyecto Maven multimódulo (Java 21) con cuatro módulos:
`common`, `broker`, `sistema-centralizado` (Sistema 1) y `sistema-cliente`
(Sistema 2). Cada módulo ejecutable tiene su propio `Dockerfile`, y
`docker-compose.yml` los orquesta junto con MariaDB.

## Vista general

```text
/
├── .githooks/                 # Hook pre-commit: compila la documentación
├── .run/                      # Configuraciones de ejecución del IDE
├── bin/                       # Mermaid CLI local para generar documentos
├── broker/                    # Módulo Broker (intermediario)
├── common/                    # Módulo compartido: contrato JSON y modelos
├── docker/
│   └── mariadb/init/          # Scripts SQL de inicialización
│       ├── 01_schema.sql      # Esquema de la base de datos
│       └── 02_seed.sql        # Datos iniciales
├── docs/                      # Documentación del proyecto
├── scripts/docs/              # Generador de PDFs a partir de Markdown y Mermaid
├── sistema-centralizado/      # Módulo Sistema 1 (proveedor de servicios)
├── sistema-cliente/           # Módulo Sistema 2 (cliente Swing)
├── .env.example               # Plantilla de variables de entorno
├── docker-compose.yml         # Orquestación de MariaDB, Broker y sistemas
├── pom.xml                    # POM padre del proyecto multimódulo
└── README.md
```

Todos los módulos siguen el layout estándar de Maven
(`src/main/java`, `src/main/resources`, `src/test/java`) bajo el paquete base
`mx.uady.inventario`.

## Módulo `common`

Biblioteca que comparten los tres componentes. Define el contrato de mensajes
para que el cliente, el Broker y el proveedor hablen el mismo JSON.

```text
common/src/main/java/mx/uady/inventario/common/
├── contrato/     # Solicitud, Respuesta y EstadoRespuesta
├── error/        # CodigoError: códigos de error del contrato
├── json/         # GsonFactory y adaptadores (LocalDateTime)
├── modelo/       # Producto, Mercancia, Venta, Usuario, TiendaAlmacen, ...
└── servicio/     # Servicios y Operaciones: nombres de servicios y operaciones
```

Los servicios definidos son `gestorTiendas`, `inventario` y `gestorPrecios`.

## Módulo `broker`

Intermediario que registra a los proveedores y enruta cada solicitud al
proveedor que ofrece el servicio pedido.

```text
broker/src/main/java/mx/uady/inventario/broker/
├── Main.java     # Punto de entrada
├── model/        # Proveedor: datos de un proveedor registrado
├── network/      # BrokerServer y ManejadorConexion (un hilo por conexión)
├── registry/     # RegistroServicios: servicio -> proveedores
├── routing/      # Enrutador: reenvío, timeouts y reintentos
└── util/         # Configuración por variables de entorno, bitácora y JSON
```

## Módulo `sistema-centralizado` (Sistema 1)

Proveedor de servicios sobre MariaDB. Atiende las solicitudes que llegan del
Broker y, cuando hay pantalla, abre la interfaz Swing del Administrador.
Sigue una arquitectura por capas (Modelo-Servicio-Repositorio) con MVC para la
interfaz.

```text
sistema-centralizado/src/main/java/mx/uady/inventario/central/
├── Main.java         # Arranca el servidor TCP y, si hay pantalla, la interfaz
├── controller/       # Controladores de la interfaz y tareas en segundo plano
├── model/            # Modelos propios de la vista (ProductoConUnidades, ...)
├── network/          # Servidor TCP, despacho de operaciones y registro en el Broker
├── repository/       # Acceso a datos por JDBC
├── service/          # Lógica de negocio, validaciones y generación de QR
│   └── jdbc/         # Implementaciones JDBC de los servicios y transacciones
├── util/             # Configuración de BD y del servicio, conexión y QR
└── view/             # Ventana principal y paneles de Productos, Mercancía y Ventas
    └── components/   # Componentes reutilizables (vista previa del QR)
```

## Módulo `sistema-cliente` (Sistema 2)

Aplicación de escritorio para el proveedor. Se comunica con el Sistema 1
únicamente a través del Broker.

```text
sistema-cliente/src/main/java/mx/uady/inventario/cliente/
├── Main.java         # Punto de entrada
├── controller/       # RegistroVentaController: flujo de registro de ventas
├── model/            # ClienteTCP y DTOs (Mercancia, Tienda, Usuario)
├── network/          # Reservado para la capa de red
├── util/             # Configuración de red y traducción de errores
└── view/             # RegistroVentaFrame y filtros de mercancía
    └── components/   # Componentes reutilizables (vista previa del QR)
```

## Documentación (`docs/` y `scripts/docs/`)

```text
docs/
├── 01_documentacion_inicial/   # Diseño del sistema (entrega inicial)
└── 02_documentacion_final/     # Documentación final y evidencias
    ├── diagramas/              # Diagramas Mermaid (.mmd)
    ├── evidencias/             # Salidas y scripts de las pruebas
    ├── extras/                 # Material de referencia externo
    ├── markdown/               # Un subdirectorio por documento
    └── out/                    # PDFs y diagramas generados
```

Los PDFs de `out/` se generan con los scripts de `scripts/docs/`, ya sea con
Docker (`procesar-docs-docker`) o con un entorno local (`setup-local` y
`procesar-docs-local`). El hook `.githooks/pre-commit` los regenera antes de
cada commit.
