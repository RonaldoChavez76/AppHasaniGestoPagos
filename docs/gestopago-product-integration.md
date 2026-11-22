# Integracion de productos GestoPago

## Flujo

El endpoint local `GET /productos` delega en `GestoPagoProductService`, que utiliza `GestoPagoProductsClient` (OpenFeign) para invocar `GET /sistema/service/getProductList.do`.

La autenticacion usa `X-API-Key` (`gestopago.auth.api-key`) junto con las credenciales del distribuidor. El endpoint de autenticacion devuelve el JWT, que se persiste y luego se envia al endpoint de productos como `Authorization: Bearer <token>`.

## Configuracion

- `gestopago.products.url`: URL base del servicio externo.
- `gestopago.auth.api-key`: API key enviado como `X-API-Key` al autenticar.
- `spring.cloud.openfeign.client.config.gestoPagoProducts.connectTimeout`: timeout de conexion en milisegundos.
- `spring.cloud.openfeign.client.config.gestoPagoProducts.readTimeout`: timeout de lectura en milisegundos.

Tambien se requieren `DB_PASSWORD` y `GESTOPAGO_AUTH_PASSWORD`. El archivo `.env.example` muestra los nombres esperados; los valores reales deben definirse en el entorno o en un gestor de secretos y nunca versionarse.

## DTOs

- `GestoPagoProductListResponse`: respuesta principal del proveedor con `code`, `message` y `data` (lista de productos).
- `GestoPagoProductDto`: modelo completo con todos los atributos del contrato XML de GestoPago:
  - `servicio`: nombre del servicio (e.g. `AGUAKAN (Cancun)`).
  - `producto`: descripción del producto o paquete (e.g. `Agua Cancun`).
  - `idServicio`: identificador numérico del servicio.
  - `idProducto`: identificador numérico del producto.
  - `idCatTipoServicio`: identificador del tipo de servicio.
  - `tipoFront`: tipo de front/payload requerido para la transacción.
  - `hasDigitoVerificador`: bandera de requerimiento de dígito verificador.
  - `tipoReferencia`: tipo de referencia para pago.
  - `precio`: precio final o comisión.
  - `showAyuda`: indicador para mostrar ayuda en pantalla.
  - `legend`: texto de ayuda o términos de la transacción (CDATA).
  - `id`, `code`, `name`, `price`, `active`: campos estándar para compatibilidad.
- `ApiResponse`: envoltura común para respuestas exitosas y errores de integración.

## Errores y logs

El servicio captura errores de timeout/comunicacion, respuestas HTTP no exitosas, autenticacion rechazada y errores inesperados. Expone un mensaje tecnico controlado mediante `GestoPagoIntegrationException` y registra solo el estado HTTP o una descripcion general; nunca registra el token.

Cada invocacion registra inicio y resultado. El token nunca se incluye en los logs ni en la respuesta HTTP.

El `GlobalExceptionHandler` responde `401` para autenticacion rechazada, `504` para timeout, `502` para respuestas no exitosas del proveedor y `500` para errores inesperados.

## Pruebas

`GestoPagoProductServiceImplTest` cubre la respuesta exitosa, el envio del Bearer Token, autenticacion rechazada y error de comunicacion sin realizar llamadas reales al proveedor.
