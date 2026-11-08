# **Contrato de interacciones**

La comunicación entre los sistemas se realizará mediante mensajes en formato JSON transmitidos a través de Sockets TCP.

## **Atributos de la Solicitud**

* **Identificador de solicitud:** ID único para rastrear el mensaje.  
* **Nombre del servicio:** Nombre del servicio solicitado al Broker.  
* **Nombre de la operación:** Acción específica a ejecutar.  
* **Parámetros:** Datos requeridos para la operación (ej. numero\_serie, id\_tienda, id\_usuario).  
* **Fecha y hora:** Marca temporal del envío de la solicitud.

## **Atributos de la Respuesta**

* **Identificador de solicitud:** ID correspondiente a la petición original.  
* **Resultado:** Datos devueltos tras procesar la operación.  
* **Estado:** Estado de ejecución (ej. éxito o error).  
* **Mensaje de error:** Descripción del error (si corresponde).  
* **Fecha y hora:** Marca temporal de la respuesta.

![Diagrama de interaccion](Interacciones.mmd)
