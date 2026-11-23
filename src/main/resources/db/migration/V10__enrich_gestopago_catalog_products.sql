ALTER TABLE gestopago_catalog_products
    ADD COLUMN IF NOT EXISTS servicio VARCHAR(100),
    ADD COLUMN IF NOT EXISTS id_servicio VARCHAR(50),
    ADD COLUMN IF NOT EXISTS tipo_front VARCHAR(20),
    ADD COLUMN IF NOT EXISTS legend TEXT;

UPDATE gestopago_catalog_products
SET active = TRUE
WHERE active = FALSE;
