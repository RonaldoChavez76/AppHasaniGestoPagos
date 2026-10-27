ALTER TABLE gestopago_transacciones
    ADD COLUMN IF NOT EXISTS numero_cuenta VARCHAR(20);

CREATE INDEX IF NOT EXISTS idx_gestopago_transacciones_cuenta
    ON gestopago_transacciones (numero_cuenta);
