# AppGestoPagos

Servicio Spring Boot para la integración con GestoPago, consulta de personas y administración local del catálogo de productos.

## Tecnologías

- Java 17 como compatibilidad de código y Spring Boot 3.3.6.
- Spring Data JPA con PostgreSQL.
- Spring Data Redis para caché.
- OpenFeign para consumir GestoPago.
- JAXB para deserializar el catálogo XML real.
- Flyway para migraciones de base de datos.
- Spring Scheduler para renovación de tokens y actualización del catálogo.
- Gradle Wrapper para compilar y ejecutar el proyecto.

## Arquitectura implementada

### Autenticación GestoPago

El servicio obtiene un token JWT desde GestoPago usando el distribuidor, dispositivo, contraseña y API key configurados. El token se persiste en PostgreSQL y se reutiliza hasta que debe renovarse.

El cliente de autenticación usa el decoder JSON estándar de Feign. El decoder JAXB está aislado exclusivamente para el cliente XML del catálogo.

### Catálogo XML

El cliente XML consulta:

```text
GET /sistema/service/getProductList.do
```

El modelo JAXB representa el contrato real:

- `RESPONSE`
- `MENSAJE` con `CODIGO` y `TEXTO`
- `PRODUCTOS`
- elementos `producto` con atributos como `servicio`, `producto`, `idServicio`, `idProducto`, `precio` y `tipoReferencia`
- elemento `legend` con texto o CDATA

### PostgreSQL y Redis

PostgreSQL es la persistencia principal del catálogo y de los tokens. Redis funciona como caché mediante Spring Cache y `@Cacheable`; no se utiliza un repositorio Redis duplicado para las entidades JPA.

El flujo del catálogo es:

1. Consultar la caché Redis.
2. Consultar PostgreSQL si no existe una entrada válida en Redis.
3. Consultar GestoPago si no hay información local.
4. Persistir la respuesta externa en PostgreSQL.
5. Responder mediante la caché en las llamadas posteriores.

El serializador Redis está configurado con Jackson y soporte para `LocalDateTime`, tipos Java y tipos polimórficos de `ResponseDTO`.

### Fallback

Ante timeout o error HTTP del proveedor externo, el servicio conserva y devuelve la información disponible localmente. Los errores de integración se registran sin exponer tokens.

### Tareas programadas

- Renovación del token GestoPago con la frecuencia definida por `gestopago.auth.refresh-rate-ms`.
- Actualización nocturna del catálogo con el cron `0 0 0 * * ?`.

## Requisitos locales

- JDK 17 o superior. El código está configurado con compatibilidad Java 17.
- PostgreSQL ejecutándose en `localhost:5432`.
- Redis ejecutándose en `localhost:6379`.
- Credenciales válidas de GestoPago para el entorno correspondiente.

## Configuración local

Copia el archivo de ejemplo:

```powershell
Copy-Item .env.example .env
```

Completa los valores de `.env`:

```properties
DB_PASSWORD=tu-password-de-postgres
GESTOPAGO_AUTH_PASSWORD=tu-password-de-gestopago
GESTOPAGO_API_KEY=tu-api-key-de-gestopago
```

`.env` está excluido de Git. Nunca subas contraseñas, API keys o tokens reales al repositorio.

La aplicación importa opcionalmente `.env` desde `application.properties`. También permite que un entorno externo proporcione esas variables.

## Base de datos

La aplicación ejecuta las migraciones Flyway al iniciar:

- `V1__create_gestopago_tokens.sql`: crea la tabla de tokens.
- `V2__create_gestopago_catalog_products.sql`: crea la tabla e índice del catálogo.

La conexión local predeterminada es:

```text
jdbc:postgresql://localhost:5432/postgres
```

## Ejecución

Desde PowerShell:

```powershell
./gradlew bootRun
```

La aplicación queda disponible en:

```text
http://localhost:8080
```

El Config Server está configurado como opcional. Si no existe un servidor en `localhost:8888`, aparecerá un aviso, pero la aplicación usará la configuración local.

## Endpoints principales

### Salud de la aplicación

```text
GET http://localhost:8080/actuator/health
```

Respuesta esperada:

```json
{"status":"UP"}
```

### Catálogo GestoPago

```text
GET http://localhost:8080/api/gestopago/catalogo
```

Ejemplo con PowerShell:

```powershell
Invoke-RestMethod http://localhost:8080/api/gestopago/catalogo
```

La respuesta contiene `codigo`, `mensaje` y `data`. El campo `data` contiene los productos disponibles.

## Verificar Redis

Comprobar que Redis acepta conexiones:

```powershell
Test-NetConnection localhost -Port 6379
```

Debe aparecer:

```text
TcpTestSucceeded : True
```

Si `redis-cli` está instalado:

```powershell
redis-cli ping
redis-cli KEYS "*catalogoGestopago*"
```

La caché del catálogo utiliza el nombre `catalogoGestopago` y la clave lógica `catalogo-v2`. No existe un endpoint HTTP directo de Redis; se valida a través de `/api/gestopago/catalogo`.

Para comprobar el caché funcionalmente, ejecuta dos veces el endpoint. Ambas respuestas deben ser `HTTP 200`; la segunda puede resolverse desde Redis sin repetir la consulta externa.

## Pruebas y build

Ejecutar la prueba enfocada del catálogo:

```powershell
./gradlew test --tests "com.proyecto.servicios.service.Impl.GestoPagoCatalogServiceImplTest"
```

Ejecutar todas las pruebas y el build:

```powershell
./gradlew clean build
```

El proyecto fue validado con `BUILD SUCCESSFUL` para el build completo y la prueba de fallback/deserialización XML del catálogo.

## Estructura relevante

```text
src/main/java/com/proyecto/servicios/
├── client/       Clientes Feign de autenticación y productos
├── config/       PostgreSQL, Flyway, Redis, OpenFeign y OpenAPI
├── controller/   Endpoints HTTP
├── entity/       Entidades JPA
├── model/        DTOs JSON y XML
├── repositorys/  Repositorios JPA
└── service/      Servicios, fallback y tareas programadas

src/main/resources/
├── application.properties
└── db/migration/
    ├── V1__create_gestopago_tokens.sql
    └── V2__create_gestopago_catalog_products.sql
```

## Advertencias conocidas

- Spring puede mostrar un aviso si Config Server no está disponible en `localhost:8888`; la importación es opcional.
- Flyway puede advertir que la versión utilizada no ha sido probada oficialmente con PostgreSQL 18.
- Las advertencias de dependencias opcionales de Oracle/OpenNLP no impiden el build cuando las clases no son requeridas por el flujo ejecutado.
