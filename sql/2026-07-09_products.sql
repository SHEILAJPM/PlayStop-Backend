-- Tienda: productos del propietario, opcionalmente asignados a una sucursal
--
-- Este proyecto no usa Flyway/Liquibase (spring.jpa.hibernate.ddl-auto=none,
-- ver application.properties), asi que el esquema se gestiona con SQL manual
-- contra Neon. Ejecutar este script una sola vez antes de desplegar el
-- backend con estos cambios.

CREATE TABLE products (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(255) NOT NULL,
    price NUMERIC(10,2) NOT NULL,
    stock INTEGER NOT NULL,
    image_url TEXT,
    active BOOLEAN NOT NULL DEFAULT true,
    owner_id UUID NOT NULL REFERENCES users(id),
    branch_id UUID REFERENCES branches(id),
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_products_active ON products(active);
CREATE INDEX idx_products_owner ON products(owner_id);
CREATE INDEX idx_products_branch ON products(branch_id);
