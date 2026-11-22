-- ==============================================================================
-- V3__create_onboarding_tables.sql
-- Creación de tablas para el flujo de Onboarding de Personas Físicas
-- ==============================================================================

-- 1. Tabla: CLIENTES (Tabla principal)
CREATE TABLE clientes (
    id BIGSERIAL PRIMARY KEY,
    primer_nombre VARCHAR(50) NOT NULL,
    segundo_nombre VARCHAR(50),
    apellido_paterno VARCHAR(50) NOT NULL,
    apellido_materno VARCHAR(50),
    fecha_nacimiento DATE NOT NULL,
    curp VARCHAR(18) NOT NULL UNIQUE,
    rfc VARCHAR(13) NOT NULL UNIQUE,
    sexo VARCHAR(20) NOT NULL,
    nacionalidad VARCHAR(40) NOT NULL,
    estado_civil VARCHAR(30) NOT NULL,
    correo VARCHAR(100) NOT NULL UNIQUE,
    telefono_movil VARCHAR(10) NOT NULL,
    telefono_alternativo VARCHAR(10),
    ocupacion VARCHAR(80),
    empresa VARCHAR(100),
    ingreso_mensual NUMERIC(12, 2) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_clientes_primer_nombre CHECK (length(btrim(primer_nombre)) BETWEEN 2 AND 50),
    CONSTRAINT ck_clientes_segundo_nombre CHECK (
        segundo_nombre IS NULL OR length(btrim(segundo_nombre)) BETWEEN 2 AND 50
    ),
    CONSTRAINT ck_clientes_apellido_paterno CHECK (length(btrim(apellido_paterno)) BETWEEN 2 AND 50),
    CONSTRAINT ck_clientes_apellido_materno CHECK (
        apellido_materno IS NULL OR length(btrim(apellido_materno)) BETWEEN 2 AND 50
    ),
    CONSTRAINT ck_clientes_curp CHECK (curp ~ '^[A-Z][AEIOUX][A-Z]{2}[0-9]{6}[HM][A-Z]{5}[A-Z0-9][0-9]$'),
    CONSTRAINT ck_clientes_rfc CHECK (rfc ~ '^[A-Z&Ñ]{3,4}[0-9]{6}[A-Z0-9]{3}$'),
    CONSTRAINT ck_clientes_correo CHECK (
        correo = lower(btrim(correo))
        AND correo ~ '^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$'
    ),
    CONSTRAINT ck_clientes_telefono_movil CHECK (telefono_movil ~ '^[0-9]{10}$'),
    CONSTRAINT ck_clientes_telefono_alternativo CHECK (
        telefono_alternativo IS NULL OR telefono_alternativo ~ '^[0-9]{10}$'
    ),
    CONSTRAINT ck_clientes_ingreso_mensual CHECK (ingreso_mensual > 0),
    CONSTRAINT uq_clientes_id_correo UNIQUE (id, correo)
);

-- 2. Tabla: DOMICILIOS (Relación 1:1 con CLIENTES)
CREATE TABLE domicilios (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL UNIQUE,
    calle VARCHAR(100) NOT NULL,
    numero_exterior VARCHAR(20) NOT NULL,
    numero_interior VARCHAR(20),
    colonia VARCHAR(80) NOT NULL,
    municipio VARCHAR(80) NOT NULL,
    estado VARCHAR(60) NOT NULL,
    codigo_postal VARCHAR(5) NOT NULL,
    pais VARCHAR(50) NOT NULL DEFAULT 'México',
    CONSTRAINT ck_domicilios_codigo_postal CHECK (codigo_postal ~ '^[0-9]{5}$'),
    CONSTRAINT fk_domicilios_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE RESTRICT
);

-- 3. Tabla: USUARIOS (un usuario de acceso por cliente)
CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL UNIQUE,
    correo VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(60) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_usuarios_correo CHECK (
        correo = lower(btrim(correo))
        AND correo ~ '^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$'
    ),
    CONSTRAINT fk_usuarios_cliente_correo FOREIGN KEY (cliente_id, correo)
        REFERENCES clientes(id, correo) ON UPDATE CASCADE ON DELETE RESTRICT
);

-- 4. Tabla: AUTENTICACIONES_FACIALES
-- El proveedor conserva los datos biométricos; aquí solo se guarda su referencia opaca.
CREATE TABLE autenticaciones_faciales (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL UNIQUE,
    proveedor VARCHAR(50) NOT NULL,
    referencia_externa VARCHAR(255) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_autenticacion_facial_proveedor_referencia UNIQUE (proveedor, referencia_externa),
    CONSTRAINT fk_autenticaciones_faciales_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE RESTRICT
);

-- 5. Tabla: CUENTAS (un cliente puede tener varias cuentas)
CREATE TABLE cuentas (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    numero_cuenta VARCHAR(20) NOT NULL UNIQUE,
    saldo NUMERIC(14, 2) NOT NULL DEFAULT 0.00 CHECK (saldo >= 0),
    estatus VARCHAR(15) NOT NULL DEFAULT 'ACTIVA',
    fecha_apertura TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_cuentas_estatus CHECK (estatus IN ('ACTIVA', 'INACTIVA', 'BLOQUEADA', 'CERRADA')),
    CONSTRAINT fk_cuentas_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE RESTRICT
);

-- ==============================================================================
-- Creación de Índices para optimización de consultas frecuentes
-- ==============================================================================
CREATE INDEX idx_clientes_activos ON clientes(id) WHERE activo = TRUE;
CREATE INDEX idx_clientes_fecha_creacion ON clientes(fecha_creacion);
CREATE INDEX idx_cuentas_cliente ON cuentas(cliente_id);
CREATE INDEX idx_cuentas_activas ON cuentas(cliente_id) WHERE estatus = 'ACTIVA';