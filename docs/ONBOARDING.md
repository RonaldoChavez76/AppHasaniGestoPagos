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

## API de onboarding

Todas las respuestas de cliente y usuario son DTO y no incluyen `password_hash`.

| Método y ruta | Descripción | Acceso |
| --- | --- | --- |
| `POST /clientes` | Registra cliente, domicilio, usuario y cuenta activa | Público; crea rol CLIENTE |
| `GET /clientes?page=0&size=20` | Lista clientes paginados | JWT de EJECUTIVO |
| `GET /clientes/{id}` | Consulta por ID | JWT del propio cliente o de EJECUTIVO |
| `GET /clientes/curp/{curp}` | Consulta por CURP | JWT de EJECUTIVO |
| `GET /clientes/rfc/{rfc}` | Consulta por RFC | JWT de EJECUTIVO |
| `GET /clientes/correo/{correo}` | Consulta por correo | JWT de EJECUTIVO |
| `GET /clientes/cuenta/{numeroCuenta}` | Busca el cliente titular | Propietario de la cuenta o EJECUTIVO |
| `GET /clientes/activos?page=0&size=20` | Lista clientes activos paginados | JWT de EJECUTIVO |
| `GET /clientes/registrados?desde=AAAA-MM-DD&hasta=AAAA-MM-DD&page=0&size=20` | Filtra por fechas inclusivas y pagina los resultados | JWT de EJECUTIVO |
| `PUT /clientes/{id}` | Actualiza datos personales, contacto, empleo y domicilio; no acepta CURP ni RFC | Propio cliente o EJECUTIVO |
| `DELETE /clientes/{id}` | Da de baja al cliente y desactiva usuario y cuentas activas | Propio cliente o JWT de EJECUTIVO |
| `GET /cuentas/{numeroCuenta}` | Consulta una cuenta | Propietario de la cuenta o EJECUTIVO |
| `GET /cuentas/{numeroCuenta}/saldo` | Consulta el saldo | Propietario de la cuenta o EJECUTIVO |
| `GET /cuentas/activas?page=0&size=20` | Lista cuentas activas paginadas | JWT de EJECUTIVO |
| `POST /auth/login` | Autentica por correo y contraseña y emite JWT | Público |
| `GET /usuarios/{id}` | Consulta los datos no sensibles del usuario autenticado | JWT del mismo usuario |
| `PUT /usuarios/{id}/password` | Cambia la contraseña, comprobando la contraseña actual | JWT del mismo usuario |

El interceptor recupera el rol vigente desde la base de datos en cada solicitud, además de comprobar que el usuario y el cliente sigan activos. Las búsquedas y listados generales están limitados a `EJECUTIVO`. El cliente solo puede consultar, actualizar o dar de baja su propio perfil y consultar sus propias cuentas; un ejecutivo puede operar sobre cualquier cliente. Los endpoints de usuario también se limitan al `id` presente en el token.

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
  "passwordActual": "ContraseñaAnterior1!",
  "passwordNueva": "ContraseñaNueva2!"
}
```

La contraseña nueva requiere mínimo 8 caracteres, mayúscula, minúscula, dígito y símbolo. También se rechazan contraseñas superiores a 72 bytes UTF-8, el límite de BCrypt.

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
