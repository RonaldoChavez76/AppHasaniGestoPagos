-- El correo de acceso se obtiene desde clientes.correo a través de cliente_id.
ALTER TABLE usuarios
    DROP CONSTRAINT IF EXISTS fk_usuarios_cliente_correo;

ALTER TABLE usuarios
    DROP COLUMN correo;
