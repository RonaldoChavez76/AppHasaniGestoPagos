# Diagrama entidad-relación (notación Chen)

Este diagrama utiliza el estilo de la referencia: rectángulos para entidades, rombos para relaciones y óvalos para atributos. Los atributos marcados como `(PK)` son claves primarias; `(UK)` indica una restricción de unicidad.

```mermaid
flowchart LR
    DOM[DOMICILIOS]
    R_TIENE{tiene}
    CLI[CLIENTES]
    R_AUT{autentica con}
    USR[USUARIOS]
    R_POSEE{posee}
    CTA[CUENTAS]
    R_FACIAL{registra}
    FAC[AUTENTICACIONES FACIALES]

    DOM ---|"0..1"| R_TIENE
    R_TIENE ---|"1"| CLI
    CLI ---|"1"| R_AUT
    R_AUT ---|"0..1"| USR
    CLI ---|"1"| R_POSEE
    R_POSEE ---|"0..N"| CTA
    USR ---|"1"| R_FACIAL
    R_FACIAL ---|"0..1"| FAC

    d_id(["id (PK)"])
    d_cliente(["cliente_id (FK, UK)"])
    d_calle(["calle"])
    d_num_ext(["numero_exterior"])
    d_num_int(["numero_interior"])
    d_colonia(["colonia"])
    d_municipio(["municipio"])
    d_estado(["estado"])
    d_cp(["codigo_postal"])
    d_pais(["pais"])

    d_id --- DOM
    d_cliente --- DOM
    d_calle --- DOM
    d_num_ext --- DOM
    d_num_int --- DOM
    d_colonia --- DOM
    d_municipio --- DOM
    d_estado --- DOM
    d_cp --- DOM
    d_pais --- DOM

    c_id(["id (PK)"])
    c_nombre(["primer_nombre"])
    c_segundo(["segundo_nombre"])
    c_apellido_p(["apellido_paterno"])
    c_apellido_m(["apellido_materno"])
    c_nacimiento(["fecha_nacimiento"])
    c_curp(["curp (UK)"])
    c_rfc(["rfc (UK)"])
    c_sexo(["sexo"])
    c_nacionalidad(["nacionalidad"])
    c_civil(["estado_civil"])
    c_correo(["correo (UK)"])
    c_tel(["telefono_movil"])
    c_tel_alt(["telefono_alternativo"])
    c_ocupacion(["ocupacion"])
    c_empresa(["empresa"])
    c_ingreso(["ingreso_mensual"])
    c_activo(["activo"])
    c_creacion(["fecha_creacion"])
    c_actualizacion(["fecha_actualizacion"])

    c_id --- CLI
    c_nombre --- CLI
    c_segundo --- CLI
    c_apellido_p --- CLI
    c_apellido_m --- CLI
    c_nacimiento --- CLI
    c_curp --- CLI
    c_rfc --- CLI
    c_sexo --- CLI
    c_nacionalidad --- CLI
    c_civil --- CLI
    c_correo --- CLI
    c_tel --- CLI
    c_tel_alt --- CLI
    c_ocupacion --- CLI
    c_empresa --- CLI
    c_ingreso --- CLI
    c_activo --- CLI
    c_creacion --- CLI
    c_actualizacion --- CLI

    u_id(["id (PK)"])
    u_cliente(["cliente_id (FK, UK)"])
    u_rol(["rol"])
    u_hash(["password_hash"])
    u_activo(["activo"])
    u_creacion(["fecha_creacion"])
    u_actualizacion(["fecha_actualizacion"])

    u_id --- USR
    u_cliente --- USR
    u_rol --- USR
    u_hash --- USR
    u_activo --- USR
    u_creacion --- USR
    u_actualizacion --- USR

    a_id(["id (PK)"])
    a_usuario(["usuario_id (FK, UK)"])
    a_proveedor(["proveedor"])
    a_referencia(["referencia_externa"])
    a_activo(["activo"])
    a_creacion(["fecha_creacion"])
    a_actualizacion(["fecha_actualizacion"])

    a_id --- FAC
    a_usuario --- FAC
    a_proveedor --- FAC
    a_referencia --- FAC
    a_activo --- FAC
    a_creacion --- FAC
    a_actualizacion --- FAC

    q_id(["id (PK)"])
    q_cliente(["cliente_id (FK)"])
    q_numero(["numero_cuenta (UK)"])
    q_saldo(["saldo"])
    q_estatus(["estatus"])
    q_apertura(["fecha_apertura"])
    q_actualizacion(["fecha_actualizacion"])

    q_id --- CTA
    q_cliente --- CTA
    q_numero --- CTA
    q_saldo --- CTA
    q_estatus --- CTA
    q_apertura --- CTA
    q_actualizacion --- CTA

    classDef entity fill:#cfe2f3,stroke:#315b7d,stroke-width:2px,color:#111,font-weight:bold
    classDef domicile fill:#d9ead3,stroke:#38761d,stroke-width:2px,color:#111,font-weight:bold
    classDef account fill:#f4cccc,stroke:#990000,stroke-width:2px,color:#111,font-weight:bold
    classDef user fill:#fce5cd,stroke:#b45f06,stroke-width:2px,color:#111,font-weight:bold
    classDef relation fill:#fff2cc,stroke:#bf9000,stroke-width:2px,color:#111
    classDef attribute fill:#ffffff,stroke:#6d9eeb,color:#111
    class DOM domicile
    class CLI entity
    class USR user
    class CTA account
    class FAC user
    class R_TIENE,R_AUT,R_POSEE,R_FACIAL relation
    class d_id,d_cliente,d_calle,d_num_ext,d_num_int,d_colonia,d_municipio,d_estado,d_cp,d_pais attribute
    class c_id,c_nombre,c_segundo,c_apellido_p,c_apellido_m,c_nacimiento,c_curp,c_rfc,c_sexo,c_nacionalidad,c_civil,c_correo,c_tel,c_tel_alt,c_ocupacion,c_empresa,c_ingreso,c_activo,c_creacion,c_actualizacion attribute
    class u_id,u_cliente,u_rol,u_hash,u_activo,u_creacion,u_actualizacion attribute
    class a_id,a_usuario,a_proveedor,a_referencia,a_activo,a_creacion,a_actualizacion attribute
    class q_id,q_cliente,q_numero,q_saldo,q_estatus,q_apertura,q_actualizacion attribute
```

## Notas del modelo

- El correo se almacena una sola vez en `clientes.correo`; el login lo obtiene a través de la relación entre usuario y cliente.
- `usuarios.rol` admite `CLIENTE` y `EJECUTIVO`. El registro público crea usuarios `CLIENTE`.
- `cliente_id` es único en `domicilios` y `usuarios`, por lo que cada cliente puede tener como máximo un domicilio y un usuario. El flujo de registro crea ambos.
- Una cuenta pertenece a un cliente; un cliente puede tener cero o más cuentas.
- La autenticación facial todavía no está integrada. La tabla solo representa la referencia que se asociaría con un proveedor externo; no almacena imágenes ni plantillas biométricas.
- En `autenticaciones_faciales`, la unicidad se aplica a la combinación `(proveedor, referencia_externa)`, no a cada campo por separado.
- La baja es lógica. Las claves foráneas restringen el borrado físico de registros relacionados.
