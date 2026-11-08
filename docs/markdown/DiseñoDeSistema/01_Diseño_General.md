# **Sistema de Inventario para los Almacenes UADY**

# **Nombre de la aplicación**

Sistema de Trazabilidad e Inventarios UADY.

# **Funcionalidad principal**

* El Sistema 1 (sistema centralizado) se encarga de registrar y almacenar toda la información de los productos, generando un código QR para darles seguimiento.  
* El Sistema 2 (sistema del proveedor) permite realizar peticiones para actualizar y registrar a qué tienda específica se le vendió un producto.

# **Integración futura entre ambos sistemas**

Ambos sistemas se conectarán utilizando la arquitectura distribuida del patrón Broker. La integración se implementará separando los procesos e interconectándolos a través de Sockets TCP, los mensajes de las solicitudes y respuestas serán empaquetados en formato JSON (mediante marshalling y unmarshalling). 

El sistema soportará atención concurrente usando hilos, además de integrar registro dinámico, timeouts y manejo de desconexiones para asegurar la robustez de la comunicación entre el proveedor y el sistema central.

# **Diseño general**

* **Cliente (Sistema 2):** Construye la solicitud de actualización de inventario en JSON y la envía al Broker por socket, sin conocer ni instanciar de manera directa al sistema centralizado.  
* **Broker (Intermediario):** Es un tercer sistema independiente, completamente separado del Sistema 1 y del Sistema 2, cuya única responsabilidad es registrar servicios y redirigir las solicitudes entre ellos. Recibe las solicitudes del Sistema 2, identifica la operación a realizar consultando su registro de servicios y reenvía la petición al sistema correspondiente. Posteriormente recibe el resultado y devuelve la respuesta al cliente, gestionando errores básicos.  
* **Proveedor (Sistema 1):** Expone una interfaz de servicio para ejecutar la operación solicitada. Procesa la actualización de la tienda y la vinculación con el QR, retornando una respuesta (que incluye el estado, el resultado y la fecha) al Broker.

