# API de onboarding de clientes

API REST para registrar personas físicas, administrar su domicilio, crear cuentas bancarias y gestionar el acceso mediante JWT. El proyecto también integra la consulta de códigos postales de México y el catálogo de productos de GestoPago.

## Tecnologías

- Java 17 y Spring Boot 3.3.6.
- Spring Web, Validation, Data JPA/Hibernate y Spring Security.
- PostgreSQL en Supabase para datos relacionales y MongoDB para el catálogo de productos.
- Flyway para migraciones SQL.
- BCrypt para almacenar contraseñas y JWT HS256 para autenticar peticiones.
- OpenAPI/Swagger UI.

## Arquitectura

Los controladores exponen la API; los servicios contienen las reglas de negocio; los repositorios acceden a PostgreSQL con JPA; las entidades representan el modelo persistente. Clientes, domicilios, cuentas y usuarios se encuentran en el esquema `onboarding` de PostgreSQL. Las migraciones se encuentran en `src/main/resources/db/migration`.

Las validaciones propias de la petición se declaran en sus DTOs y se ejecutan con `@Valid`; `GlobalExceptionHandler` las transforma en respuestas `400` consistentes. Las consultas de listas aceptan `pagina` y `tamanio`: el cliente puede elegir el tamaño entre 1 y 100, y la API aplica un orden estable para que la paginación sea reproducible. Las reglas que dependen de datos persistidos —por ejemplo, unicidad, existencia y estado activo— permanecen en los servicios.

### Sobrecarga y sobrescritura en Java

- **Sobrecarga:** `ClienteMapper` declara `toEntity` para `ClienteRequest` y `DomicilioRequest`, y `actualizar` para los DTOs de cliente y domicilio. El nombre coincide, pero la firma de parámetros determina qué transformación se ejecuta.
- **Sobrescritura:** las implementaciones de `ClienteService`, `CuentaService` y `UsuarioService` marcan sus operaciones con `@Override`. Así se cumple el contrato declarado por cada interfaz y el compilador detecta firmas incompatibles.

La creación del cliente se realiza en una transacción: registra cliente y domicilio, genera una cuenta con saldo inicial y crea el usuario con contraseña BCrypt. La baja lógica conserva los registros, pero desactiva las cuentas y el usuario asociado.

## Diagrama entidad–relación

![Diagrama entidad–relación: tablas, campos, tipos, llaves y relaciones](docs/diagrama-er.svg)

`USUARIOS.cliente_id` es nulo únicamente para el usuario de rol `EJECUTIVO`; cada usuario `CLIENTE` está asociado a un único cliente. El correo del usuario cliente se mantiene alineado con el correo del cliente mediante una llave foránea compuesta.

## Base de datos

El script consolidado de creación está en [`database/schema.sql`](database/schema.sql). Reproduce las migraciones V1 a V5 y debe ejecutarse únicamente sobre una base nueva. En la aplicación, Flyway ejecuta las migraciones automáticamente al iniciar; no ejecutes además el script consolidado sobre una base que ya tenga esas migraciones.

La base contiene las tablas heredadas `public.gestopago_tokens` y `public.personas`, además de `onboarding.clientes`, `onboarding.domicilios`, `onboarding.cuentas` y `onboarding.usuarios`. Las llaves, restricciones, índices, secuencia de números de cuenta y valores permitidos están declarados en el SQL.

## Configuración y ejecución local

Configura los valores por variables de entorno (Spring Boot las usa para sobrescribir `application.properties`).

Variables principales:

| Variable                          | Uso                                                                                                 |
| --------------------------------- | --------------------------------------------------------------------------------------------------- |
| `SPRING_DATASOURCE_URL`           | URL JDBC de PostgreSQL/Supabase.                                                                    |
| `SPRING_DATASOURCE_USERNAME`      | Usuario de base de datos.                                                                           |
| `SPRING_DATASOURCE_PASSWORD`      | Contraseña de base de datos.                                                                        |
| `SPRING_DATA_MONGODB_URI`         | URI de MongoDB.                                                                                     |
| `ONBOARDING_JWT_SECRETO`          | Llave HS256 codificada en Base64, de al menos 256 bits.                                             |
| `ONBOARDING_EJECUTIVO_CORREO`     | Correo del ejecutivo inicial.                                                                       |
| `ONBOARDING_EJECUTIVO_PASSWORD`   | Contraseña inicial del ejecutivo; cumple la política de contraseñas.                                |
| `GESTOPAGO_AUTH_PASSWORD`         | Contraseña de autenticación de GestoPago.                                                           |
| `GESTOPAGO_API_KEY`               | API key opcional asignada por GestoPago.                                                            |
| `GESTOPAGO_PAYLOAD_SECRET_KEY`    | Clave AES-256 Base64 asignada por GestoPago; no uses la clave de ejemplo de la documentación.       |
| `GESTOPAGO_PAYLOAD_IV`            | IV Base64 de 16 bytes asignado por GestoPago.                                                       |
| `GESTOPAGO_REPLAY_ENCRYPTION_KEY` | Clave local Base64 de 32 bytes para cifrar con AES-GCM el payload que se conserva para `confirmTx`. |
| `GESTOPAGO_UNIDAD`                | Unidad/sucursal de 3–25 caracteres enviada a GestoPago.                                             |
| `GESTOPAGO_BEARER_TOKEN`          | Token Bearer opcional del catálogo; si se omite, se usa el token renovado guardado en PostgreSQL.   |

El ejecutivo se crea al arrancar si su correo no existe. Su contraseña se almacena con BCrypt. Si el usuario ya existe, el inicializador no cambia su contraseña.

En PowerShell, establece las variables en la sesión y arranca:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://HOST:5432/postgres?sslmode=require"
$env:SPRING_DATASOURCE_USERNAME = "USUARIO"
$env:SPRING_DATASOURCE_PASSWORD = "CONTRASENA"
$env:SPRING_DATA_MONGODB_URI = "URI_MONGODB"
$env:ONBOARDING_JWT_SECRETO = "LLAVE_BASE64_DE_32_BYTES_O_MAS"
$env:ONBOARDING_EJECUTIVO_CORREO = "correo-del-ejecutivo"
$env:ONBOARDING_EJECUTIVO_PASSWORD = "CONTRASENA_SEGURA"
./gradlew.bat bootRun
```

Completa los valores entre comillas con los datos de tu entorno. Para Render, configura las mismas variables en **Environment** del servicio. No publiques contraseñas, URI con credenciales ni la llave JWT en capturas o repositorios.

## Swagger y flujo de autenticación

- Aplicación desplegada: [https://pwa-n2mh.onrender.com/](https://pwa-n2mh.onrender.com/)
- Swagger UI desplegado: [https://pwa-n2mh.onrender.com/swagger-ui/index.html](https://pwa-n2mh.onrender.com/swagger-ui/index.html)
- OpenAPI JSON: [https://pwa-n2mh.onrender.com/v3/api-docs](https://pwa-n2mh.onrender.com/v3/api-docs)

### Inicio de sesión con doble factor y sesión única

El inicio de sesión tiene dos pasos y aplica a los dos roles (`EJECUTIVO` y `CLIENTE`):

1. `POST /auth/login` con `{ "correo", "password", "plataforma": "MOVIL" | "WEB", "dispositivo" }`. Si la contraseña es correcta, envía un código de 6 dígitos al correo del usuario (Gmail SMTP, `spring.mail.*`) y responde `202` con un `desafioId`.
2. `POST /auth/verificar-codigo` con `{ "desafioId", "codigo" }`. Abre la sesión:
   - `MOVIL` (por defecto, también Swagger): el JWT viene en `token`; se envía como `Authorization: Bearer`.
   - `WEB`: el JWT viaja en la cookie `SESION` (`HttpOnly; Secure; SameSite=Strict`) y no aparece en el cuerpo. Las peticiones que modifican datos deben repetir la cookie `XSRF-TOKEN` en el header `X-XSRF-TOKEN` (CSRF).

Reglas del código (`onboarding.verificacion.*`): se guarda solo su hash BCrypt, vence en 5 minutos, es de un solo uso (`410` si se reutiliza), admite 5 intentos (`429` al agotarlos), se puede reenviar con `POST /auth/reenviar-codigo` cada 60 segundos y hasta 3 veces. Además, el limitador por IP cubre login, verificación y reenvío.

**Sesión única respaldada por el servidor:** cada JWT lleva el id de una fila de `onboarding.sesiones` (claim `jti`) y cada petición valida que esa sesión siga abierta. Si el usuario ya tiene una sesión en otro dispositivo, `POST /auth/login` responde `409` con `sesionActiva` (plataforma y hora); al repetirlo con `"cerrarSesionAnterior": true` y verificar el código, la sesión anterior se cierra y su token responde `401` de inmediato. `POST /auth/logout` cierra la sesión en el servidor y `GET /auth/sesion` devuelve la sesión actual (la app web lo usa al recargar).

En Swagger: `POST /auth/login`, revisa el correo, `POST /auth/verificar-codigo` y copia `token` en **Authorize**. El rol `EJECUTIVO` administra clientes/cuentas/usuarios; el rol `CLIENTE` consulta y actualiza sus propios datos y cuentas.

### Ejecutivo inicial

No existe un endpoint para crear usuarios con rol `EJECUTIVO`. El usuario inicial se crea al arrancar con `onboarding.ejecutivo.correo` y `onboarding.ejecutivo.password` si ese correo no existe; si ya existía, el inicializador no cambia su contraseña.

## Endpoints principales

| Método  | Ruta                               | Acceso / función                                                                                                 |
| ------- | ---------------------------------- | ---------------------------------------------------------------------------------------------------------------- |
| `POST`  | `/auth/login`                      | Público; valida la contraseña y envía el código por correo (`202`). `409` si hay sesión en otro dispositivo.   |
| `POST`  | `/auth/verificar-codigo`           | Público; valida el código y abre la sesión (token en el cuerpo o cookie `SESION`).                              |
| `POST`  | `/auth/reenviar-codigo`            | Público; envía un código nuevo para el mismo desafío.                                                            |
| `GET`   | `/auth/sesion`                     | Sesión actual (usuario, rol, vigencia).                                                                          |
| `POST`  | `/auth/logout`                     | Cierra la sesión en el servidor.                                                                                 |
| `POST`  | `/clientes`                        | Público; registra cliente, domicilio, cuenta y usuario.                                                          |
| `POST`  | `/clientes/consulta`               | Ejecutivo; lista paginada y filtra por nombre, apellidos, CURP, RFC, correo, número de cuenta, estatus y fechas. |
| `POST`  | `/clientes/detalle`                | Ejecutivo o el propio cliente; cuerpo `{ "id": 1 }`.                                                             |
| `PATCH` | `/clientes`                        | Ejecutivo o el propio cliente; `id` en el cuerpo, actualización parcial y baja/reactivación lógica. CURP y RFC son inmutables. |
| `POST`  | `/cuentas/consulta`                | Ejecutivo; filtra por `clienteId` y/o `estatus`. Cliente: debe indicar su `clienteId`.                           |
| `POST`  | `/cuentas/detalle`                 | Ejecutivo o propietario; cuerpo `{ "numeroCuenta": "1000000001" }`.                                              |
| `POST`  | `/cuentas/saldo`                   | Ejecutivo o propietario; cuerpo `{ "numeroCuenta": "1000000001" }`.                                              |
| `POST`  | `/cuentas`                         | Ejecutivo; crea cuenta para cliente activo.                                                                      |
| `PATCH` | `/cuentas`                         | Ejecutivo; `numeroCuenta` en el cuerpo, modifica estatus.                                                        |
| `POST`  | `/usuarios/consulta`               | Ejecutivo; filtra por correo, estatus o cliente.                                                                 |
| `PUT`   | `/usuarios/agregar`                | Ejecutivo; agrega usuario a cliente activo sin usuario.                                                          |
| `POST`  | `/codigos-postales/consulta`       | Público; cuerpo `{ "codigoPostal": "44100" }`, consulta CP mexicano, estado, municipio y colonias.               |
| `GET`   | `/productos`                       | Público; consulta el catálogo de productos externo.                                                              |
| `POST`  | `/transacciones`                   | Propietario de la cuenta o ejecutivo; `numeroCuenta` en el cuerpo y header `Idempotency-Key`.                     |
| `POST`  | `/transacciones/consulta`          | Cuerpo `{ "id": "<uuid>" }`; consulta el estado de una transacción.                                              |

Ningún endpoint usa `@PathVariable` ni parámetros de consulta: identificadores y criterios de búsqueda viajan en el cuerpo JSON y se validan con `@Valid`. Las listas usan `pagina` (inicia en 0) y `tamanio` (por defecto 20, máximo 100) dentro del mismo cuerpo. El detalle completo de modelos está en Swagger.

## Validaciones y manejo de errores

Se validan mayoría de edad, formato de CURP/RFC/correo, unicidad de CURP/RFC/correo, sexo `H` o `M`, teléfono mexicano de 10 dígitos, CP de 5 dígitos y domicilio contra el servicio de códigos postales. Se aplican restricciones SQL, validaciones de Spring y excepciones centralizadas. Las contraseñas requieren mayúscula, minúscula, número, carácter especial y al menos 8 caracteres.

Los errores se devuelven con respuesta JSON mediante `GlobalExceptionHandler` y el manejador de seguridad. La API incluye límites de tamaño del cuerpo, paginación y protección frente a intentos repetidos de autenticación.

## Pruebas y evidencias

Las pruebas unitarias están en `src/test/java`. Ejecución:

```powershell
./gradlew.bat test
```

## Entregables del proyecto

- Diagrama ER: incluido en este README.
- Script SQL: `database/schema.sql`.
- Código fuente: `src/main/java`.
- API desplegada y Swagger: enlaces en la sección anterior.
- Pruebas/evidencias: pruebas unitarias en `src/test/java`; las evidencias manuales deben agregarse cuando se realicen.
- Documento técnico: arquitectura, configuración, seguridad, endpoints y decisiones técnicas descritos en este README.
