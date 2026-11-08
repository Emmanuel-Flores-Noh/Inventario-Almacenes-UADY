-- Inicialización de Base de Datos para el Sistema de Inventario y Trazabilidad UADY
CREATE DATABASE IF NOT EXISTS almacenes_uady CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE almacenes_uady;

-- 1. ROLES
CREATE TABLE IF NOT EXISTS roles (
    id_rol INT AUTO_INCREMENT PRIMARY KEY,
    nombre_rol VARCHAR(50) NOT NULL UNIQUE,
    descripcion VARCHAR(255)
);

-- 2. USUARIOS
CREATE TABLE IF NOT EXISTS usuarios (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    id_rol INT NOT NULL,
    nombres VARCHAR(100) NOT NULL,
    apellido_paterno VARCHAR(100) NOT NULL,
    apellido_materno VARCHAR(100),
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES roles(id_rol)
);

-- 3. PRODUCTOS
CREATE TABLE IF NOT EXISTS productos (
    id_producto INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    precio_actual DECIMAL(10, 2) NOT NULL
);

-- 4. HISTORIAL_PRECIO
CREATE TABLE IF NOT EXISTS historial_precio (
    id_historial INT AUTO_INCREMENT PRIMARY KEY,
    id_producto INT NOT NULL,
    precio_historico DECIMAL(10, 2) NOT NULL,
    fecha_cambio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_historial_producto FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
);

-- 5. MERCANCIA
CREATE TABLE IF NOT EXISTS mercancia (
    numero_serie VARCHAR(100) PRIMARY KEY,
    id_producto INT NOT NULL,
    estado VARCHAR(50) NOT NULL DEFAULT 'EN_ALMACEN',
    ruta_qr VARCHAR(255),
    CONSTRAINT fk_mercancia_producto FOREIGN KEY (id_producto) REFERENCES productos(id_producto)
);

-- 6. TIENDAS_ALMACENES
CREATE TABLE IF NOT EXISTS tiendas_almacenes (
    id_tienda INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    direccion VARCHAR(255) NOT NULL
);

-- 7. VENTAS
CREATE TABLE IF NOT EXISTS ventas (
    id_venta INT AUTO_INCREMENT PRIMARY KEY,
    numero_serie VARCHAR(100) NOT NULL,
    id_tienda INT NOT NULL,
    id_usuario INT NOT NULL,
    fecha_venta TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_venta_mercancia FOREIGN KEY (numero_serie) REFERENCES mercancia(numero_serie),
    CONSTRAINT fk_venta_tienda FOREIGN KEY (id_tienda) REFERENCES tiendas_almacenes(id_tienda),
    CONSTRAINT fk_venta_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario)
);
