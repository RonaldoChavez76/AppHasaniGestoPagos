CREATE TABLE IF NOT EXISTS gestopago_transacciones (
    id                  BIGSERIAL PRIMARY KEY,
    upc                 VARCHAR(64)     NOT NULL UNIQUE,
    tipo_operacion      VARCHAR(30)     NOT NULL,
    id_producto         INTEGER         NOT NULL,
    id_servicio         INTEGER         NOT NULL,
    telefono            VARCHAR(30)     NOT NULL,
    referencia          VARCHAR(100),
    monto_pago          NUMERIC(19, 4),
    hora_local          VARCHAR(20)     NOT NULL,
    unidad              VARCHAR(50)     NOT NULL,
    id_tx               VARCHAR(100),
    num_autorizacion    VARCHAR(100),
    codigo_respuesta    VARCHAR(10),
    texto_respuesta     VARCHAR(500),
    saldo               VARCHAR(50),
    saldo_formateado    VARCHAR(50),
    comision            VARCHAR(50),
    comision_formateada VARCHAR(50),
    fecha_transaccion   VARCHAR(50),
    pin                 TEXT,
    legend              TEXT,
    estado              VARCHAR(30)     NOT NULL,
    raw_request_payload TEXT,
    raw_response_xml    TEXT,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_gestopago_transacciones_upc
    ON gestopago_transacciones (upc);

CREATE INDEX IF NOT EXISTS idx_gestopago_transacciones_tipo_fecha
    ON gestopago_transacciones (tipo_operacion, created_at);

CREATE INDEX IF NOT EXISTS idx_gestopago_transacciones_id_tx
    ON gestopago_transacciones (id_tx);

CREATE INDEX IF NOT EXISTS idx_gestopago_transacciones_autorizacion
    ON gestopago_transacciones (num_autorizacion);
