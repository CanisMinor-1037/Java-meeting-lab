-- 创建数据库
CREATE DATABASE IF NOT EXISTS javalab;
USE javalab;

-- 创建products表
CREATE TABLE IF NOT EXISTS cache_breakdown_products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 插入测试数据
INSERT INTO cache_breakdown_products (name, price, stock) VALUES
('iPhone 15', 999.99, 100),
('MacBook Pro', 1999.99, 50),
('AirPods Pro', 249.99, 200),
('iPad Air', 599.99, 75),
('Apple Watch', 399.99, 150),
('MacBook Air', 1299.99, 80),
('iMac', 1499.99, 30),
('HomePod', 299.99, 100); 