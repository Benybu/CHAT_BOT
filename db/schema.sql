CREATE DATABASE IF NOT EXISTS chatbot_marketplace
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE chatbot_marketplace;

CREATE TABLE IF NOT EXISTS usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario VARCHAR(80) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    rol ENUM('ADMIN','LECTOR') NOT NULL DEFAULT 'LECTOR',
    activo TINYINT(1) NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS productos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    precio DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    stock INT NOT NULL DEFAULT 0,
    descripcion VARCHAR(255) NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS respuestas (
    id INT AUTO_INCREMENT PRIMARY KEY,
    palabras_clave VARCHAR(255) NOT NULL,
    respuesta TEXT NOT NULL,
    activo TINYINT(1) NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS mensajes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    mensaje_entrada TEXT NOT NULL,
    respuesta_generada TEXT NOT NULL,
    canal VARCHAR(30) NOT NULL DEFAULT 'WEB',
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO usuarios(usuario, password, rol, activo)
VALUES ('admin', 'admin123', 'ADMIN', 1),
       ('lector', 'lector123', 'LECTOR', 1)
ON DUPLICATE KEY UPDATE usuario = usuario;

INSERT INTO productos(nombre, precio, stock, descripcion, activo)
VALUES ('Monitor 24 pulgadas', 450.00, 8, 'Monitor LED Full HD', 1),
       ('Teclado mecánico', 180.00, 15, 'Switch azul', 1),
       ('Mouse gamer', 75.00, 20, 'RGB y 3200 DPI', 1);

INSERT INTO respuestas(palabras_clave, respuesta, activo)
VALUES ('hola,buenas,hello', 'Hola, gracias por escribirnos. ¿En qué te ayudamos?', 1),
       ('envio,entrega,delivery', 'Sí realizamos delivery y también recojo en tienda.', 1),
       ('garantia,garantía', 'Todos nuestros productos cuentan con garantía.', 1);
