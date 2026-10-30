# Integración segura de operaciones con GestoPago

Contrato consultado: [PuntoRed API Technical Specification en Postman](https://documenter.getpostman.com/view/19876210/Uz5MFtdn#aee8b4c7-06b6-439d-9899-06a74998bb7d).

## Contrato aplicado

- `POST /sistema/service/sendTx.do` y `POST /sistema/service/confirmTx.do` reciben `application/x-www-form-urlencoded` con `random` y `signed`; `signed` es el JSON cifrado en Base64 con AES-256-CBC/PKCS5Padding.
- Todas las llamadas llevan `Authorization: Bearer <token>` y `X-API-Key` únicamente si el distribuidor recibió una.
- La colección indica que el token dura 24 horas. No se pide uno por operación; se usa el token rotado por `GestoPagoTokenService`.
- UPC es obligatorio en `sendTx`, de 3–15 caracteres, único por operación. La aplicación genera un UPC decimal aleatorio de 15 dígitos y lo conserva junto con la operación idempotente.
- `horaLocal` usa `yyyyMMddHHmmss`; `unidad` tiene 3–25 caracteres.
- En `tipoFront=1`, `precio` del catálogo es el precio final y el monto se valida por igualdad. En `tipoFront=2`, `precio` es una comisión y no debe usarse como monto del recibo; el monto se valida con los límites configurables. El teléfono del tipo 2 usa el valor fijo documentado; tipos de servicio 10 y 11 usan la variante con `#` indicada por la colección.
- Se admiten las variantes estándar `tipoFront=1` y `tipoFront=2`. Los flujos especializados `tipoFront=4/5/30/31/40` tienen otros contratos y se rechazan para no mandar payloads incompletos.
- La respuesta XML `CODIGO=01` confirma aprobación. `02` representa saldo insuficiente del distribuidor; los demás códigos no exitosos son rechazo salvo `06` (aplicada anteriormente) y `82` (timeout), que se concilian antes de liberar saldo.
- Ante timeout, la documentación exige esperar al menos 62 segundos y llamar `confirmTx` dentro de las 24 horas con el mismo `random` y el mismo payload. La configuración espera 72 segundos por defecto para evitar una consulta concurrente con la petición original. `confirmTx` con código `06` y autorización confirma la aplicación; código `70` confirma que no se aplicó.

## Flujo de la aplicación

1. `POST /cuentas/{numeroCuenta}/transacciones` requiere JWT, autorización del propietario (o EJECUTIVO), `Idempotency-Key` y DTO validado.
2. Se valida el producto y su servicio contra MongoDB. El precio y el `tipoFront` provienen del catálogo; el cliente no puede enviar precios o UPC.
3. Se genera el JSON y se cifra para el proveedor. El UPC y la huella de la solicitud quedan vinculados a la clave idempotente y al cliente.
4. En una transacción PostgreSQL corta se bloquea la cuenta, se verifica estado del cliente/cuenta y saldo, se descuenta el monto y se registra la operación `PENDIENTE`. El payload de repetición se cifra adicionalmente con AES-GCM usando una clave local independiente.
5. Fuera de la transacción SQL se llama una sola vez a `sendTx` con timeout finito y sin reintentos automáticos.
6. Aprobación conserva el débito. Rechazo definitivo devuelve el saldo una vez. Timeout, error de transporte, XML inválido, código 06 o 82 dejan el importe reservado y operación `CONCILIACION`.
7. El job consulta `confirmTx` con los mismos datos criptográficos guardados. Si se confirma aplicada, finaliza como aprobada; si la respuesta dice que no se aplicó, reembolsa. Sin respuesta definitiva tras 24 horas, conserva el estado y el débito para conciliación manual.
8. Las repeticiones de una misma `Idempotency-Key` y mismo contenido devuelven la operación existente sin llamar de nuevo a `sendTx`; reutilizar la clave con otro contenido responde conflicto.

Las operaciones pendientes devuelven HTTP 202 y se consultan con `GET /transacciones/{id}`. El endpoint nunca devuelve tokens, payload cifrado ni mensajes crudos del proveedor. Los datos PIN/FOLIO solo se muestran a usuarios autorizados para consultar esa operación y no se escriben en logs.

## Configuración

Variables sensibles y operativas:

- `GESTOPAGO_AUTH_PASSWORD`, `GESTOPAGO_API_KEY` (opcional).
- `GESTOPAGO_PAYLOAD_SECRET_KEY` y `GESTOPAGO_PAYLOAD_IV`: valores de producción entregados por GestoPago; no usar ejemplos de la documentación.
- `GESTOPAGO_REPLAY_ENCRYPTION_KEY`: clave local aleatoria Base64 de 32 bytes para proteger el payload persistido para conciliación.
- `GESTOPAGO_UNIDAD`: nombre/código de terminal asignado, entre 3 y 25 caracteres.
- `GESTOPAGO_TRANSACTION_TIMEOUT_MS` (70 000 por omisión), `GESTOPAGO_CONFIRMATION_DELAY_SECONDS` (72 por omisión), `GESTOPAGO_TRANSACTION_AMOUNT_MIN`/`MAX` y `GESTOPAGO_TRANSACTION_TIME_ZONE`.

Las claves AES-CBC/IV entregadas al integrador son requisito del protocolo legado; AES-CBC no autentica el ciphertext. La aplicación mantiene esos datos solo en configuración del entorno/gestor de secretos, cifra los payloads guardados con AES-GCM, no registra contenido de petición/respuesta y deshabilita los reintentos HTTP del POST financiero.

## Datos pendientes para habilitar una prueba contra el proveedor

La colección describe el protocolo, pero no incluye credenciales ni configuración exclusiva del distribuidor. Antes de ejecutar operaciones en sandbox o producción, el responsable de GestoPago debe entregar por un canal seguro la contraseña de autenticación, las claves AES e IV asignados, la unidad habilitada, y confirmar si aplica `X-API-Key`. También hacen falta productos y servicios habilitados para pruebas y una cuenta/distribuidor con saldo. Estos valores se configuran con las variables de entorno anteriores; no deben agregarse a Git. Sin esos datos no se puede verificar el cifrado contra el proveedor ni ejecutar una transacción de extremo a extremo. Las pruebas locales usan respuestas simuladas y no acreditan la aceptación del contrato por el ambiente remoto.

## Escenarios de error y respuesta

| Escenario | Estado local / HTTP | Saldo |
|---|---|---|
| Parámetro, catálogo, precio o rango inválido | Rechazo local `400`/`422` | Sin cambio |
| Cliente/cuenta inactivos, cuenta ajena o saldo insuficiente | `403`/`422` | Sin cambio |
| Éxito de GestoPago (`01`) | `APROBADA` / `201` | Débito conservado |
| Rechazo definitivo, incluido `02` | `RECHAZADA` / `422` | Reembolso único |
| Código `06` o `82`, timeout de red, HTTP 5xx, XML inválido | `CONCILIACION` / `202` | Importe retenido |
| `confirmTx` aplicada (`06`) | `APROBADA` | Débito conservado |
| `confirmTx` no aplicada (`70`) | `RECHAZADA` | Reembolso único |
| Sin resultado definitivo al cumplir 24 h | Conciliación manual | Importe retenido |

No se hacen cargos reales en pruebas. Los escenarios de compra y confirmación se deben probar con respuestas XML simuladas; la prueba contractual contra sandbox requiere credenciales, claves y productos habilitados para el ambiente de pruebas.
