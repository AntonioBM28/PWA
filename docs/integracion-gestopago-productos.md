# Integración GestoPago – Catálogo de productos

Swagger: `http://localhost:8080/swagger-ui/index.html` → `GET /productos`.

El catálogo de GestoPago (`GET /sistema/service/getProductList.do`) se sincroniza una vez al día en MongoDB Atlas, y la
aplicación lo expone en JSON desde MongoDB en `GET /productos`.

## Flujo

```
1) Carga (cron diario 06:00 America/Mexico_City y al arrancar)
   CatalogoProductoScheduler -> CatalogoProductoService -> GestoPagoProductosClient (Feign) -> GestoPago (XML)
                                     |  valida CODIGO=01, mapea                   Authorization: Bearer <token>
                                     v
                                  MongoDB Atlas (gestoPago.cat_products)

2) Consulta
   GET /productos -> ProductoController -> ProductoService -> MongoDB -> JSON
```

Si la carga falla (GestoPago caído, timeout, código de error o catálogo vacío), **no se modifica MongoDB**: se conserva el
último catálogo válido y la consulta sigue respondiendo.

## Componentes

| Capa | Clase | Responsabilidad |
|---|---|---|
| Configuración | `application.properties` | URL, token, timeouts, cron y conexión a MongoDB |
| Configuración | `config/GestoPagoProductosFeignConfig` | Registra el decoder XML y el `ErrorDecoder` (HTTP 401/403 y otros estatus no exitosos) |
| Configuración | `config/GestoPagoXmlDecoder` | Lee la respuesta XML con JAXB, protegido contra XXE |
| Client | `client/GestoPagoProductosClient` | Cliente Feign declarativo (mismo patrón que `GestoPagoAuthClient`) |
| Scheduler | `scheduler/CatalogoProductoScheduler` | Dispara la carga por cron y al arrancar; atrapa errores para no detener el scheduler |
| Service | `service/CatalogoProductoService(Impl)` | Resolución del token, invocación a GestoPago, logs, traducción de errores y guardado en MongoDB |
| Service | `service/ProductoService(Impl)` | Consulta del catálogo en MongoDB |
| Entidad Mongo | `entity/mongo/CatProducto` | Documento de la colección (`_id` = `idProducto`) |
| Repositorio | `repositorys/mongo/CatProductoRepository(Custom/Impl)` | `MongoRepository` del catálogo + reemplazo bulk con `MongoTemplate` |
| DTO externo | `model/gestopago/GestoPagoProductListResponse`, `GestoPagoMensaje`, `GestoPagoProducto` | Contrato XML de GestoPago (anotaciones JAXB) |
| DTO de salida | `model/ProductosResponse`, `ProductoResponse` | Respuesta JSON; extiende `GenericResponse` como `PersonaResponse` |
| Mapper | `mapper/GestoPagoProductoMapper` | MapStruct: XML → documento Mongo → respuesta JSON |
| Controller | `controller/ProductoController` | `GET /productos` |
| Errores | `exception/*` | Jerarquía de excepciones y `@RestControllerAdvice` |

## Configuración

Toda la configuración vive en `src/main/resources/application.properties`, siguiendo la convención existente
`gestopago.<módulo>.<propiedad>`:

| Sección | Propiedades |
|---|---|
| PostgreSQL local (tokens) | `spring.datasource.url`, `username`, `password` (BD `PagoServicios`) |
| Autenticación GestoPago | `gestopago.auth.url`, `id-distribuidor`, `codigo-dispositivo`, `password`, `refresh-rate-ms` |
| Catálogo GestoPago | `gestopago.productos.url`, `token`, `cron`, `cron-zona`, `carga-al-iniciar`, `coleccion` y timeouts de Feign |
| MongoDB Atlas | `spring.data.mongodb.uri` (con timeouts de conexión) y `spring.data.mongodb.database` (`gestoPago`) |

- `spring.cloud.config.enabled=false`: el proyecto incluye el cliente de Spring Cloud Config, pero no hay servidor de
  configuración.
- La URI de Mongo incluye `connectTimeoutMS`, `socketTimeoutMS` y `serverSelectionTimeoutMS` para que una falla de red
  responda en segundos en lugar de dejar la petición colgada.
- En Atlas, la IP del equipo o servidor debe estar autorizada en **Security → Network Access**.

### Obtención del token Bearer

1. Si `gestopago.productos.token` tiene valor, se usa ese token.
2. Si está vacío, se usa el token vigente que `GestoPagoTokenService` renueva periódicamente y guarda en BD
   (identificado por `gestopago.auth.id-distribuidor` y `gestopago.auth.codigo-dispositivo`).
3. Si todavía no existe (por ejemplo, la carga al arrancar se ejecuta antes que la primera renovación), se solicita una
   renovación y se vuelve a buscar.
4. Si no hay ninguno disponible, se lanza `GestoPagoAuthenticationException` sin invocar al servicio externo.

## Respuesta de GestoPago (XML)

```xml
<RESPONSE>
    <MENSAJE><CODIGO>01</CODIGO><TEXTO>Operacion realizada con exito</TEXTO></MENSAJE>
    <PRODUCTOS>
        <producto servicio='...' producto='...' idServicio='56' idProducto='185' idCatTipoServicio='15'
                  tipoFront='2' hasDigitoVerificador='false' precio='10.0' showAyuda='false' tipoReferencia='c'>
            <legend><![CDATA[...]]></legend>
        </producto>
    </PRODUCTOS>
</RESPONSE>
```

- Se lee con un `Decoder` JAXB propio; JAXB ya era dependencia del proyecto. DTD y entidades externas deshabilitadas (XXE).
- `CODIGO = 01` significa éxito; cualquier otro código se considera respuesta no exitosa aunque el HTTP sea 200.
- `producto` → `nombre` y `legend` → `leyenda` (con espacios normalizados).

## Guardado en MongoDB

- Cada producto es un documento con `_id = idProducto`; `precio` se guarda como `Decimal128` y se agrega `fechaActualizacion`.
- La sincronización hace *upsert* de todos los productos en una sola operación bulk (`CatProductoRepositoryImpl`) y
  después elimina los que ya no vienen en GestoPago. Así la colección nunca queda vacía a mitad de la carga.
- Un catálogo vacío de GestoPago se trata como error, para no borrar el catálogo vigente.
- La consulta devuelve los productos ordenados por `servicio` y `nombre`; si la colección está vacía devuelve lista vacía.

## Manejo de errores

| Escenario | Excepción | HTTP expuesto / efecto |
|---|---|---|
| Autenticación (401/403 o sin token) | `GestoPagoAuthenticationException` | Carga fallida, se conserva el catálogo |
| HTTP no 2xx | `GestoPagoResponseException` | Carga fallida, se conserva el catálogo |
| `CODIGO` ≠ `01`, catálogo vacío, cuerpo vacío o XML ilegible | `GestoPagoResponseException` | Carga fallida, se conserva el catálogo |
| Timeout (conexión o lectura) | `GestoPagoTimeoutException` | Carga fallida, se conserva el catálogo |
| Error de comunicación | `GestoPagoCommunicationException` | Carga fallida, se conserva el catálogo |
| MongoDB no disponible | `CatalogoProductosException` | `GET /productos` responde 503 |

Las excepciones de GestoPago heredan de `GestoPagoIntegrationException` (con su estatus HTTP) y
`GestoPagoExceptionHandler` convierte todas en `GenericResponse` (`codigo = 1`). La configuración Feign **no** se anota
con `@Configuration`, así el decoder XML y el `ErrorDecoder` solo aplican a este cliente.

## Logs

- Inicio y fin de la sincronización y de la invocación a GestoPago (con duración en ms y número de productos).
- Errores con el tipo de excepción y un mensaje controlado.
- Nunca se registran el token, contraseñas, headers ni el cuerpo de la respuesta externa.

## Pruebas

- `CatalogoProductoServiceImplTest` (15): carga exitosa guardada en Mongo, token por configuración/vigente/renovado, sin
  token, token inactivo, 401, estatus no exitoso, código ≠ 01, catálogo vacío, sin `MENSAJE`, respuesta nula, XML
  ilegible, timeout, error de conexión y falla al guardar en Mongo.
- `ProductoServiceImplTest` (3): consulta con datos, colección vacía y Mongo no disponible.
- `CatalogoProductoSchedulerTest` (3): carga al arrancar habilitada/deshabilitada y que un error no detenga el cron.
- `GestoPagoXmlDecoderTest` (3): XML de ejemplo, XML malformado y rechazo de DTD/XXE.
- `GestoPagoProductosFeignConfigTest` (3): traducción de 401/403/500.
- `CatProductoTest` (1): el nombre de la colección se toma de la configuración.

```bash
./gradlew test
```

## Supuestos y pendientes

- **Códigos de negocio**: solo se conoce `01` (éxito). Si GestoPago documenta otros códigos se pueden mapear a
  excepciones específicas.
- **Cambios de build**: se agregó `spring-boot-starter-data-mongodb`; se actualizó Lombok de 1.18.26 a 1.18.36 (la
  anterior no compila con JDK 21) y se agregó `junit-platform-launcher`, que Gradle 9 exige para ejecutar pruebas.
