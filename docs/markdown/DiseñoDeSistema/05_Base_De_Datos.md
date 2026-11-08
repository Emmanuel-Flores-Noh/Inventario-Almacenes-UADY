# **Modelo de Base de Datos (MariaDB)**

El sistema utiliza MariaDB para la persistencia de datos. A continuación se definen las entidades principales que soportan el registro de productos, la generación de códigos QR (que incrustan el nombre del producto, su ID en la base de datos y su número de serie) y el seguimiento de las ventas en los distintos almacenes.  

## **Entidades y Atributos**

### **ROLES**

Catálogo independiente para definir jerarquías operativas.

* id\_rol (PK)  
* nombre\_rol (ej. Administrador Sistema 1, Proveedor Sistema 2\)  
* descripcion

### **USUARIOS**

Gestiona a los actores que interactúan con los sistemas.

* id\_usuario (PK)  
* id\_rol (FK)  
* nombres  
* apellido\_paterno  
* apellido\_materno

### **PRODUCTOS**

Almacena la información general del catálogo de inventario.

* id\_producto (PK)  
* nombre  
* descripcion  
* precio\_actual

### **HISTORIAL\_PRECIO**

Mantiene la auditoría de las fluctuaciones de costo de los productos.

* id\_historial (PK)  
* id\_producto (FK)  
* precio\_historico  
* fecha\_cambio

### **MERCANCIA**

Representa cada unidad física individual para su trazabilidad exacta.

* numero\_serie (PK)  
* id\_producto (FK)  
* estado (ej. en almacén, vendido)  
* ruta\_qr (Ubicación del archivo PNG del código QR)

### **TIENDAS\_ALMACENES**

Catálogo de los destinos físicos o sucursales de la UADY.

* id\_tienda (PK)  
* nombre  
* direccion

### **VENTAS**

Registra la petición de actualización y vincula la mercancía a su destino final.

* id\_venta (PK)  
* numero\_serie (FK)  
* id\_tienda (FK)  
* id\_usuario (FK)  
* fecha\_venta

![Diagrama entidad relacion](BaseDeDatos.mmd)

