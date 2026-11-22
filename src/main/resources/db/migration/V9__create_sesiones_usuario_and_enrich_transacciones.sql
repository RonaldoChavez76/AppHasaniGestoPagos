-- ==============================================================================
-- V9__create_sesiones_usuario_and_enrich_transacciones.sql
-- Control de sesión única activa entre plataformas (WEB / MOVIL) y enriquecimiento
-- de transacciones con nombre de producto para el historial de compras.
-- ==============================================================================

CREATE TABLE IF NOT EXISTS sesiones_usuario (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    cliente_id BIGINT NOT NULL,
    session_id VARCHAR(64) NOT NULL UNIQUE,
    plataforma VARCHAR(20) NOT NULL DEFAULT 'WEB',
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_inicio TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_expiracion TIMESTAMP NOT NULL,
    fecha_cierre TIMESTAMP,
    motivo_cierre VARCHAR(100),
    CONSTRAINT fk_sesiones_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_sesiones_usuario_activa
    ON sesiones_usuario (usuario_id, activa);

CREATE INDEX IF NOT EXISTS idx_sesiones_session_id
    ON sesiones_usuario (session_id);

ALTER TABLE gestopago_transacciones
    ADD COLUMN IF NOT EXISTS nombre_producto VARCHAR(255);

