# **Tecnologías a utilizar**

* **Lenguaje de Programación:** Java (obligatorio para el desarrollo de ambos sistemas).  
* **Plataforma del Sistema del Proveedor (Sistema 2):** PC (Aplicación de escritorio en Java, utilizando Java Swing estilizado con la librería FlatLaf para la interfaz de usuario).  
* **Plataforma del Sistema Centralizado (Sistema 1):** Java (Backend para la lógica de almacenamiento, generación de códigos QR y vinculación de ventas, utilizando la librería ZXing (Zebra Crossing) para generar códigos QR en formato PNG), con una interfaz de escritorio para el Administrador desarrollada en Java Swing y estilizada con FlatLaf.  
* **Arquitectura de Comunicación:** Sockets TCP e Hilos (Threads) para la atención concurrente en el Broker.  
* **Intercambio de Datos:** Formato JSON (implementando marshalling y unmarshalling mediante la librería Gson en ambos sistemas).  
* **Despliegue y Entorno:** Docker Compose (para definir una estructura y proceso de desarrollo uniforme y más sencillo al orquestar los tres sistemas).

# **Arquitectura y Estructura de Carpetas del Sistema 1 (Centralizado / Proveedor)**

* **Base de Datos y Persistencia:** MariaDB para el almacenamiento de la información de los productos, utilizando el conector JDBC (MariaDB Connector/J) para la integración con el backend en Java.

**Arquitectura:** Se empleará una arquitectura por capas (Modelo-Servicio-Repositorio) que separa la lógica de negocio, el acceso a datos y la comunicación de red para mantener el sistema modular. La interfaz del Administrador se organiza con el patrón MVC sobre la capa de servicio: las vistas y controladores nunca acceden directamente a los repositorios. La interfaz solo se abre cuando hay una pantalla disponible; en entornos sin pantalla (como el contenedor Docker) el sistema arranca únicamente como servicio.  
**Estructura de Carpetas:**

* **model**: Contiene las clases de dominio (entidades como Producto, Venta y representaciones del QR).  
* **service**: Contiene la lógica de negocio, incluyendo la generación de códigos QR y la vinculación de ventas.  
* **repository**: Encargado del almacenamiento y acceso a la información de los productos en la base de datos.  
* **network**: Implementa la conexión por Sockets TCP, exponiendo la interfaz del servicio, registrándolo en el Broker y procesando las peticiones JSON entrantes.  
* **util**: Clases auxiliares para el marshalling y unmarshalling de JSON y herramientas de generación de QR.  
* **view**: Ventanas del Administrador (ventana principal con pestañas de Productos, Mercancía y Ventas) desarrolladas con Java Swing y estilizadas con FlatLaf.  
* **controller**: Manejadores de los eventos de la interfaz que invocan a la capa de servicio en segundo plano, sin congelar la interfaz.

# **Arquitectura y Estructura de Carpetas del Sistema 2 (Cliente)**

**Arquitectura:** Se empleará el patrón MVC (Modelo-Vista-Controlador) para desacoplar de forma efectiva la interfaz gráfica de escritorio de la lógica de comunicación con el intermediario.  
**Estructura de Carpetas:**

* **model**: Clases de datos y DTOs (Data Transfer Objects) para estructurar las solicitudes y procesar las respuestas del inventario.  
* **view**: Interfaces gráficas de usuario (ventanas, formularios de actualización) desarrolladas estrictamente con Java Swing y estilizadas con la librería FlatLaf.  
* **controller**: Manejadores que capturan los eventos de la interfaz (clics, ingresos de texto) y ordenan las ejecuciones de red.  
* **network**: Cliente Socket TCP que envía las solicitudes estructuradas en JSON al Broker y espera la respuesta sin conocer al Sistema 1\.  
* **util**: Herramientas auxiliares, dedicadas principalmente a la conversión y formateo de mensajes JSON.

# **Arquitectura y Estructura de Carpetas del Broker (Intermediario)**

* **model:** Clases y DTOs para representar la estructura genérica de las solicitudes y respuestas en tránsito.  
* **registry:** Módulo encargado de mantener en memoria el registro dinámico de proveedores y servicios disponibles, permitiendo búsquedas, registros y validaciones rápidas.  
* **network:** Manejo central de las comunicaciones de red; incluye la configuración del ServerSocket, la asignación de hilos (Threads) para la atención concurrente de múltiples peticiones y el enrutamiento de los mensajes.  
* **util:** Clases auxiliares para el marshalling y unmarshalling de los mensajes JSON (utilizando Gson) y la gestión centralizada de errores o timeouts.

