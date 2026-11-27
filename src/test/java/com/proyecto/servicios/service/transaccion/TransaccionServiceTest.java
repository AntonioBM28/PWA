package com.proyecto.servicios.service.transaccion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoTransaccionesClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.mongo.CatProducto;
import com.proyecto.servicios.entity.onboarding.Transaccion;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.exception.TransaccionException;
import com.proyecto.servicios.model.gestopago.GestoPagoTransaccionResponse;
import com.proyecto.servicios.model.transaccion.TransaccionResponse;
import com.proyecto.servicios.model.transaccion.CrearTransaccionRequest;
import com.proyecto.servicios.repositorys.mongo.CatProductoRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.gestopago.GestoPagoPayloadCipher;
import com.proyecto.servicios.service.gestopago.GestoPagoTransaccionXmlParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TransaccionServiceTest {
    private CatProductoRepository catalogo;
    private TransaccionPersistenceService persistencia;
    private GestoPagoTokenService tokenService;
    private GestoPagoPayloadCipher cipher;
    private GestoPagoTransaccionesClient provider;
    private GestoPagoTransaccionXmlParser parser;
    private TransaccionService service;
    private CrearTransaccionRequest request;
    private CatProducto product;
    private JwtAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        catalogo = mock(CatProductoRepository.class);
        persistencia = mock(TransaccionPersistenceService.class);
        tokenService = mock(GestoPagoTokenService.class);
        cipher = mock(GestoPagoPayloadCipher.class);
        provider = mock(GestoPagoTransaccionesClient.class);
        parser = mock(GestoPagoTransaccionXmlParser.class);
        service = new TransaccionService(catalogo, persistencia, tokenService, cipher, provider, parser,
                new ObjectMapper(), "Sucursal 1", new BigDecimal("1.00"), new BigDecimal("10000.00"),
                "America/Mexico_City", 83, "GPS83-TPV-17");
        product = new CatProducto();
        product.setIdProducto(200);
        product.setIdServicio(71);
        product.setTipoFront(1);
        product.setTipoReferencia("a");
        product.setPrecio(new BigDecimal("50.00"));
        request = new CrearTransaccionRequest();
        request.setIdProducto(200);
        request.setIdServicio(71);
        request.setDestino("5512345678");
        request.setMonto(new BigDecimal("50.00"));
        Jwt jwt = Jwt.withTokenValue("jwt").header("alg", "HS256").claim("clienteId", 9).build();
        authentication = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));
    }

    @Test
    void rechazaPrecioManipuladoAntesDeReservarSaldo() {
        product.setPrecio(new BigDecimal("100.00"));
        when(catalogo.findById(200)).thenReturn(Optional.of(product));

        assertThrows(TransaccionException.class,
                () -> service.comprar("1234567890", "idempo-123", request, authentication));

        verify(persistencia).buscarPorClave("1234567890", 9, false, "idempo-123");
        verify(persistencia, never()).reservar(anyString(), any(), anyBoolean(), anyString(), anyString(),
                anyString(), anyString(), anyString(), any(), any());
        verifyNoInteractions(provider);
    }

    @Test
    void enviaUnaSolaPeticionYConfirmaRespuestaExitosa() {
        when(catalogo.findById(200)).thenReturn(Optional.of(product));
        when(persistencia.buscarPorClave("1234567890", 9, false, "idempo-123")).thenReturn(Optional.empty());
        GestoPagoToken token = new GestoPagoToken();
        token.setToken("provider-token");
        token.setActivo(true);
        when(tokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(token));
        when(cipher.cifrar(anyMap())).thenReturn("provider-encrypted");
        when(cipher.protegerParaAlmacenamiento("provider-encrypted")).thenReturn("local-encrypted");
        Transaccion pending = transaction(EstadoTransaccion.PENDIENTE);
        when(persistencia.reservar(eq("1234567890"), eq(9), eq(false), eq("idempo-123"), anyString(),
                anyString(), anyString(), eq("local-encrypted"), eq(new BigDecimal("50.00")), any()))
                .thenReturn(new TransaccionPersistenceService.Reserva(pending, false));
        when(provider.enviar(eq("provider-token"), anyString(), eq("provider-encrypted"))).thenReturn("<RESPONSE/>");
        GestoPagoTransaccionResponse response = new GestoPagoTransaccionResponse();
        response.setMonto(new BigDecimal("50.00"));
        var message = new GestoPagoTransaccionResponse.GestoPagoTransaccionMensaje();
        message.setCodigo("01");
        message.setTexto("Operación realizada");
        response.setMensaje(message);
        when(parser.parse("<RESPONSE/>")).thenReturn(response);
        Transaccion approved = transaction(EstadoTransaccion.APROBADA);
        approved.setNumeroAutorizacion("AUTH-1");
        when(persistencia.finalizar(eq(pending.getId()), eq(EstadoTransaccion.APROBADA), eq("01"),
                anyString(), any(), any(), any(), any(), any())).thenReturn(approved);

        var result = service.comprar("1234567890", "idempo-123", request, authentication);

        assertEquals(EstadoTransaccion.APROBADA, result.getEstado());
        assertEquals("AUTH-1", result.getNumeroAutorizacion());
        verify(provider, times(1)).enviar(eq("provider-token"), anyString(), eq("provider-encrypted"));
        verify(provider, never()).confirmar(anyString(), anyString(), anyString());
    }

    @Test
    void clasificaTimeoutComoConciliacionYConservaElDebitoReservado() {
        when(catalogo.findById(200)).thenReturn(Optional.of(product));
        when(persistencia.buscarPorClave("1234567890", 9, false, "idempo-123")).thenReturn(Optional.empty());
        GestoPagoToken token = new GestoPagoToken();
        token.setToken("provider-token");
        token.setActivo(true);
        when(tokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(token));
        when(cipher.cifrar(anyMap())).thenReturn("provider-encrypted");
        when(cipher.protegerParaAlmacenamiento("provider-encrypted")).thenReturn("local-encrypted");
        Transaccion pending = transaction(EstadoTransaccion.PENDIENTE);
        when(persistencia.reservar(eq("1234567890"), eq(9), eq(false), eq("idempo-123"), anyString(),
                anyString(), anyString(), eq("local-encrypted"), eq(new BigDecimal("50.00")), any()))
                .thenReturn(new TransaccionPersistenceService.Reserva(pending, false));
        when(provider.enviar(eq("provider-token"), anyString(), eq("provider-encrypted")))
                .thenThrow(new GestoPagoTransaccionesClient.ProviderCommunicationException());
        Transaccion pendingConfirmation = transaction(EstadoTransaccion.CONCILIACION);
        when(persistencia.finalizar(eq(pending.getId()), eq(EstadoTransaccion.CONCILIACION),
                eq("PROVIDER_UNKNOWN"), anyString(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(pendingConfirmation);

        var result = service.comprar("1234567890", "idempo-123", request, authentication);

        assertEquals(EstadoTransaccion.CONCILIACION, result.getEstado());
        verify(persistencia).finalizar(eq(pending.getId()), eq(EstadoTransaccion.CONCILIACION),
                eq("PROVIDER_UNKNOWN"), anyString(), isNull(), isNull(), isNull(), isNull(), isNull());
    }

    // ─────────────────────────────────────────────────────────────
    // validarClave
    // ─────────────────────────────────────────────────────────────

    @Test
    void rechazaClaveIdempotenciaInvalida() {
        assertThrows(TransaccionException.class,
                () -> service.comprar("1234567890", "corto", request, authentication));
        verifyNoInteractions(catalogo, persistencia, provider);
    }

    @Test
    void rechazaClaveIdempotenciaConCaracteresInvalidos() {
        assertThrows(TransaccionException.class,
                () -> service.comprar("1234567890", "clave con espacios", request, authentication));
        verifyNoInteractions(catalogo, persistencia, provider);
    }

    // ─────────────────────────────────────────────────────────────
    // idempotencia — solicitud previa existente
    // ─────────────────────────────────────────────────────────────

    @Test
    void devuelveResultadoPrevioConMismosDataIdempotente() {
        Transaccion previa = transaction(EstadoTransaccion.APROBADA);
        previa.setHuellaSolicitud(huellaDe("1234567890", request));
        when(persistencia.buscarPorClave("1234567890", 9, false, "idempo-123"))
                .thenReturn(Optional.of(previa));

        var result = service.comprar("1234567890", "idempo-123", request, authentication);

        assertEquals(EstadoTransaccion.APROBADA, result.getEstado());
        verifyNoInteractions(catalogo, provider);
    }

    @Test
    void rechazaReutilizacionDeClaveConDatosDiferentes() {
        Transaccion previa = transaction(EstadoTransaccion.APROBADA);
        previa.setHuellaSolicitud("huella-distinta-que-no-coincide-con-request");
        when(persistencia.buscarPorClave("1234567890", 9, false, "idempo-123"))
                .thenReturn(Optional.of(previa));

        assertThrows(TransaccionException.class,
                () -> service.comprar("1234567890", "idempo-123", request, authentication));
        verifyNoInteractions(provider);
    }

    // ─────────────────────────────────────────────────────────────
    // validaciones de producto y monto
    // ─────────────────────────────────────────────────────────────

    @Test
    void rechazaCuandoProductoNoExisteEnCatalogo() {
        when(persistencia.buscarPorClave(anyString(), anyInt(), anyBoolean(), anyString()))
                .thenReturn(Optional.empty());
        when(catalogo.findById(200)).thenReturn(Optional.empty());

        assertThrows(TransaccionException.class,
                () -> service.comprar("1234567890", "idempo-123", request, authentication));
    }

    @Test
    void rechazaCuandoIdServicioNoCorrespondeAlProducto() {
        request.setIdServicio(99); // distinto del producto (71)
        when(persistencia.buscarPorClave(anyString(), anyInt(), anyBoolean(), anyString()))
                .thenReturn(Optional.empty());
        when(catalogo.findById(200)).thenReturn(Optional.of(product));

        assertThrows(TransaccionException.class,
                () -> service.comprar("1234567890", "idempo-123", request, authentication));
    }

    @Test
    void rechazaTipoFrontInvalidoNulo() {
        product.setTipoFront(null);
        when(persistencia.buscarPorClave(anyString(), anyInt(), anyBoolean(), anyString()))
                .thenReturn(Optional.empty());
        when(catalogo.findById(200)).thenReturn(Optional.of(product));

        assertThrows(TransaccionException.class,
                () -> service.comprar("1234567890", "idempo-123", request, authentication));
    }

    @Test
    void rechazaTipoFrontInvalidoValorDesconocido() {
        product.setTipoFront(99);
        when(persistencia.buscarPorClave(anyString(), anyInt(), anyBoolean(), anyString()))
                .thenReturn(Optional.empty());
        when(catalogo.findById(200)).thenReturn(Optional.of(product));

        assertThrows(TransaccionException.class,
                () -> service.comprar("1234567890", "idempo-123", request, authentication));
    }

    @Test
    void rechazaTipoFront2ConMontoFueraDeRangoMinimo() {
        product.setTipoFront(2);
        product.setTipoReferencia("");
        request.setMonto(new BigDecimal("0.00"));
        when(persistencia.buscarPorClave(anyString(), anyInt(), anyBoolean(), anyString()))
                .thenReturn(Optional.empty());
        when(catalogo.findById(200)).thenReturn(Optional.of(product));

        assertThrows(TransaccionException.class,
                () -> service.comprar("1234567890", "idempo-123", request, authentication));
    }

    @Test
    void rechazaTipoFront2ConMontoFueraDeRangoMaximo() {
        product.setTipoFront(2);
        product.setTipoReferencia("");
        request.setMonto(new BigDecimal("99999.00"));
        when(persistencia.buscarPorClave(anyString(), anyInt(), anyBoolean(), anyString()))
                .thenReturn(Optional.empty());
        when(catalogo.findById(200)).thenReturn(Optional.of(product));

        assertThrows(TransaccionException.class,
                () -> service.comprar("1234567890", "idempo-123", request, authentication));
    }

    @Test
    void rechazaTipoFront2ConReferenciaObligatoriaAusente() {
        product.setTipoFront(2);
        product.setTipoReferencia("b"); // requiere referencia
        request.setMonto(new BigDecimal("100.00"));
        request.setReferencia(null);
        when(persistencia.buscarPorClave(anyString(), anyInt(), anyBoolean(), anyString()))
                .thenReturn(Optional.empty());
        when(catalogo.findById(200)).thenReturn(Optional.of(product));

        assertThrows(TransaccionException.class,
                () -> service.comprar("1234567890", "idempo-123", request, authentication));
    }

    @Test
    void rechazaTipoFront1SinDestino() {
        product.setTipoReferencia("x"); // no es "a", así que no valida formato teléfono
        request.setDestino(null);
        when(persistencia.buscarPorClave(anyString(), anyInt(), anyBoolean(), anyString()))
                .thenReturn(Optional.empty());
        when(catalogo.findById(200)).thenReturn(Optional.of(product));

        assertThrows(TransaccionException.class,
                () -> service.comprar("1234567890", "idempo-123", request, authentication));
    }

    @Test
    void rechazaTipoFront1TipoReferenciaAConDestinoInvalido() {
        product.setTipoReferencia("a");
        request.setDestino("123"); // no tiene 10 dígitos
        when(persistencia.buscarPorClave(anyString(), anyInt(), anyBoolean(), anyString()))
                .thenReturn(Optional.empty());
        when(catalogo.findById(200)).thenReturn(Optional.of(product));

        assertThrows(TransaccionException.class,
                () -> service.comprar("1234567890", "idempo-123", request, authentication));
    }

    // ─────────────────────────────────────────────────────────────
    // unidad no configurada
    // ─────────────────────────────────────────────────────────────

    /** GestoPago no disponible: la operacion se registra RECHAZADA sin cargo y nunca se envia. */
    private void prepararRechazoSinCargo() {
        Transaccion rechazada = transaction(EstadoTransaccion.RECHAZADA);
        rechazada.setMensajeProveedor(TransaccionService.MOTIVO_NO_DISPONIBLE);
        rechazada.setCodigoProveedor(TransaccionService.CODIGO_NO_DISPONIBLE);
        rechazada.setIdServicio(71);
        rechazada.setCuentaId(3);
        when(persistencia.registrarRechazo(eq("1234567890"), eq(9), eq(false), eq("idempo-123"), anyString(),
                anyString(), eq(new BigDecimal("50.00")), any(), eq(TransaccionService.CODIGO_NO_DISPONIBLE),
                eq(TransaccionService.MOTIVO_NO_DISPONIBLE)))
                .thenReturn(new TransaccionPersistenceService.Reserva(rechazada, false));
        when(persistencia.infoTicket(3)).thenReturn(new TransaccionPersistenceService.InfoTicket(
                "1000000009", new BigDecimal("500.00"), "Jose Balderas"));
    }

    private void verificarRechazoSinCargo(TransaccionResponse result) {
        assertEquals(EstadoTransaccion.RECHAZADA, result.getEstado());
        assertEquals(TransaccionService.MOTIVO_NO_DISPONIBLE, result.getMotivo());
        assertEquals("******0009", result.getNumeroCuenta());
        assertEquals(new BigDecimal("500.00"), result.getSaldoDisponible());
        assertEquals("Jose Balderas", result.getNombreCliente());
        assertTrue(result.getImagenUrl().endsWith("/71.png"));
        verify(persistencia, never()).reservar(any(), any(), anyBoolean(), any(), any(), any(), any(), any(), any(), any());
        verifyNoInteractions(provider);
    }

    @Test
    void sinUnidadConfigurada_rechazaSinCargoConTicket() {
        TransaccionService sinUnidad = new TransaccionService(
                catalogo, persistencia, tokenService, cipher, provider, parser,
                new ObjectMapper(), "", new BigDecimal("1.00"), new BigDecimal("10000.00"),
                "America/Mexico_City", 83, "GPS83-TPV-17");
        when(catalogo.findById(200)).thenReturn(Optional.of(product));
        when(persistencia.buscarPorClave(anyString(), anyInt(), anyBoolean(), anyString()))
                .thenReturn(Optional.empty());
        prepararRechazoSinCargo();

        verificarRechazoSinCargo(sinUnidad.comprar("1234567890", "idempo-123", request, authentication));
    }

    // ─────────────────────────────────────────────────────────────
    // resolverToken — renovación automática
    // ─────────────────────────────────────────────────────────────

    @Test
    void sinTokenDeGestoPagoTrasRenovar_rechazaSinCargo() {
        when(catalogo.findById(200)).thenReturn(Optional.of(product));
        when(persistencia.buscarPorClave(anyString(), anyInt(), anyBoolean(), anyString()))
                .thenReturn(Optional.empty());
        when(tokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.empty());
        prepararRechazoSinCargo();

        verificarRechazoSinCargo(service.comprar("1234567890", "idempo-123", request, authentication));
        verify(tokenService, times(1)).renovarToken();
    }

    @Test
    void renovaTokenSiElActivoEstaInactivo() {
        when(catalogo.findById(200)).thenReturn(Optional.of(product));
        when(persistencia.buscarPorClave(anyString(), anyInt(), anyBoolean(), anyString()))
                .thenReturn(Optional.empty());
        GestoPagoToken inactivo = new GestoPagoToken();
        inactivo.setToken("viejo");
        inactivo.setActivo(false);
        when(tokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(inactivo));
        prepararRechazoSinCargo();

        verificarRechazoSinCargo(service.comprar("1234567890", "idempo-123", request, authentication));
        verify(tokenService).renovarToken();
    }

    @Test
    void falloAlCifrar_rechazaSinCargo() {
        when(catalogo.findById(200)).thenReturn(Optional.of(product));
        when(persistencia.buscarPorClave(anyString(), anyInt(), anyBoolean(), anyString()))
                .thenReturn(Optional.empty());
        GestoPagoToken token = new GestoPagoToken();
        token.setToken("provider-token");
        token.setActivo(true);
        when(tokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(token));
        when(cipher.cifrar(anyMap())).thenThrow(new IllegalStateException("sin llave"));
        prepararRechazoSinCargo();

        verificarRechazoSinCargo(service.comprar("1234567890", "idempo-123", request, authentication));
    }

    // ─────────────────────────────────────────────────────────────
    // reserva ya existente (race condition idempotente en reservar)
    // ─────────────────────────────────────────────────────────────

    @Test
    void devuelveRespuestaDirectaSiReservaYaExistia() {
        Transaccion existente = transaction(EstadoTransaccion.APROBADA);
        when(catalogo.findById(200)).thenReturn(Optional.of(product));
        when(persistencia.buscarPorClave(anyString(), anyInt(), anyBoolean(), anyString()))
                .thenReturn(Optional.empty());

        GestoPagoToken token = tokenActivo();
        when(tokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(token));
        when(cipher.cifrar(anyMap())).thenReturn("enc");
        when(cipher.protegerParaAlmacenamiento("enc")).thenReturn("local-enc");
        when(persistencia.reservar(any(), any(), anyBoolean(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new TransaccionPersistenceService.Reserva(existente, true));

        var result = service.comprar("1234567890", "idempo-123", request, authentication);

        assertEquals(EstadoTransaccion.APROBADA, result.getEstado());
        verifyNoInteractions(provider);
    }

    // ─────────────────────────────────────────────────────────────
    // procesarRespuesta — distintos códigos de GestoPago
    // ─────────────────────────────────────────────────────────────

    @Test
    void clasificaRespuestaNulaComoConciliacion() {
        prepararCompraConProvider(null);

        var result = service.comprar("1234567890", "idempo-123", request, authentication);

        assertEquals(EstadoTransaccion.CONCILIACION, result.getEstado());
    }

    @Test
    void clasificaRespuestaConMensajeNuloComoConciliacion() {
        GestoPagoTransaccionResponse respuesta = new GestoPagoTransaccionResponse();
        respuesta.setMensaje(null);
        prepararCompraConProvider(respuesta);

        var result = service.comprar("1234567890", "idempo-123", request, authentication);

        assertEquals(EstadoTransaccion.CONCILIACION, result.getEstado());
    }

    @Test
    void clasificaRespuestaConCodigoVacioComoConciliacion() {
        GestoPagoTransaccionResponse respuesta = new GestoPagoTransaccionResponse();
        var msg = new GestoPagoTransaccionResponse.GestoPagoTransaccionMensaje();
        msg.setCodigo("");
        respuesta.setMensaje(msg);
        prepararCompraConProvider(respuesta);

        var result = service.comprar("1234567890", "idempo-123", request, authentication);

        assertEquals(EstadoTransaccion.CONCILIACION, result.getEstado());
    }

    @Test
    void clasificaCodigo01ConMontoDiferenteComoConciliacion() {
        GestoPagoTransaccionResponse respuesta = respuestaConCodigo("01");
        respuesta.setMonto(new BigDecimal("99.00")); // distinto al monto de la tx (50.00)
        prepararCompraConProvider(respuesta);

        var result = service.comprar("1234567890", "idempo-123", request, authentication);

        assertEquals(EstadoTransaccion.CONCILIACION, result.getEstado());
    }

    @Test
    void clasificaCodigo06ComoConciliacion() {
        prepararCompraConProvider(respuestaConCodigo("06"));

        var result = service.comprar("1234567890", "idempo-123", request, authentication);

        assertEquals(EstadoTransaccion.CONCILIACION, result.getEstado());
        assertEquals(TransaccionService.CODIGO_EXITO, result.getCodigo());
    }

    @Test
    void clasificaCodigo82ComoConciliacion() {
        prepararCompraConProvider(respuestaConCodigo("82"));

        var result = service.comprar("1234567890", "idempo-123", request, authentication);

        assertEquals(EstadoTransaccion.CONCILIACION, result.getEstado());
    }

    @Test
    void clasificaCodigoDesconocidoComoRechazada() {
        prepararCompraConProvider(respuestaConCodigo("99"));

        var result = service.comprar("1234567890", "idempo-123", request, authentication);

        assertEquals(EstadoTransaccion.RECHAZADA, result.getEstado());
        assertEquals(TransaccionService.CODIGO_RECHAZO, result.getCodigo());
    }

    // ─────────────────────────────────────────────────────────────
    // HTTP 4xx → RECHAZADA; 5xx → CONCILIACION
    // ─────────────────────────────────────────────────────────────

    @Test
    void clasificaProviderHttp4xxComoRechazada() {
        Transaccion pending = transaction(EstadoTransaccion.PENDIENTE);
        Transaccion rechazada = transaction(EstadoTransaccion.RECHAZADA);
        prepararBaseCompra(pending);
        when(provider.enviar(anyString(), anyString(), anyString()))
                .thenThrow(new GestoPagoTransaccionesClient.ProviderHttpException(422));
        when(persistencia.finalizar(eq(pending.getId()), eq(EstadoTransaccion.RECHAZADA),
                anyString(), anyString(), any(), any(), any(), any(), any()))
                .thenReturn(rechazada);

        var result = service.comprar("1234567890", "idempo-123", request, authentication);

        assertEquals(EstadoTransaccion.RECHAZADA, result.getEstado());
    }

    @Test
    void clasificaProviderHttp5xxComoConciliacion() {
        Transaccion pending = transaction(EstadoTransaccion.PENDIENTE);
        Transaccion conciliacion = transaction(EstadoTransaccion.CONCILIACION);
        prepararBaseCompra(pending);
        when(provider.enviar(anyString(), anyString(), anyString()))
                .thenThrow(new GestoPagoTransaccionesClient.ProviderHttpException(503));
        when(persistencia.finalizar(eq(pending.getId()), eq(EstadoTransaccion.CONCILIACION),
                anyString(), anyString(), any(), any(), any(), any(), any()))
                .thenReturn(conciliacion);

        var result = service.comprar("1234567890", "idempo-123", request, authentication);

        assertEquals(EstadoTransaccion.CONCILIACION, result.getEstado());
    }

    @Test
    void clasificaIllegalArgumentExceptionComoConciliacion() {
        Transaccion pending = transaction(EstadoTransaccion.PENDIENTE);
        Transaccion conciliacion = transaction(EstadoTransaccion.CONCILIACION);
        prepararBaseCompra(pending);
        when(provider.enviar(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("bad arg"));
        when(persistencia.finalizar(eq(pending.getId()), eq(EstadoTransaccion.CONCILIACION),
                anyString(), anyString(), any(), any(), any(), any(), any()))
                .thenReturn(conciliacion);

        var result = service.comprar("1234567890", "idempo-123", request, authentication);

        assertEquals(EstadoTransaccion.CONCILIACION, result.getEstado());
    }

    // ─────────────────────────────────────────────────────────────
    // confirmar
    // ─────────────────────────────────────────────────────────────

    @Test
    void confirmaCodigo06ConAutorizacionComoAprobada() {
        Transaccion tx = transaction(EstadoTransaccion.CONCILIACION);
        tx.setRandomProveedor("random-1");
        tx.setPayloadCifrado("enc");

        GestoPagoToken token = tokenActivo();
        when(tokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(token));
        when(cipher.recuperarParaConfirmacion("enc")).thenReturn("dec");
        when(provider.confirmar(anyString(), anyString(), anyString())).thenReturn("<XML/>");

        GestoPagoTransaccionResponse respuesta = new GestoPagoTransaccionResponse();
        respuesta.setNumeroAutorizacion("AUTH-99");
        var msg = new GestoPagoTransaccionResponse.GestoPagoTransaccionMensaje();
        msg.setCodigo("06");
        respuesta.setMensaje(msg);
        when(parser.parse("<XML/>")).thenReturn(respuesta);

        Transaccion aprobada = transaction(EstadoTransaccion.APROBADA);
        when(persistencia.finalizar(eq(tx.getId()), eq(EstadoTransaccion.APROBADA),
                anyString(), anyString(), any(), anyString(), any(), any(), any()))
                .thenReturn(aprobada);

        Transaccion result = service.confirmar(tx);

        assertEquals(EstadoTransaccion.APROBADA, result.getEstado());
    }

    @Test
    void confirmaCodigo70ComoRechazada() {
        Transaccion tx = transaction(EstadoTransaccion.CONCILIACION);
        tx.setRandomProveedor("random-1");
        tx.setPayloadCifrado("enc");

        GestoPagoToken token = tokenActivo();
        when(tokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(token));
        when(cipher.recuperarParaConfirmacion("enc")).thenReturn("dec");
        when(provider.confirmar(anyString(), anyString(), anyString())).thenReturn("<XML/>");

        GestoPagoTransaccionResponse respuesta = new GestoPagoTransaccionResponse();
        var msg = new GestoPagoTransaccionResponse.GestoPagoTransaccionMensaje();
        msg.setCodigo("70");
        respuesta.setMensaje(msg);
        when(parser.parse("<XML/>")).thenReturn(respuesta);

        Transaccion rechazada = transaction(EstadoTransaccion.RECHAZADA);
        when(persistencia.finalizar(eq(tx.getId()), eq(EstadoTransaccion.RECHAZADA),
                anyString(), anyString(), any(), any(), any(), any(), any()))
                .thenReturn(rechazada);

        Transaccion result = service.confirmar(tx);

        assertEquals(EstadoTransaccion.RECHAZADA, result.getEstado());
    }

    @Test
    void confirmarRetornaTxOriginalSiRespuestaEsNula() {
        Transaccion tx = transaction(EstadoTransaccion.CONCILIACION);
        tx.setRandomProveedor("random-1");
        tx.setPayloadCifrado("enc");

        GestoPagoToken token = tokenActivo();
        when(tokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(token));
        when(cipher.recuperarParaConfirmacion("enc")).thenReturn("dec");
        when(provider.confirmar(anyString(), anyString(), anyString())).thenReturn("<XML/>");
        when(parser.parse("<XML/>")).thenReturn(null);

        Transaccion result = service.confirmar(tx);

        assertSame(tx, result);
    }

    @Test
    void confirmarRetornaTxOriginalSiCodigoNoReconocido() {
        Transaccion tx = transaction(EstadoTransaccion.CONCILIACION);
        tx.setRandomProveedor("random-1");
        tx.setPayloadCifrado("enc");

        GestoPagoToken token = tokenActivo();
        when(tokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(token));
        when(cipher.recuperarParaConfirmacion("enc")).thenReturn("dec");
        when(provider.confirmar(anyString(), anyString(), anyString())).thenReturn("<XML/>");

        GestoPagoTransaccionResponse respuesta = new GestoPagoTransaccionResponse();
        var msg = new GestoPagoTransaccionResponse.GestoPagoTransaccionMensaje();
        msg.setCodigo("99");
        respuesta.setMensaje(msg);
        when(parser.parse("<XML/>")).thenReturn(respuesta);

        Transaccion result = service.confirmar(tx);

        assertSame(tx, result);
    }

    @Test
    void confirmarAbsorbeExcepcionYDevuelveTxOriginal() {
        Transaccion tx = transaction(EstadoTransaccion.CONCILIACION);
        tx.setRandomProveedor("random-1");
        tx.setPayloadCifrado("enc");

        when(tokenService.obtenerTokenActivo(83, "GPS83-TPV-17"))
                .thenThrow(new RuntimeException("fallo red"));

        Transaccion result = service.confirmar(tx);

        assertSame(tx, result);
    }

    // ─────────────────────────────────────────────────────────────
    // consultar
    // ─────────────────────────────────────────────────────────────

    @Test
    void consultaTransaccionComoCliente() {
        UUID id = UUID.randomUUID();
        Transaccion tx = transaction(EstadoTransaccion.APROBADA);
        when(persistencia.buscar(id, 9, false)).thenReturn(tx);

        var result = service.consultar(id, authentication);

        assertEquals(EstadoTransaccion.APROBADA, result.getEstado());
    }

    @Test
    void consultaTransaccionComoEjecutivo() {
        UUID id = UUID.randomUUID();
        Transaccion tx = transaction(EstadoTransaccion.RECHAZADA);
        Jwt jwt = Jwt.withTokenValue("jwt").header("alg", "HS256").claim("clienteId", 1).build();
        JwtAuthenticationToken ejecutivo = new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority("ROLE_EJECUTIVO")));
        when(persistencia.buscar(id, 1, true)).thenReturn(tx);

        var result = service.consultar(id, ejecutivo);

        assertEquals(EstadoTransaccion.RECHAZADA, result.getEstado());
    }

    // ─────────────────────────────────────────────────────────────
    // mensaje en respuesta según estado
    // ─────────────────────────────────────────────────────────────

    @Test
    void mensajeAprobadaEsCorrecto() {
        UUID id = UUID.randomUUID();
        Transaccion tx = transaction(EstadoTransaccion.APROBADA);
        when(persistencia.buscar(id, 9, false)).thenReturn(tx);

        var result = service.consultar(id, authentication);

        assertEquals("Operación aprobada", result.getMensaje());
    }

    @Test
    void mensajeRechazadaEsCorrecto() {
        UUID id = UUID.randomUUID();
        Transaccion tx = transaction(EstadoTransaccion.RECHAZADA);
        when(persistencia.buscar(id, 9, false)).thenReturn(tx);

        var result = service.consultar(id, authentication);

        assertTrue(result.getMensaje().contains("rechazada"));
    }

    @Test
    void mensajePendienteEsCorrecto() {
        UUID id = UUID.randomUUID();
        Transaccion tx = transaction(EstadoTransaccion.PENDIENTE);
        when(persistencia.buscar(id, 9, false)).thenReturn(tx);

        var result = service.consultar(id, authentication);

        assertTrue(result.getMensaje().contains("pendiente"));
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────

    private static Transaccion transaction(EstadoTransaccion state) {
        Transaccion tx = new Transaccion();
        tx.setId(UUID.randomUUID());
        tx.setEstado(state);
        tx.setMonto(new BigDecimal("50.00"));
        return tx;
    }

    private static GestoPagoToken tokenActivo() {
        GestoPagoToken t = new GestoPagoToken();
        t.setToken("provider-token");
        t.setActivo(true);
        return t;
    }

    private static GestoPagoTransaccionResponse respuestaConCodigo(String codigo) {
        GestoPagoTransaccionResponse r = new GestoPagoTransaccionResponse();
        r.setMonto(new BigDecimal("50.00"));
        var msg = new GestoPagoTransaccionResponse.GestoPagoTransaccionMensaje();
        msg.setCodigo(codigo);
        r.setMensaje(msg);
        return r;
    }

    /** Configura mocks comunes para comprar() hasta el punto de llamar a provider.enviar() */
    private void prepararBaseCompra(Transaccion pending) {
        when(catalogo.findById(200)).thenReturn(Optional.of(product));
        when(persistencia.buscarPorClave(anyString(), anyInt(), anyBoolean(), anyString()))
                .thenReturn(Optional.empty());
        GestoPagoToken token = tokenActivo();
        when(tokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(token));
        when(cipher.cifrar(anyMap())).thenReturn("enc");
        when(cipher.protegerParaAlmacenamiento("enc")).thenReturn("local-enc");
        when(persistencia.reservar(any(), any(), anyBoolean(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new TransaccionPersistenceService.Reserva(pending, false));
    }

    /** Variante de prepararBaseCompra que también configura provider y persistencia.finalizar */
    private void prepararCompraConProvider(GestoPagoTransaccionResponse respuestaProvider) {
        Transaccion pending = transaction(EstadoTransaccion.PENDIENTE);
        prepararBaseCompra(pending);
        when(provider.enviar(anyString(), anyString(), anyString())).thenReturn("<XML/>");
        when(parser.parse("<XML/>")).thenReturn(respuestaProvider);
        // Devuelve la transacción con el estado que el servicio decidió, para validar la clasificación real
        when(persistencia.finalizar(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> transaction(inv.getArgument(1, EstadoTransaccion.class)));
    }

    /** Calcula la huella de una solicitud igual a como lo hace el servicio (para test idempotencia). */
    private String huellaDe(String cuenta, CrearTransaccionRequest req) {
        try {
            java.util.Map<String, Object> canonical = new java.util.LinkedHashMap<>();
            canonical.put("cuenta", cuenta);
            canonical.put("idServicio", req.getIdServicio());
            canonical.put("idProducto", req.getIdProducto());
            canonical.put("destino", org.apache.commons.lang3.StringUtils.trimToEmpty(req.getDestino()));
            canonical.put("referencia", org.apache.commons.lang3.StringUtils.trimToEmpty(req.getReferencia()));
            canonical.put("monto", req.getMonto().stripTrailingZeros().toPlainString());
            return java.util.HexFormat.of().formatHex(
                    java.security.MessageDigest.getInstance("SHA-256")
                            .digest(new ObjectMapper().writeValueAsBytes(canonical)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
