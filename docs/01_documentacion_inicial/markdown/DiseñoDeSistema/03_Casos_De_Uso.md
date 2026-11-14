# **Casos de Uso**

Especificación funcional de interacción y arquitectura distribuida entre componentes centrales, intermediarios y clientes.

## **Caso de Uso 1: Registrar Producto y Generar QR**

* **Actor:** Administrador del Almacén (Sistema 1 \- Centralizado)  
* **Descripción:** Permite registrar la información detallada de los productos y generar un código QR que permita dar seguimiento a su trazabilidad.  
* **Flujo principal:**  
  1. Se ingresa la información del producto.  
  2. El Sistema 1 registra y almacena la información detallada.  
  3. Se genera un código QR vinculado al producto.

## **Caso de Uso 2: Actualizar Tienda de Venta**

* **Actor:** Proveedor (Sistema 2 \- Cliente)  
* **Descripción:** Permite registrar a qué tienda específica se le vendió un producto mediante el envío de una solicitud.  
* **Flujo principal:**  
1. El Sistema 2 construye una solicitud estructurada en JSON.  
2. El Sistema 2 envía la solicitud al Broker a través de Sockets TCP.  
3. El Sistema 2 recibe la respuesta enviada por el Broker y la presenta al usuario.

## **Caso de Uso 3: Procesamiento y Vinculación de Venta**

* **Actor:** Sistema Centralizado (Sistema 1 \- Proveedor)  
* **Descripción:** El Sistema 1 procesa la actualización de la tienda y la vincula con el código QR correspondiente.  
* **Flujo principal:**  
1. El Sistema 1 recibe la solicitud a través de su interfaz común de servicio.  
2. Procesa la actualización y vincula la venta al código QR.  
3. Genera una respuesta incluyendo estado, resultado, y fecha y hora.  
4. Retorna la respuesta al Broker.

## **Caso de Uso 4: Intermediación de Solicitudes y Enrutamiento**

* **Actor:** Broker  
* **Descripción:** El Broker recibe peticiones del Sistema 2, identifica la operación y la enruta al Sistema 1 sin que los sistemas se conozcan directamente.  
* **Flujo principal:**  
1. El Broker recibe la solicitud del Sistema 2\.  
2. Identifica la operación y busca al proveedor adecuado en el registro de servicios.  
3. Reenvía la solicitud al Sistema 1\.  
4. Recibe el resultado y se lo devuelve al Sistema 2 original.  
5. Gestiona posibles errores como servicios desconocidos o parámetros inválidos.

## **Caso de Uso 5: Gestión de Registro de Servicios**

* **Actor:** Broker y Proveedores  
* **Descripción:** Mantenimiento del registro dinámico de proveedores para poder sustituirlos sin afectar al cliente.  
* **Flujo principal:**  
1. Los proveedores (como el Sistema 1\) se registran asociando un nombre de servicio con su instancia.  
2. El Broker mantiene esta lista actualizada y permite localizar al proveedor correcto durante una solicitud.
