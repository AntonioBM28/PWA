package com.proyecto.servicios.service.transaccion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoTransaccionesClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.mongo.CatProducto;
import com.proyecto.servicios.entity.onboarding.Transaccion;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.exception.GestoPagoAuthenticationException;
import com.proyecto.servicios.exception.TransaccionException;
import com.proyecto.servicios.model.gestopago.GestoPagoTransaccionResponse;
import com.proyecto.servicios.model.transaccion.CrearTransaccionRequest;
import com.proyecto.servicios.model.transaccion.TransaccionResponse;
import com.proyecto.servicios.repositorys.mongo.CatProductoRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
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

        CatProducto producto = catalogo.findById(request.getIdProducto())
                .orElseThrow(() -> new TransaccionException("El producto no existe en el catálogo vigente", HttpStatus.UNPROCESSABLE_ENTITY));
        if (!request.getIdServicio().equals(producto.getIdServicio())) {
            throw new TransaccionException("El producto no corresponde al servicio indicado", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        validarProductoYMonto(producto, request);
        if (unidad == null || unidad.length() < 3 || unidad.length() > 25) {
            throw new GestoPagoAuthenticationException("La unidad de GestoPago no está configurada");
        }

        String token = resolverToken();
        String upc = crearUpc();
        String random = Long.toString(System.currentTimeMillis());
        String signed = cipher.cifrar(payload(producto, request, upc));
        String payloadPersistido = cipher.protegerParaAlmacenamiento(signed);
        TransaccionPersistenceService.Reserva reserva = persistencia.reservar(
                numeroCuenta, clienteAutenticado, ejecutivo, idempotencyKey, huella, upc, random, payloadPersistido, request.getMonto());
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

    private void validarProductoYMonto(CatProducto producto, CrearTransaccionRequest request) {
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

    private static TransaccionResponse respuesta(Transaccion tx) {
        String mensaje = switch (tx.getEstado()) {
            case APROBADA -> "Operación aprobada";
            case RECHAZADA -> "Operación rechazada; el saldo fue liberado";
            case CONCILIACION, PENDIENTE -> "Operación pendiente de confirmación; el monto está retenido";
        };
        return TransaccionResponse.builder().id(tx.getId()).estado(tx.getEstado()).monto(tx.getMonto())
                .codigoProveedor(tx.getCodigoProveedor()).idTx(tx.getIdTx())
                .numeroAutorizacion(tx.getNumeroAutorizacion()).pin(tx.getPin()).folio(tx.getFolio())
                .leyenda(tx.getLeyenda()).fechaCreacion(tx.getFechaCreacion()).mensaje(mensaje).build();
    }

    private String huella(String cuenta, CrearTransaccionRequest request) {
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
