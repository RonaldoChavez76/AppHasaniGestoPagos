CREATE TABLE IF NOT EXISTS gestopago_catalog_products (
    id            BIGSERIAL PRIMARY KEY,
    product_id    VARCHAR(100) NOT NULL UNIQUE,
    code          VARCHAR(100) NOT NULL,
    name          VARCHAR(255) NOT NULL,
    price         NUMERIC(19, 4),
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_gestopago_catalog_products_product_id
    ON gestopago_catalog_products (product_id);
