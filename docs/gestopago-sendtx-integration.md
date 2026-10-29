# Integración de la API sendTx de GestoPago (PuntoRed)

## Descripción General

Implementación del consumo del endpoint `sendTx.do` de GestoPago para la venta de tiempo aire (recargas telefónicas) y pago de servicios/pines/gift cards, cumpliendo con la especificación técnica oficial de PuntoRed / GestoPago:
- **Documentación de referencia**: [Postman GestoPago sendTx](https://documenter.getpostman.com/view/19876210/Uz5MFtdn#aee8b4c7-06b6-439d-9899-06a74998bb7d)
- **Seguridad en Payload**: Cifrado simétrico **AES-256-CBC** con padding PKCS5/PKCS7 codificado en Base64.
- **Autenticación**: `Authorization: Bearer <token>` (gestión y renovación automática de JWT de 24 horas) y `X-API-Key`.
- **Persistencia y Evidencia (Log evidence)**: Registro obligatorio de transacciones y respuestas en PostgreSQL (`gestopago_transacciones`).
- **Verificación en Base de Datos**: Validación de existencia de productos en el catálogo local (`gestopago_catalog_products`) y prevención de transacciones duplicadas por ventana de tiempo.

---

## Diferenciación de Operación (tipoFront)

De acuerdo con el contrato de GestoPago, las operaciones se diferencian por el tipo de producto/servicio (`tipoFront` e `idCatTipoServicio`):

| Operación | tipoFront | Parámetro `telefono` | Parámetro `referencia` | Parámetro `montoPago` |
|---|---|---|---|---|
| **Recarga Telefónica** | 1 | Número telefónico a 10 dígitos del usuario | No aplica (se omite) | Opcional (definido por el paquete/producto) |
| **Pago de Servicios** | 2 | Fijo en `"1111111111"` | Referencia alfanumérica del recibo o factura | Requerido (monto a pagar en decimales) |
| **Gift Card / Pines** | 10 u 11 | Fijo en `"#1111111111"` | Identificador o cuenta según aplique | Según el producto |

---

## Endpoints Disponibles

Todos los endpoints están mapeados bajo `/api/gestopago/transacciones` y con alias `/transacciones`:

### 1. Recarga Telefónica (Endpoint Especializado)
- **Ruta**: `POST /api/gestopago/transacciones/recarga`
- **Content-Type**: `application/json`
- **Cuerpo de Solicitud (JSON)**:
```json
{
  "idProducto": 582,
  "idServicio": 133,
  "telefono": "5512345678",
  "monto": 10.00,
  "numeroCuenta": "8766090787749132",
  "simular": true
}
```
*Notas:*
- `numeroCuenta`: Si se envía, valida que la cuenta exista, esté ACTIVA y tenga saldo suficiente (`saldo >= monto`). Si la transacción es exitosa con GestoPago o en simulación, debita automáticamente el saldo de PostgreSQL en la tabla `cuentas`.
- `simular`: Si se envía en `true`, simula una aprobación exitosa de GestoPago para permitir pruebas locales sin depender de que tu IP pública esté en lista blanca.


### 2. Pago de Servicios (Endpoint Especializado)
- **Ruta**: `POST /api/gestopago/transacciones/servicio`
- **Content-Type**: `application/json`
- **Cuerpo de Solicitud (JSON)**:
```json
{
  "idProducto": 205,
  "idServicio": 76,
  "referencia": "25720381",
  "montoPago": 601.99,
  "telefono": "1111111111",
  "upc": "UPCSER98765",
  "unidad": "083"
}
```
*Nota: `telefono` es opcional; si no se provee, se fija automáticamente en `"1111111111"`.*

### 3. Transacción Unificada (Bandera / Discriminador)
- **Ruta**: `POST /api/gestopago/transacciones`
- **Content-Type**: `application/json`
- Permite enviar un único endpoint con discriminador `tipoOperacion` (`RECARGA` o `PAGO_SERVICIO`) o con bandera booleana `esServicio` (`true`/`false`):
```json
{
  "tipoOperacion": "PAGO_SERVICIO",
  "esServicio": true,
  "idProducto": 205,
  "idServicio": 76,
  "referencia": "25720381",
  "montoPago": 601.99
}
```

### 4. Consultar Transacción por UPC
- **Ruta**: `GET /api/gestopago/transacciones/{upc}`
- Retorna el detalle completo y comprobante de la transacción registrada.

### 5. Historial de Transacciones
- **Ruta**: `GET /api/gestopago/transacciones`
- Lista las últimas 50 transacciones persistidas en PostgreSQL con estado, comisiones, saldo y números de autorización.

---

## Formato de Respuesta del Backend

Todas las respuestas utilizan el formato unificado `ApiResponse<T>`:

```json
{
  "status": 200,
  "message": "Operacion realizada con exito",
  "data": {
    "upc": "UPCREC12345",
    "tipoOperacion": "RECARGA",
    "exitoso": true,
    "estado": "EXITOSA",
    "codigo": "01",
    "mensaje": "Operacion realizada con exito",
    "idTx": "291437188",
    "numAutorizacion": "100024988",
    "monto": 10.00,
    "saldo": "157731.83",
    "saldoFormateado": "157,731.83",
    "comision": "0.7",
    "comisionFormateada": "0.70",
    "fecha": "25/Feb/2022 18:15:25",
    "telefono": "5555555551",
    "referencia": null,
    "pin": null,
    "legend": null,
    "fechaRegistro": "2026-10-26T15:10:00"
  },
  "timestamp": "2026-10-26T21:10:00Z"
}
```

---

## Reglas de Validación de Duplicados (UPC)

Conforme a la sección *Duplicate Validation Rules by Service Type* de la documentación de GestoPago:
1. **Recargas y Pines**: Se detecta como duplicada si se repite el mismo `upc` dentro de los últimos **15 minutos**.
2. **Servicios**: Se detecta como duplicada si se repite el mismo `upc` en las últimas **24 horas**, o si coincide la combinación de mismo servicio y monto en las últimas **24 horas**.

El backend valida contra PostgreSQL antes de enviar la petición a GestoPago para proteger la integridad de los fondos del comercio.

---

## Cifrado AES-256-CBC

La solicitud HTTP externa a `https://gestopago.portalventas.net/sistema/service/sendTx.do` se envía en formato `application/x-www-form-urlencoded` con dos campos:
1. `random`: Marca de tiempo aleatoria (`<timestamp>_rand`).
2. `signed`: Cadena JSON cifrada con AES-256-CBC y codificada en Base64.

Valores configurables en `application.properties` / `.env`:
- `gestopago.crypto.secret-key`: Clave simétrica de 256 bits (32 bytes en Base64).
- `gestopago.crypto.iv`: Vector de inicialización de 16 bytes en Base64.
- `gestopago.unidad`: Código de sucursal comercial (por defecto `083`).
- Timeout de lectura de 62 segundos para soportar la respuesta de los operadores telefónicos.

---

## Estrategia Financiera, Concurrencia y Manejo de Timeouts

### 1. Control de Concurrencia (Bloqueo Pesimista)
Para evitar condiciones de carrera (*race conditions* / doble gasto) ante solicitudes simultáneas sobre la misma cuenta bancaria:
- Se utiliza `@Lock(LockModeType.PESSIMISTIC_WRITE)` en `CuentaRepository.findByNumeroCuentaForUpdate()`.
- Esto genera un bloqueo a nivel de fila en PostgreSQL (`SELECT ... FOR UPDATE`), garantizando que si dos transacciones concurrentes intentan debitar de la misma cuenta, la segunda esperará hasta que la primera finalice su commit, impidiendo saldos negativos.

### 2. Estrategia de Aplicación de Saldo en Dos Fases
1. **Fase 1: Validación y Verificación Previa:**
   - Se valida que la cuenta exista y se encuentre con estatus `ACTIVA`.
   - Se valida que `saldo >= montoRequerido`. Si no hay saldo suficiente, se aborta inmediatamente con `400 Bad Request` antes de contactar a GestoPago.
2. **Fase 2: Ejecución Externa:**
   - Se envía la petición cifrada a GestoPago.
3. **Fase 3: Liquidación Condicional:**
   - **Éxito (`CODIGO 01`):** Se debita el saldo de la cuenta atómicamente y se persiste la transacción en `gestopago_transacciones`.
   - **Rechazo del Proveedor (ej. Código 55, 06, etc.):** El saldo del cliente **permanece intacto y no se descuenta**.
   - **Timeout (Código 82 o SocketTimeoutException):** **NO** se asume que falló ni se descuenta a ciegas. La transacción se marca con estado `TIMEOUT` y se registra el UPC para conciliación posterior mediante la consulta o auditoría con el proveedor.

### 3. Seguridad de Credenciales
- El token JWT y las claves API (`GESTOPAGO_AUTH_PASSWORD`, `GESTOPAGO_API_KEY`, etc.) se inyectan a través de variables de entorno y archivos `.env` (excluidos del control de versiones mediante `.gitignore`).
- Las respuestas HTTP y los registros de log enmascaran u omiten cualquier secreto o payload cifrado sensible.

### 4. Manejo Centralizado de Excepciones (`@RestControllerAdvice`)
- `GlobalExceptionHandler` captura:
  - `MethodArgumentNotValidException` $\rightarrow$ `400 Bad Request` con detalle de campos no válidos.
  - `IllegalArgumentException` $\rightarrow$ `400 Bad Request` para reglas de negocio (saldo insuficiente, cuenta inactiva).
  - `DataIntegrityViolationException` $\rightarrow$ `409 Conflict` para violaciones de unicidad.
  - `GestoPagoIntegrationException` $\rightarrow$ Código HTTP contextualizado (502/504) protegiendo los detalles internos del proveedor.

---

## Verificación de Segundo Factor (2FA con OTP Temporal)

Se implementó el mecanismo de **Doble Factor de Autenticación (2FA)** conforme a las mejores prácticas de la banca digital y estándares de seguridad NIST:

### Flujo de Operación:
1. **Generación del Código (`POST /api/seguridad/2fa/solicitar`):**
   - El cliente envía su `numeroCuenta`.
   - Se valida que la cuenta esté `ACTIVA`.
   - Se genera un código OTP criptográficamente seguro de 6 dígitos numéricos (`SecureRandom`).
   - Se persiste nativamente en **PostgreSQL** (`seguridad_dos_factor_otp`) con expiración estricta de **5 minutos** (`TTL = 300` segundos) y aceleración opcional en **Redis** si está disponible.
2. **Autorización en la Transacción (`POST /api/gestopago/transacciones/...`):**
   - El cliente envía el campo `"codigo2FA": "123456"` en el payload.
   - Si el código es incorrecto o expiró $\rightarrow$ Retorna `400 Bad Request`.
   - Si el código es válido $\rightarrow$ Se valida y se **consume inmediatamente (marcando `consumido = true` en PostgreSQL y purgando de Redis)** para impedir ataques de repetición (*Replay Attacks*).
