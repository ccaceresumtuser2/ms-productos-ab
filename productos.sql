-- Tabla productos + data de prueba (PostgreSQL)

DROP TABLE IF EXISTS productos;

CREATE TABLE productos (
    id              SERIAL PRIMARY KEY,
    codigo          VARCHAR(20)    NOT NULL UNIQUE,
    nombre          VARCHAR(100)   NOT NULL,
    descripcion     VARCHAR(255),
    categoria       VARCHAR(50)    NOT NULL,
    precio          NUMERIC(12,2)  NOT NULL CHECK (precio >= 0),
    stock           INTEGER        NOT NULL DEFAULT 0 CHECK (stock >= 0),
    activo          BOOLEAN        NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO productos (codigo, nombre, descripcion, categoria, precio, stock, activo) VALUES
('PRD-001', 'Notebook Lenovo IdeaPad 3',   'Notebook 15.6" Ryzen 5, 16GB RAM, 512GB SSD', 'Computación', 549990, 15, TRUE),
('PRD-002', 'Mouse Logitech M170',         'Mouse inalámbrico USB',                        'Accesorios',    9990,  120, TRUE),
('PRD-003', 'Teclado Mecánico Redragon',   'Teclado mecánico RGB switch blue',             'Accesorios',   34990,  45, TRUE),
('PRD-004', 'Monitor Samsung 24"',         'Monitor LED Full HD 75Hz',                     'Computación', 119990,  30, TRUE),
('PRD-005', 'Audífonos Sony WH-CH520',     'Audífonos Bluetooth on-ear',                   'Audio',        44990,  60, TRUE),
('PRD-006', 'Parlante JBL Flip 6',         'Parlante portátil resistente al agua',         'Audio',        99990,  25, TRUE),
('PRD-007', 'Smartphone Samsung A55',      '128GB, 8GB RAM, 5G',                           'Telefonía',   379990,  20, TRUE),
('PRD-008', 'Cargador USB-C 25W',          'Cargador rápido Samsung',                      'Telefonía',    14990, 200, TRUE),
('PRD-009', 'Disco SSD Kingston 1TB',      'SSD NVMe M.2 PCIe 4.0',                        'Almacenamiento',59990, 40, TRUE),
('PRD-010', 'Pendrive SanDisk 64GB',       'USB 3.2',                                      'Almacenamiento', 7990, 300, TRUE),
('PRD-011', 'Impresora HP Smart Tank 580', 'Multifuncional tinta continua WiFi',           'Impresión',   159990,  10, TRUE),
('PRD-012', 'Webcam Logitech C920',        'Cámara Full HD 1080p',                         'Accesorios',   69990,   0, FALSE),
('PRD-013', 'Tablet Xiaomi Pad 6',         '11", 128GB, 6GB RAM',                          'Computación', 299990,  12, TRUE),
('PRD-014', 'Router TP-Link Archer AX23',  'Router WiFi 6 doble banda',                    'Redes',        54990,  35, TRUE),
('PRD-015', 'Smartwatch Amazfit Bip 5',    'Reloj inteligente con GPS',                    'Wearables',    69990,  18, TRUE);
