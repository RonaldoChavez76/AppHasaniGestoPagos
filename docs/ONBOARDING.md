# Onboarding de clientes persona física

## Alcance implementado

El flujo permite registrar un cliente persona física con domicilio, un usuario de acceso y una cuenta bancaria activa. El alta se ejecuta dentro de una transacción: si falla cualquiera de las escrituras, se revierte el flujo completo. El saldo inicial actual se define en el servicio como `0.00`.

Las contraseñas se almacenan mediante BCrypt. El login devuelve un token JWT firmado con HMAC-SHA-256 y vigencia de 15 minutos. Los endpoints del dominio de onboarding exigen `Authorization: Bearer <token>`, salvo `POST /clientes` y `POST /auth/login`.

El correo de acceso es `clientes.correo`; no se duplica en `usuarios`. Un cliente puede tener a lo sumo un usuario y a lo sumo un domicilio, y puede tener varias cuentas. Los usuarios nuevos reciben el rol `CLIENTE`; ningún endpoint público permite cambiar el rol.

## Migraciones

Flyway administra el esquema en `src/main/resources/db/migration`:

| Versión | Propósito |
| --- | --- |
| V1 | Tokens de integración GestoPago |
| V2 | Catálogo de productos GestoPago |
| V3 | Clientes, domicilios, usuarios, autenticaciones faciales y cuentas |
| V4 | Elimina el correo duplicado de `usuarios`; el correo de acceso procede de `clientes` |
| V5 | Agrega roles de usuario y elimina una restricción compuesta redundante |

V3, V4 y V5 se aplicaron y verificaron en la base local `PWA`. V4 preserva el correo de cliente y no elimina usuarios, contraseñas ni otras tablas. V5 asigna `CLIENTE` a los usuarios existentes y elimina la restricción redundante. Para conocer el historial de otro entorno, consulta `flyway_schema_history`.

## Alcance acordado para la adecuación de la API

La especificación se interpretará como administración de clientes persona física, no como administración independiente de credenciales. Los filtros `nombre`, `RFC` y `CURP` pertenecen a `clientes`; `usuarios` conserva las credenciales y el rol asociado a cada cliente, y `cuentas` conserva los productos bancarios relacionados.

La búsqueda combinada está implementada en `POST /clientes/busqueda`: recibe filtros y opciones de página en un Request DTO validado con `@Valid`. Los filtros proporcionados se combinan con AND; el nombre usa coincidencia parcial sin distinguir mayúsculas, mientras CURP, RFC, correo y número de cuenta usan coincidencia exacta. También permite filtrar por ID, estado activo e intervalo inclusivo de fechas de registro. La respuesta es paginada y no devuelve entidades JPA.

Las operaciones de consulta individual, actualización, baja, consulta de cuentas y operaciones de usuario usan rutas estáticas y reciben sus identificadores y criterios mediante Request DTOs en el cuerpo. La actualización sigue siendo completa (`PUT`), con los campos controlados por el servidor excluidos del DTO. La autorización de cliente propio y ejecutivo se conserva y la propiedad se verifica en los controladores después de extraer el ID del cuerpo.

La baja mantiene la regla ya implementada: se conserva la información, se desactiva el cliente, su usuario y sus cuentas activas; un usuario inactivo no puede iniciar sesión ni seguir usando sus tokens.

### Contratos Request DTO propuestos

Los DTO de contrato están definidos en `model/onboarding`:

| DTO | Uso previsto | Contenido y validación |
| --- | --- | --- |
| `BuscarClientesRequest` | Búsqueda paginada combinable | Filtros opcionales `nombre`, `rfc`, `curp`, `correo`, `numeroCuenta`, `clienteId`, `activo`, `registradoDesde` y `registradoHasta`; `page` y `size` predeterminados 0/20 y máximo 100. RFC/CURP se normalizan a mayúsculas y se validan por formato. |
| `ConsultarClienteRequest` | Consulta individual | `clienteId` obligatorio y positivo. |
| `ActualizarClienteConIdRequest` | Actualización completa | `clienteId` obligatorio y positivo y `datos` anidados, validados reutilizando `ActualizarClienteRequest`. |
| `DarDeBajaClienteRequest` | Baja lógica | `clienteId` obligatorio y positivo. |
| `BuscarCuentasRequest` | Consulta paginada de cuentas | `numeroCuenta`, `clienteId`, `soloActivas`, `page` y `size`; sin número de cuenta devuelve cuentas activas por defecto. |
| `UsuarioIdRequest` | Consulta de usuario | `usuarioId` obligatorio y positivo. |
| `CambiarPasswordUsuarioRequest` | Cambio de contraseña | `usuarioId` y el objeto `cambio`, validado reutilizando `CambiarPasswordRequest`. |

Los DTO no reciben campos de servidor como rol, CURP/RFC de actualización, saldo ni hashes de contraseña. El número de cuenta se admite únicamente como filtro de consulta, nunca para cambiar la cuenta asociada.

## API de onboarding

Todas las operaciones HTTP activas usan el envoltorio `ApiResponse<T>` para respuestas exitosas y errores:

```json
{
  "status": 200,
  "message": "Cliente consultado correctamente.",
  "data": {},
  "timestamp": "2026-10-16T12:00:00Z"
}
```

En errores, `data` es `null` y `status` coincide con el código HTTP. Los resultados paginados y los DTO propios de cada operación se encuentran dentro de `data`. El registro devuelve HTTP 201; las operaciones exitosas con efecto lateral devuelven HTTP 200 y el mismo formato. Las contraseñas no se incluyen en ninguna respuesta.

| Método y ruta | Descripción | Acceso |
| --- | --- | --- |
| `POST /clientes` | Registra cliente, domicilio, usuario y cuenta activa | Público; crea rol CLIENTE |
| `POST /clientes/busqueda` | Busca clientes con filtros combinables y paginación en el cuerpo JSON; acepta nombre, RFC, CURP, correo, cuenta, ID, estado activo y fechas de registro | JWT de EJECUTIVO; cliente solo puede consultar por su número de cuenta |
| `POST /clientes/consulta` | Consulta por `clienteId` enviado en el DTO | JWT del propio cliente o de EJECUTIVO |
| `PUT /clientes/actualizar` | Actualiza datos personales, contacto, empleo y domicilio; recibe `clienteId` y `datos`, no acepta CURP ni RFC | Propio cliente o EJECUTIVO |
| `DELETE /clientes/baja` | Recibe `clienteId`; da de baja al cliente y desactiva usuario y cuentas activas | Propio cliente o EJECUTIVO |
| `POST /cuentas/busqueda` | Consulta cuentas paginadas; `CuentaResponse` incluye número, estado y saldo | JWT de EJECUTIVO; cliente requiere el número de su propia cuenta |
| `POST /auth/login` | Autentica por correo y contraseña y emite JWT | Público |
| `POST /usuarios/consulta` | Consulta por `usuarioId`, sin datos sensibles | JWT de EJECUTIVO o del mismo usuario |
| `PUT /usuarios/password` | Recibe `usuarioId` y `cambio`; verifica la contraseña actual | JWT del mismo usuario |

El interceptor recupera el rol vigente desde la base de datos en cada solicitud, además de comprobar que el usuario y el cliente sigan activos. El controlador valida los identificadores recibidos en el cuerpo: un cliente solo puede consultar, actualizar o dar de baja su propio perfil, y consultar sus propias cuentas. Un ejecutivo puede consultar el DTO no sensible de cualquier usuario; el cambio de contraseña sigue limitado al propio usuario incluso para un ejecutivo.

No intercambies los cuerpos de los endpoints: `POST /clientes/busqueda` recibe filtros de cliente y `POST /usuarios/consulta` recibe `usuarioId`. Swagger ofrece ejemplos en el selector **Example Value** de cada Request Body.

Para listar todos los clientes, autentícate con el botón **Authorize** usando un token de usuario EJECUTIVO y envía `{"page":0,"size":20}` a `/clientes/busqueda`. Un cliente normal solo puede buscar por el número de su propia cuenta; filtros globales y listados generales devuelven `403` con un mensaje que explica este requisito.

Para consultar `/usuarios/consulta`, envía el ID de usuario (no el ID del cliente) en `{"usuarioId":1}`. Un EJECUTIVO puede consultar cualquier usuario; un CLIENTE solo el asociado a su token. Si Swagger sigue mostrando `rangoFechasValido`, reinicia la aplicación y vuelve a cargar `/v3/api-docs` o Swagger UI: ese campo es un método de validación interno y no pertenece al JSON Request.

Ejemplo para `POST /clientes/busqueda`:

```json
{
  "nombre": "ana lo",
  "rfc": "ABCD010101ABC",
  "curp": null,
  "correo": null,
  "numeroCuenta": null,
  "clienteId": null,
  "activo": true,
  "registradoDesde": null,
  "registradoHasta": null,
  "page": 0,
  "size": 20
}
```

Los filtros no nulos se combinan con AND. Cada término del nombre se busca como subcadena, sin distinguir mayúsculas, en nombre(s) y apellidos; así, `ana lo` puede encontrar clientes cuyo nombre contenga `Ana` y cuyo apellido contenga `Lo`. RFC, CURP, correo y número de cuenta son coincidencias exactas. Se pueden omitir filtros; un cuerpo vacío `{}` devuelve la primera página de todos los clientes, disponible para ejecutivos.

Los ejemplos siguientes representan el objeto enviado dentro de `data`, excepto los request bodies que se indican explícitamente.

Ejemplo de `POST /clientes/consulta`:

```json
{
  "clienteId": 12
}
```

Ejemplo de `PUT /clientes/actualizar`:

```json
{
  "clienteId": 12,
  "datos": {
    "primerNombre": "Ronaldo",
    "segundoNombre": null,
    "apellidoPaterno": "Chávez",
    "apellidoMaterno": "López",
    "fechaNacimiento": "1998-05-20",
    "sexo": "M",
    "nacionalidad": "Mexicana",
    "estadoCivil": "Soltero",
    "correo": "ronaldo@example.com",
    "telefonoMovil": "5512345678",
    "telefonoAlternativo": null,
    "ocupacion": "Desarrollador",
    "empresa": "Empresa",
    "ingresoMensual": 25000,
    "domicilio": {
      "calle": "Reforma",
      "numeroExterior": "10",
      "numeroInterior": null,
      "colonia": "Centro",
      "municipio": "Ciudad de México",
      "estado": "CDMX",
      "codigoPostal": "06000",
      "pais": "México"
    }
  }
}
```

Ejemplo de `POST /cuentas/busqueda` para consultar el saldo y el estado de una cuenta:

```json
{
  "numeroCuenta": "12345678901234567890",
  "clienteId": null,
  "soloActivas": null,
  "page": 0,
  "size": 20
}
```

Para `POST /usuarios/consulta`, enviar `{"usuarioId": 12}`. En las operaciones disponibles para clientes, el servidor valida que el identificador corresponda al propietario autenticado; el cliente no puede ampliar su acceso cambiando los IDs del cuerpo.

### Alta controlada de ejecutivos

El registro público siempre crea usuarios `CLIENTE`. La asignación de `EJECUTIVO` requiere un canal administrativo seguro con acceso restringido a la base de datos; nunca se debe agregar el rol al JSON de registro ni permitir una promoción pública. Ejemplo para una base local, reemplazando `123` por el ID verificado:

```sql
UPDATE usuarios SET rol = 'EJECUTIVO' WHERE id = 123;
```

No se crea un ejecutivo predeterminado ni una contraseña compartida. La aplicación aún no incluye un flujo administrativo para aprovisionar ejecutivos.

### Ejemplo de login

En Swagger UI abre `POST /auth/login`, usa **Try it out** y autentícate. Copia únicamente el valor de `accessToken` de la respuesta. Pulsa **Authorize** y pégalo sin escribir `Bearer `; Swagger agrega ese prefijo automáticamente para las operaciones marcadas con seguridad Bearer. El registro y el login son públicos.

```json
{
  "correo": "cliente@example.com",
  "password": "ContraseñaSegura1!"
}
```

La respuesta contiene `accessToken`, `tokenType` (`Bearer`), `expiresIn` y `expiresAt`. Enviar el token en la cabecera `Authorization` de las operaciones protegidas.

### Cambio de contraseña

```json
{
  "usuarioId": 12,
  "cambio": {
    "passwordActual": "ContraseñaAnterior1!",
    "passwordNueva": "ContraseñaNueva2!"
  }
}
```

La contraseña nueva requiere mínimo 8 caracteres, mayúscula, minúscula, dígito y símbolo. También se rechazan contraseñas superiores a 72 bytes UTF-8, el límite de BCrypt.

El resultado exitoso de cambio de contraseña conserva la envoltura común, con `data: null`.

## Validaciones y reglas

- Nombre y apellidos requeridos, longitud entre 2 y 50 caracteres y solo letras/espacios. El segundo nombre es opcional; como el enunciado no marca como opcionales el apellido materno, la ocupación ni la empresa, estos se requieren en registro y actualización.
- Los textos se recortan en los extremos antes de validar, por lo que `"  Ronaldo  "` se procesa como `"Ronaldo"` y los espacios exteriores no cuentan para los límites. Los campos opcionales compuestos solo por espacios se tratan como ausentes; los obligatorios vacíos o con espacios solamente se rechazan.
- CURP y RFC se normalizan a mayúsculas, se validan por formato y deben ser únicos.
- Correo se normaliza a minúsculas, se valida y es único.
- El cliente debe ser mayor de edad.
- Teléfonos tienen 10 dígitos y código postal 5.
- Ingreso debe ser positivo; el saldo no puede ser negativo. Los importes deben caber en la precisión decimal definida por la API y la base.
- Número de cuenta es único y el estatus inicial es `ACTIVA`.
- Los campos requeridos ausentes o nulos, JSON inválido, fechas con formato incorrecto, cadenas en campos numéricos y valores que no cumplen el formato se rechazan con error de solicitud inválida; los duplicados se responden como conflicto.
- Las contraseñas no se guardan en texto plano. Se valida el límite de 72 bytes UTF-8 de BCrypt antes de comparar o cifrar para evitar entradas demasiado largas.
- Credenciales incorrectas se rechazan; cliente o usuario inactivo no puede iniciar sesión.
- La baja lógica desactiva cliente, usuario y cuentas activas, y los tokens dejan de autorizar solicitudes cuando el usuario o cliente se desactiva.
- Las listas aceptan `page` (base cero) y `size` (1 a 100); el tamaño predeterminado es 20 y el orden es ascendente por ID.
- Las fechas de nacimiento se envían como `AAAA-MM-DD`; la API no requiere ni acepta una hora en ese campo.

La API también valida los datos en la aplicación. Las restricciones SQL de V3 cubren formatos y unicidad, pero las reglas que requieren otras filas relacionadas —por ejemplo, consistencia de estado activo entre cliente y cuenta— se hacen cumplir en la lógica de servicio.

## Configuración local

Define en `.env`:

```properties
DB_PASSWORD=...
JWT_SECRET=<clave aleatoria Base64 de al menos 32 bytes>
GESTOPAGO_AUTH_PASSWORD=...
GESTOPAGO_API_KEY=...
```

El archivo `.env` está excluido de Git. No compartir ni registrar el secreto JWT. El valor local se puede generar con un generador criptográfico, por ejemplo:

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
[Convert]::ToBase64String($bytes)
```

El secreto debe ser idéntico en todas las instancias que validen los mismos tokens. Rotarlo invalida los JWT existentes.

## Pruebas

Ejecutar toda la suite:

```powershell
.\gradlew.bat test
```

La suite cubre registro transaccional, normalización del correo, contraseña BCrypt, edad, duplicados, login correcto/incorrecto, estados inactivos, emisión y validación del JWT, autorización del interceptor, consulta y actualización de cliente/cuenta y cambio de contraseña. Los resultados reproducibles se generan en `build/reports/tests/test/index.html`.

## Autenticación facial

La tabla `autenticaciones_faciales` solo almacena proveedor y referencia externa. No se almacenan imágenes ni plantillas biométricas. El endpoint y el verificador facial externo todavía no están implementados: es necesario elegir un proveedor compatible con detección de vida, verificar cómo acredita una autenticación exitosa y definir la gestión de consentimiento y revocación. No se debe emitir JWT basándose únicamente en una referencia enviada por el cliente.

## Diagrama

Consulta el [diagrama entidad-relación](./DIAGRAMA_ER.md).
