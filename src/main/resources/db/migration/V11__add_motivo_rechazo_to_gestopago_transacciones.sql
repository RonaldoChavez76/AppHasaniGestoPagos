ALTER TABLE gestopago_transacciones
    ADD COLUMN IF NOT EXISTS motivo_rechazo VARCHAR(500);

CREATE INDEX IF NOT EXISTS idx_gestopago_transacciones_cuenta_estado_fecha
    ON gestopago_transacciones (numero_cuenta, estado, created_at DESC);
