CREATE TABLE IF NOT EXISTS seguridad_dos_factor_otp (
    id BIGSERIAL PRIMARY KEY,
    numero_cuenta VARCHAR(20) NOT NULL,
    codigo_otp VARCHAR(10) NOT NULL,
    expira_en TIMESTAMP NOT NULL,
    consumido BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_2fa_cuenta_consumido ON seguridad_dos_factor_otp(numero_cuenta, consumido);
