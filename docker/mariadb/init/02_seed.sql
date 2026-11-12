-- Datos de prueba para el Sistema de Inventario y Trazabilidad UADY.
-- Se ejecuta despues de 01_schema.sql cuando Docker crea el volumen por primera vez.
-- Es idempotente: usa IDs explicitos y ON DUPLICATE KEY UPDATE para poder aplicarse
-- manualmente sobre una base ya existente sin duplicar filas.

USE almacenes_uady;

START TRANSACTION;

-- 1. ROLES
INSERT INTO roles (id_rol, nombre_rol, descripcion) VALUES
    (1, 'Administrador Sistema 1', 'Acceso total al sistema centralizado y a la persistencia'),
    (2, 'Proveedor Sistema 2', 'Operacion de ventas y consulta desde el cliente de escritorio'),
    (3, 'Auditor de Inventario', 'Consulta de inventario y trazabilidad sin modificaciones')
ON DUPLICATE KEY UPDATE
    nombre_rol = VALUES(nombre_rol),
    descripcion = VALUES(descripcion);

-- 2. USUARIOS
INSERT INTO usuarios (id_usuario, id_rol, nombres, apellido_paterno, apellido_materno) VALUES
    (1, 1, 'Ana', 'Pineda', 'Solis'),
    (2, 2, 'Carlos', 'Mendez', 'Ruiz'),
    (3, 3, 'Lucero', 'Chan', 'Canul'),
    (4, 2, 'Emmanuel', 'Flores', 'Noh')
ON DUPLICATE KEY UPDATE
    id_rol = VALUES(id_rol),
    nombres = VALUES(nombres),
    apellido_paterno = VALUES(apellido_paterno),
    apellido_materno = VALUES(apellido_materno);

-- 3. PRODUCTOS
INSERT INTO productos (id_producto, nombre, descripcion, precio_actual) VALUES
    (1, 'Laptop Lenovo ThinkPad E14', 'Equipo de computo para laboratorio de ingenieria', 18999.00),
    (2, 'Proyector Epson PowerLite', 'Proyector portatil para aulas y auditorios', 12500.50),
    (3, 'Silla ergonomica', 'Silla con soporte lumbar para oficina', 3499.00),
    (4, 'Impresora laser HP', 'Impresora monocromatica de red', 4200.00),
    (5, 'Tablet Samsung Galaxy Tab', 'Tablet de 11 pulgadas para consulta de campo', 7899.99)
ON DUPLICATE KEY UPDATE
    nombre = VALUES(nombre),
    descripcion = VALUES(descripcion),
    precio_actual = VALUES(precio_actual);

-- 4. TIENDAS_ALMACENES
INSERT INTO tiendas_almacenes (id_tienda, nombre, direccion) VALUES
    (1, 'Almacen Central UADY', 'Calle 60 s/n, Centro, Merida, Yucatan'),
    (2, 'Tienda Facultad de Ingenieria', 'Periferico Norte, Campus de Ingenieria, Merida'),
    (3, 'Tienda Facultad de Matematicas', 'Campus de Ciencias Exactas, Merida')
ON DUPLICATE KEY UPDATE
    nombre = VALUES(nombre),
    direccion = VALUES(direccion);

-- 5. MERCANCIA
-- Mezcla de piezas en almacen y vendidas para poder probar ambos estados.
INSERT INTO mercancia (numero_serie, id_producto, estado, ruta_qr) VALUES
    ('SN-UADY-0001', 1, 'EN_ALMACEN', '/app/storage/qr/SN-UADY-0001.png'),
    ('SN-UADY-0002', 1, 'VENDIDO',    '/app/storage/qr/SN-UADY-0002.png'),
    ('SN-UADY-0003', 2, 'EN_ALMACEN', '/app/storage/qr/SN-UADY-0003.png'),
    ('SN-UADY-0004', 2, 'VENDIDO',    '/app/storage/qr/SN-UADY-0004.png'),
    ('SN-UADY-0005', 3, 'EN_ALMACEN', '/app/storage/qr/SN-UADY-0005.png'),
    ('SN-UADY-0006', 3, 'EN_ALMACEN', '/app/storage/qr/SN-UADY-0006.png'),
    ('SN-UADY-0007', 4, 'EN_ALMACEN', '/app/storage/qr/SN-UADY-0007.png'),
    ('SN-UADY-0008', 4, 'VENDIDO',    '/app/storage/qr/SN-UADY-0008.png'),
    ('SN-UADY-0009', 5, 'EN_ALMACEN', '/app/storage/qr/SN-UADY-0009.png'),
    ('SN-UADY-0010', 5, 'VENDIDO',    '/app/storage/qr/SN-UADY-0010.png')
ON DUPLICATE KEY UPDATE
    id_producto = VALUES(id_producto),
    estado = VALUES(estado),
    ruta_qr = VALUES(ruta_qr);

COMMIT;
