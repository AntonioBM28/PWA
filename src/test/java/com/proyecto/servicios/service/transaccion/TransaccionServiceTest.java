package com.proyecto.servicios.service.transaccion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.GestoPagoTransaccionesClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.mongo.CatProducto;
import com.proyecto.servicios.entity.onboarding.Transaccion;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.exception.TransaccionException;
import com.proyecto.servicios.model.gestopago.GestoPagoTransaccionResponse;
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
                anyString(), anyString(), anyString(), any());
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
                anyString(), anyString(), eq("local-encrypted"), eq(new BigDecimal("50.00"))))
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
                anyString(), anyString(), eq("local-encrypted"), eq(new BigDecimal("50.00"))))
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

    private static Transaccion transaction(EstadoTransaccion state) {
        Transaccion tx = new Transaccion();
        tx.setId(UUID.randomUUID());
        tx.setEstado(state);
        tx.setMonto(new BigDecimal("50.00"));
        return tx;
    }
}
