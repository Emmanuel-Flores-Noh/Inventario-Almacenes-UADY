# **Requisitos Funcionales**

## **Sistema 1 (Sistema Centralizado / Proveedor)**

* Registrar y almacenar la información detallada de los productos.  
* Generar un código QR que contendrá la información del nombre del producto, ID en la base de datos y su número de serie, permitiendo dar seguimiento a la trazabilidad de cada producto.  
* Procesar las peticiones para actualizar la tienda a la que se le vendió el producto y realizar la vinculación correspondiente con el código QR.  
* Ejecutar la operación solicitada a través de una interfaz de servicio común y retornar una respuesta que incluya el estado, resultado y fecha.

## **Sistema 2 (Cliente del Proveedor)**

* Construir solicitudes estructuradas para registrar a qué tienda específica se le vendió un producto.  
* Enviar la solicitud de actualización de inventario al Broker sin instanciar ni conocer la ubicación directa del Sistema 1\.  
* Recibir y presentar la respuesta de confirmación de la operación enviada por el Broker.

## **Broker y Registro de Servicios**

* Recibir las solicitudes emitidas por el Sistema 2 e identificar la operación a realizar.  
* Mantener un registro que permita registrar proveedores, buscar servicios y sustituirlos dinámicamente sin modificar al cliente.  
* Reenviar las peticiones al sistema correspondiente (Sistema 1\) y devolver el resultado al cliente original.  
* Gestionar errores básicos, rechazando servicios desconocidos o solicitudes con parámetros inválidos.

# **Requisitos No Funcionales**

## **Arquitectura y Comunicación**

* El sistema debe estar desarrollado en el lenguaje de programación Java.  
* Debe implementarse bajo una arquitectura distribuida basada en el patrón Broker, separando el cliente, el intermediario y los proveedores en procesos distintos.  
* La comunicación entre los componentes del sistema distribuido debe realizarse utilizando Sockets TCP.  
* El intercambio de información de las solicitudes y respuestas debe estructurarse obligatoriamente en formato JSON.

## **Rendimiento y Confiabilidad**

* El sistema debe implementar marshalling y unmarshalling para el procesamiento de los mensajes JSON.  
* El servidor debe soportar atención concurrente de múltiples peticiones mediante el uso de hilos.  
* Debe incluir mecanismos de resiliencia de red, específicamente el manejo de timeouts, gestión de desconexiones y reintentos limitados.  
* Las solicitudes y respuestas deben incluir metadatos obligatorios como el identificador único, nombre de la operación, estado, mensaje de error (si aplica) y la marca de tiempo (fecha y hora).
