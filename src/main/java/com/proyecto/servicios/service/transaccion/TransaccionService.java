package com.proyecto.servicios.service.transaccion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoTransaccionesClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.mongo.CatProducto;
import com.proyecto.servicios.entity.onboarding.Transaccion;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.exception.GestoPagoAuthenticationException;
import com.proyecto.servicios.exception.GestoPagoIntegrationException;
import com.proyecto.servicios.exception.TransaccionException;
import com.proyecto.servicios.model.gestopago.GestoPagoTransaccionResponse;
import com.proyecto.servicios.model.transaccion.CrearTransaccionRequest;
import com.proyecto.servicios.model.transaccion.DatosCompra;
import com.proyecto.servicios.model.transaccion.TransaccionResponse;
import com.proyecto.servicios.repositorys.mongo.CatProductoRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.Impl.ProductoServiceImpl;
import com.proyecto.servicios.service.gestopago.GestoPagoPayloadCipher;
import com.proyecto.servicios.service.gestopago.GestoPagoTransaccionXmlParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class TransaccionService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter HORA_LOCAL = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String TELEFONO_SERVICIO = "1111111111";
    static final int CODIGO_EXITO = 0;
    static final int CODIGO_RECHAZO = 1;
    static final String CODIGO_NO_DISPONIBLE = "GP-NODIS";
    static final String MOTIVO_NO_DISPONIBLE =
            "El servicio de GestoPago no está disponible en este momento. No se realizó ningún cargo a tu cuenta";

    private final CatProductoRepository catalogo;
    private final TransaccionPersistenceService persistencia;
    private final GestoPagoTokenService tokenService;
    private final GestoPagoPayloadCipher cipher;
    private final GestoPagoTransaccionesClient provider;
    private final GestoPagoTransaccionXmlParser parser;
    private final ObjectMapper objectMapper;
    private final String unidad;
    private final BigDecimal montoMinimo;
    private final BigDecimal montoMaximo;
    private final ZoneId zonaHoraria;
    private final Integer idDistribuidor;
    private final String codigoDispositivo;

    /** Imagenes de los servicios publicadas por GestoPago (para el ticket). */
    @Value("${gestopago.imagenes.url-base:" + ProductoServiceImpl.URL_IMAGENES_DEFAULT + "}")
    private String urlImagenes = ProductoServiceImpl.URL_IMAGENES_DEFAULT;

    public TransaccionService(CatProductoRepository catalogo,
                              TransaccionPersistenceService persistencia,
                              GestoPagoTokenService tokenService,
                              GestoPagoPayloadCipher cipher,
                              GestoPagoTransaccionesClient provider,
                              GestoPagoTransaccionXmlParser parser,
                              ObjectMapper objectMapper,
                              @Value("${gestopago.transacciones.unidad:}") String unidad,
                              @Value("${gestopago.transacciones.monto-min:0.01}") BigDecimal montoMinimo,
                              @Value("${gestopago.transacciones.monto-max:100000.00}") BigDecimal montoMaximo,
                              @Value("${gestopago.transacciones.zona-horaria:America/Mexico_City}") String zonaHoraria,
                              @Value("${gestopago.auth.id-distribuidor}") Integer idDistribuidor,
                              @Value("${gestopago.auth.codigo-dispositivo}") String codigoDispositivo) {
        this.catalogo = catalogo;
        this.persistencia = persistencia;
        this.tokenService = tokenService;
        this.cipher = cipher;
        this.provider = provider;
        this.parser = parser;
        this.objectMapper = objectMapper;
        this.unidad = unidad;
        this.montoMinimo = montoMinimo;
        this.montoMaximo = montoMaximo;
        this.zonaHoraria = ZoneId.of(zonaHoraria);
        this.idDistribuidor = idDistribuidor;
        this.codigoDispositivo = codigoDispositivo;
    }

    public TransaccionResponse comprar(String numeroCuenta, String idempotencyKey,
                                        CrearTransaccionRequest request, JwtAuthenticationToken auth) {
        validarClave(idempotencyKey);
        Integer clienteAutenticado = clienteId(auth);
        boolean ejecutivo = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_EJECUTIVO"));
        String huella = huella(numeroCuenta, request);

        // Las repeticiones idempotentes devuelven el resultado original aunque
        // el catálogo haya cambiado desde la primera solicitud.
        Optional<Transaccion> anterior = persistencia.buscarPorClave(numeroCuenta, clienteAutenticado, ejecutivo, idempotencyKey);
        if (anterior.isPresent()) {
            if (!anterior.get().getHuellaSolicitud().equals(huella)) {
                throw new TransaccionException("La clave de idempotencia ya se usó con otros datos", HttpStatus.CONFLICT);
            }
            return respuesta(anterior.get());
        }

        CatProducto producto = validarCompra(request);
        String upc = crearUpc();
        var datos = new TransaccionPersistenceService.DatosOperacion(producto.getIdProducto(), producto.getIdServicio(),
                producto.getNombre(), producto.getServicio(),
                enmascarar(StringUtils.defaultIfBlank(request.getDestino(), request.getReferencia())));

        // Preparar el envio (unidad, token y cifrado de GestoPago). Si falla, la solicitud nunca sale:
        // la operacion se registra RECHAZADA sin descontar saldo y se devuelve su ticket.
        String token;
        String signed;
        String payloadPersistido;
        try {
            if (unidad == null || unidad.length() < 3 || unidad.length() > 25) {
                throw new GestoPagoAuthenticationException("La unidad de GestoPago no está configurada");
            }
            token = resolverToken();
            signed = cipher.cifrar(payload(producto, request, upc));
            payloadPersistido = cipher.protegerParaAlmacenamiento(signed);
        } catch (GestoPagoIntegrationException | IllegalStateException ex) {
            log.warn("GestoPago no disponible; operacion rechazada sin cargo: tipo={}", ex.getClass().getSimpleName());
            var rechazo = persistencia.registrarRechazo(numeroCuenta, clienteAutenticado, ejecutivo, idempotencyKey,
                    huella, upc, request.getMonto(), datos, CODIGO_NO_DISPONIBLE, MOTIVO_NO_DISPONIBLE);
            return respuesta(rechazo.transaccion());
        }
        String random = Long.toString(System.currentTimeMillis());
        TransaccionPersistenceService.Reserva reserva = persistencia.reservar(
                numeroCuenta, clienteAutenticado, ejecutivo, idempotencyKey, huella, upc, random, payloadPersistido,
                request.getMonto(), datos);
        if (reserva.repetida()) return respuesta(reserva.transaccion());

        Transaccion tx = reserva.transaccion();
        try {
            String xml = provider.enviar(token, random, signed);
            GestoPagoTransaccionResponse resultado = parser.parse(xml);
            return procesarRespuesta(tx, resultado);
        } catch (GestoPagoTransaccionesClient.ProviderHttpException ex) {
            if (ex.getStatus() >= 400 && ex.getStatus() < 500) {
                Transaccion rechazada = persistencia.finalizar(tx.getId(), EstadoTransaccion.RECHAZADA,
                        Integer.toString(ex.getStatus()), "Solicitud rechazada antes de procesar", null, null, null, null, null);
                log.warn("GestoPago rechazó la transacción id={} http={}", tx.getId(), ex.getStatus());
                return respuesta(rechazada);
            }
            return pendiente(tx, "HTTP_PROVIDER_ERROR", "Respuesta externa indeterminada");
        } catch (GestoPagoTransaccionesClient.ProviderCommunicationException | IllegalArgumentException ex) {
            return pendiente(tx, "PROVIDER_UNKNOWN", "Requiere consulta confirmTx");
        }
    }

    private TransaccionResponse procesarRespuesta(Transaccion tx, GestoPagoTransaccionResponse resultado) {
        if (resultado == null || resultado.getMensaje() == null || StringUtils.isBlank(resultado.getMensaje().getCodigo())) {
            return pendiente(tx, "INVALID_RESPONSE", "Respuesta de proveedor incompleta");
        }
        String codigo = resultado.getMensaje().getCodigo();
        if ("01".equals(codigo)) {
            if (resultado.getMonto() != null && resultado.getMonto().compareTo(tx.getMonto()) != 0) {
                return pendiente(tx, codigo, "El monto de respuesta requiere conciliación");
            }
            return respuesta(persistencia.finalizar(tx.getId(), EstadoTransaccion.APROBADA, codigo,
                    "Operación realizada", resultado.getIdTx(), resultado.getNumeroAutorizacion(),
                    resultado.getPin(), resultado.getFolio(), resultado.getLeyenda()));
        }
        if ("06".equals(codigo) || "82".equals(codigo)) {
            Transaccion conciliacion = persistencia.finalizar(tx.getId(), EstadoTransaccion.CONCILIACION, codigo,
                    "Pendiente de confirmación con GestoPago", resultado.getIdTx(), resultado.getNumeroAutorizacion(),
                    null, null, null);
            log.warn("Transacción GestoPago requiere confirmación id={} codigo={}", tx.getId(), codigo);
            return respuesta(conciliacion);
        }
        Transaccion rechazada = persistencia.finalizar(tx.getId(), EstadoTransaccion.RECHAZADA, codigo,
                "GestoPago rechazó la operación", resultado.getIdTx(), resultado.getNumeroAutorizacion(),
                null, null, null);
        log.info("Transacción rechazada id={} codigo={}", tx.getId(), codigo);
        return respuesta(rechazada);
    }

    public Transaccion confirmar(Transaccion tx) {
        try {
            String xml = provider.confirmar(resolverToken(), tx.getRandomProveedor(),
                    cipher.recuperarParaConfirmacion(tx.getPayloadCifrado()));
            GestoPagoTransaccionResponse resultado = parser.parse(xml);
            if (resultado == null || resultado.getMensaje() == null) return tx;
            String codigo = resultado.getMensaje().getCodigo();
            String idTx = resultado.getIdTx() != null ? resultado.getIdTx() : resultado.getMensaje().getIdTx();
            if ("06".equals(codigo) && StringUtils.isNotBlank(resultado.getNumeroAutorizacion())) {
                return persistencia.finalizar(tx.getId(), EstadoTransaccion.APROBADA, codigo,
                        "Operación confirmada", idTx, resultado.getNumeroAutorizacion(),
                        resultado.getPin(), resultado.getFolio(), resultado.getLeyenda());
            }
            if ("70".equals(codigo)) {
                return persistencia.finalizar(tx.getId(), EstadoTransaccion.RECHAZADA, codigo,
                        "GestoPago confirmó que no se aplicó", idTx, resultado.getNumeroAutorizacion(), null, null, null);
            }
            return tx;
        } catch (RuntimeException ex) {
            log.warn("No se pudo confirmar la transacción GestoPago id={}", tx.getId());
            return tx;
        }
    }

    public TransaccionResponse consultar(UUID id, JwtAuthenticationToken auth) {
        boolean ejecutivo = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_EJECUTIVO"));
        return respuesta(persistencia.buscar(id, clienteId(auth), ejecutivo));
    }

    private TransaccionResponse pendiente(Transaccion tx, String codigo, String detalle) {
        Transaccion guardada = persistencia.finalizar(tx.getId(), EstadoTransaccion.CONCILIACION,
                codigo, detalle, null, null, null, null, null);
        log.warn("Transacción GestoPago en conciliación id={}", tx.getId());
        return respuesta(guardada);
    }

    private String resolverToken() {
        Optional<String> token = tokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo)
                .filter(t -> Boolean.TRUE.equals(t.getActivo()))
                .map(GestoPagoToken::getToken)
                .filter(StringUtils::isNotBlank);
        if (token.isEmpty()) {
            tokenService.renovarToken();
            token = tokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo)
                    .filter(t -> Boolean.TRUE.equals(t.getActivo()))
                    .map(GestoPagoToken::getToken)
                    .filter(StringUtils::isNotBlank);
        }
        return token.orElseThrow(() -> new GestoPagoAuthenticationException("No hay token de GestoPago disponible"));
    }

    private void validarProductoYMonto(CatProducto producto, DatosCompra request) {
        if (producto.getTipoFront() == null || (producto.getTipoFront() != 1 && producto.getTipoFront() != 2)) {
            throw new TransaccionException("El tipo de producto requiere un flujo de pago especializado", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (producto.getTipoFront() == 1) {
            if (producto.getPrecio() == null || request.getMonto().compareTo(producto.getPrecio()) != 0) {
                throw new TransaccionException("El monto debe coincidir con el precio vigente del producto", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        } else if (request.getMonto().compareTo(montoMinimo) < 0 || request.getMonto().compareTo(montoMaximo) > 0) {
            throw new TransaccionException("El monto está fuera del rango permitido", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        String tipoReferencia = StringUtils.defaultString(producto.getTipoReferencia()).toLowerCase();
        if (producto.getTipoFront() == 2 && (tipoReferencia.contains("b") || tipoReferencia.contains("c"))
                && StringUtils.isBlank(request.getReferencia())) {
            throw new TransaccionException("Falta la referencia requerida por el producto", HttpStatus.BAD_REQUEST);
        }
        if (producto.getTipoFront() == 1 && "a".equals(tipoReferencia)
                && (StringUtils.isBlank(request.getDestino()) || !request.getDestino().matches("^[0-9]{10}$"))) {
            throw new TransaccionException("El destino debe ser un teléfono de 10 dígitos", HttpStatus.BAD_REQUEST);
        }
        if (producto.getTipoFront() == 1 && StringUtils.isBlank(request.getDestino())) {
            throw new TransaccionException("El destino es obligatorio para este producto", HttpStatus.BAD_REQUEST);
        }
    }

    private Map<String, Object> payload(CatProducto producto, CrearTransaccionRequest request, String upc) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("idProducto", producto.getIdProducto());
        data.put("idServicio", producto.getIdServicio());
        data.put("telefono", producto.getTipoFront() == 1 ? request.getDestino() :
                ((producto.getIdCatTipoServicio() != null && (producto.getIdCatTipoServicio() == 10 || producto.getIdCatTipoServicio() == 11))
                        ? "#" + TELEFONO_SERVICIO : TELEFONO_SERVICIO));
        data.put("horaLocal", HORA_LOCAL.withZone(zonaHoraria).format(java.time.Instant.now()));
        data.put("upc", upc);
        data.put("unidad", unidad);
        if (StringUtils.isNotBlank(request.getReferencia())) data.put("referencia", request.getReferencia());
        if (producto.getTipoFront() == 2) data.put("montoPago", request.getMonto());
        return data;
    }

    /** 5512345678 -> ******5678. En el historial solo se muestran los ultimos 4 caracteres. */
    static String enmascarar(String referencia) {
        String valor = StringUtils.trimToNull(referencia);
        if (valor == null) return null;
        int visibles = Math.min(4, valor.length());
        String oculto = "*".repeat(Math.min(16, valor.length() - visibles));
        return oculto + valor.substring(valor.length() - visibles);
    }

    static TransaccionResponse respuestaBase(Transaccion tx) {
        String mensaje = switch (tx.getEstado()) {
            case APROBADA -> "Operación aprobada";
            case RECHAZADA -> "Operación rechazada; el saldo fue liberado";
            case CONCILIACION, PENDIENTE -> "Operación pendiente de confirmación; el monto está retenido";
        };
        TransaccionResponse response = new TransaccionResponse();
        response.setCodigo(tx.getEstado() == EstadoTransaccion.RECHAZADA ? CODIGO_RECHAZO : CODIGO_EXITO);
        response.setMensaje(mensaje);
        response.setId(tx.getId());
        response.setEstado(tx.getEstado());
        response.setMonto(tx.getMonto());
        response.setCodigoProveedor(tx.getCodigoProveedor());
        response.setIdTx(tx.getIdTx());
        response.setNumeroAutorizacion(tx.getNumeroAutorizacion());
        response.setPin(tx.getPin());
        response.setFolio(tx.getFolio());
        response.setLeyenda(tx.getLeyenda());
        response.setFechaCreacion(tx.getFechaCreacion());
        response.setIdProducto(tx.getIdProducto());
        response.setIdServicio(tx.getIdServicio());
        response.setProducto(tx.getProducto());
        response.setServicio(tx.getServicio());
        response.setReferencia(tx.getReferencia());
        response.setUpc(tx.getUpc());
        // Los mensajes de conciliacion son internos; solo se muestra el motivo de un rechazo
        if (tx.getEstado() == EstadoTransaccion.RECHAZADA) response.setMotivo(tx.getMensajeProveedor());
        return response;
    }

    /** Respuesta con los datos del ticket: imagen del servicio, cuenta enmascarada y saldo disponible. */
    private TransaccionResponse respuesta(Transaccion tx) {
        TransaccionResponse response = respuestaBase(tx);
        if (tx.getIdServicio() != null) response.setImagenUrl(urlImagenes + tx.getIdServicio() + ".png");
        if (tx.getCuentaId() != null) {
            TransaccionPersistenceService.InfoTicket info = persistencia.infoTicket(tx.getCuentaId());
            if (info != null) {
                response.setNumeroCuenta(enmascarar(info.numeroCuenta()));
                response.setSaldoDisponible(info.saldoDisponible());
                response.setNombreCliente(info.nombreCliente());
            }
        }
        return response;
    }

    /**
     * Valida que el producto exista en el catalogo vigente y que el monto, el destino y la
     * referencia sean correctos para ese producto. Se usa al pedir el token y al comprar.
     */
    public CatProducto validarCompra(DatosCompra request) {
        CatProducto producto = catalogo.findById(request.getIdProducto())
                .orElseThrow(() -> new TransaccionException("El producto no existe en el catálogo vigente", HttpStatus.UNPROCESSABLE_ENTITY));
        if (!request.getIdServicio().equals(producto.getIdServicio())) {
            throw new TransaccionException("El producto no corresponde al servicio indicado", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        validarProductoYMonto(producto, request);
        return producto;
    }

    public String huella(String cuenta, DatosCompra request) {
        try {
            Map<String, Object> canonical = new LinkedHashMap<>();
            canonical.put("cuenta", cuenta);
            canonical.put("idServicio", request.getIdServicio());
            canonical.put("idProducto", request.getIdProducto());
            canonical.put("destino", StringUtils.trimToEmpty(request.getDestino()));
            canonical.put("referencia", StringUtils.trimToEmpty(request.getReferencia()));
            canonical.put("monto", request.getMonto().stripTrailingZeros().toPlainString());
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(objectMapper.writeValueAsBytes(canonical)));
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible preparar la solicitud");
        }
    }

    private static void validarClave(String key) {
        if (key == null || !key.matches("^[A-Za-z0-9_-]{8,64}$")) {
            throw new TransaccionException("Idempotency-Key debe tener entre 8 y 64 caracteres válidos", HttpStatus.BAD_REQUEST);
        }
    }

    private static String crearUpc() {
        return Long.toString(RANDOM.nextLong(100_000_000_000_000L, 999_999_999_999_999L));
    }

    private static Integer clienteId(JwtAuthenticationToken auth) {
        Object value = auth.getToken().getClaim("clienteId");
        return value instanceof Number n ? n.intValue() : null;
    }
}
