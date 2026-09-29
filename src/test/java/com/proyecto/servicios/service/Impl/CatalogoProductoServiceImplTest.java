package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductosClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.mongo.CatProducto;
import com.proyecto.servicios.exception.CatalogoProductosException;
import com.proyecto.servicios.exception.GestoPagoAuthenticationException;
import com.proyecto.servicios.exception.GestoPagoCommunicationException;
import com.proyecto.servicios.exception.GestoPagoResponseException;
import com.proyecto.servicios.exception.GestoPagoTimeoutException;
import com.proyecto.servicios.mapper.GestoPagoProductoMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoMensaje;
import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoProducto;
import com.proyecto.servicios.repositorys.mongo.CatProductoRepository;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.Request;
import feign.RetryableException;
import feign.codec.DecodeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogoProductoServiceImplTest {

    private static final String TOKEN_CONFIGURADO = "token-configurado";
    private static final String TOKEN_RENOVADO = "token-renovado";
    private static final Integer ID_DISTRIBUIDOR = 10;
    private static final String CODIGO_DISPOSITIVO = "DISP-01";

    @Mock
    private GestoPagoProductosClient productosClient;

    @Mock
    private GestoPagoTokenService tokenService;

    @Mock
    private CatProductoRepository catProductoRepository;

    @Captor
    private ArgumentCaptor<List<CatProducto>> documentosCaptor;

    private final GestoPagoProductoMapper productoMapper = Mappers.getMapper(GestoPagoProductoMapper.class);

    private CatalogoProductoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = crearServicio(TOKEN_CONFIGURADO);
    }

    @Test
    void sincronizarCatalogo_respuestaExitosa_guardaEnMongo() {
        when(productosClient.getProductList("Bearer " + TOKEN_CONFIGURADO)).thenReturn(respuestaConProductos());

        int total = service.sincronizarCatalogo();

        assertEquals(1, total);
        verify(catProductoRepository).reemplazarCatalogo(documentosCaptor.capture());
        CatProducto documento = documentosCaptor.getValue().get(0);
        assertEquals(185, documento.getIdProducto());
        assertEquals("Agua Cancun", documento.getNombre());
        assertEquals("AGUAKAN (Cancun)", documento.getServicio());
        assertEquals(new BigDecimal("10.0"), documento.getPrecio());
        assertEquals("Leyenda de prueba", documento.getLeyenda());
        assertNotNull(documento.getFechaActualizacion());
        verifyNoInteractions(tokenService);
    }

    @Test
    void sincronizarCatalogo_sinTokenConfigurado_usaTokenVigente() {
        service = crearServicio("");
        when(tokenService.obtenerTokenActivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.of(token(TOKEN_RENOVADO, true)));
        when(productosClient.getProductList("Bearer " + TOKEN_RENOVADO)).thenReturn(respuestaConProductos());

        assertEquals(1, service.sincronizarCatalogo());
        verify(tokenService, never()).renovarToken();
    }

    @Test
    void sincronizarCatalogo_sinTokenVigente_renuevaYReintenta() {
        service = crearServicio(null);
        when(tokenService.obtenerTokenActivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(token(TOKEN_RENOVADO, true)));
        when(productosClient.getProductList("Bearer " + TOKEN_RENOVADO)).thenReturn(respuestaConProductos());

        assertEquals(1, service.sincronizarCatalogo());
        verify(tokenService).renovarToken();
    }

    @Test
    void sincronizarCatalogo_sinTokenDisponible_lanzaErrorAutenticacion() {
        service = crearServicio(null);
        when(tokenService.obtenerTokenActivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO)).thenReturn(Optional.empty());

        assertThrows(GestoPagoAuthenticationException.class, () -> service.sincronizarCatalogo());
        verify(productosClient, never()).getProductList(anyString());
        verifyNoInteractions(catProductoRepository);
    }

    @Test
    void sincronizarCatalogo_tokenInactivo_lanzaErrorAutenticacion() {
        service = crearServicio(" ");
        when(tokenService.obtenerTokenActivo(ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO))
                .thenReturn(Optional.of(token(TOKEN_RENOVADO, false)));

        assertThrows(GestoPagoAuthenticationException.class, () -> service.sincronizarCatalogo());
        verify(productosClient, never()).getProductList(anyString());
    }

    @Test
    void sincronizarCatalogo_credencialesRechazadas_propagaErrorAutenticacion() {
        when(productosClient.getProductList(anyString()))
                .thenThrow(new GestoPagoAuthenticationException("GestoPago rechazo las credenciales de acceso"));

        assertThrows(GestoPagoAuthenticationException.class, () -> service.sincronizarCatalogo());
        verifyNoInteractions(catProductoRepository);
    }

    @Test
    void sincronizarCatalogo_estatusNoExitoso_propagaErrorRespuesta() {
        when(productosClient.getProductList(anyString()))
                .thenThrow(new GestoPagoResponseException("GestoPago respondio con estatus no exitoso", 500));

        GestoPagoResponseException ex = assertThrows(GestoPagoResponseException.class, () -> service.sincronizarCatalogo());
        assertEquals(500, ex.getStatusExterno());
        verifyNoInteractions(catProductoRepository);
    }

    @Test
    void sincronizarCatalogo_codigoDeNegocioNoExitoso_noModificaCatalogo() {
        when(productosClient.getProductList(anyString())).thenReturn(respuesta("05", null));

        GestoPagoResponseException ex = assertThrows(GestoPagoResponseException.class, () -> service.sincronizarCatalogo());
        assertTrue(ex.getMessage().contains("codigo=05"));
        verifyNoInteractions(catProductoRepository);
    }

    @Test
    void sincronizarCatalogo_catalogoVacio_noBorraCatalogoVigente() {
        when(productosClient.getProductList(anyString())).thenReturn(respuesta(GestoPagoMensaje.CODIGO_EXITO, List.of()));

        assertThrows(GestoPagoResponseException.class, () -> service.sincronizarCatalogo());
        verifyNoInteractions(catProductoRepository);
    }

    @Test
    void sincronizarCatalogo_respuestaSinMensaje_lanzaErrorRespuesta() {
        when(productosClient.getProductList(anyString())).thenReturn(new GestoPagoProductListResponse());

        assertThrows(GestoPagoResponseException.class, () -> service.sincronizarCatalogo());
    }

    @Test
    void sincronizarCatalogo_respuestaNula_lanzaErrorRespuesta() {
        when(productosClient.getProductList(anyString())).thenReturn(null);

        assertThrows(GestoPagoResponseException.class, () -> service.sincronizarCatalogo());
    }

    @Test
    void sincronizarCatalogo_xmlIlegible_lanzaErrorRespuesta() {
        when(productosClient.getProductList(anyString()))
                .thenThrow(new DecodeException(200, "XML invalido", request()));

        GestoPagoResponseException ex = assertThrows(GestoPagoResponseException.class, () -> service.sincronizarCatalogo());
        assertEquals(200, ex.getStatusExterno());
    }

    @Test
    void sincronizarCatalogo_timeout_lanzaErrorTimeout() {
        when(productosClient.getProductList(anyString()))
                .thenThrow(errorDeRed(new SocketTimeoutException("Read timed out")));

        assertThrows(GestoPagoTimeoutException.class, () -> service.sincronizarCatalogo());
    }

    @Test
    void sincronizarCatalogo_errorDeConexion_lanzaErrorComunicacion() {
        when(productosClient.getProductList(anyString()))
                .thenThrow(errorDeRed(new ConnectException("Connection refused")));

        assertThrows(GestoPagoCommunicationException.class, () -> service.sincronizarCatalogo());
    }

    @Test
    void sincronizarCatalogo_errorAlGuardarEnMongo_lanzaErrorCatalogo() {
        when(productosClient.getProductList(anyString())).thenReturn(respuestaConProductos());
        doThrow(new DataAccessResourceFailureException("Mongo caido")).when(catProductoRepository).reemplazarCatalogo(anyList());

        assertThrows(CatalogoProductosException.class, () -> service.sincronizarCatalogo());
    }

    private CatalogoProductoServiceImpl crearServicio(String tokenConfigurado) {
        return new CatalogoProductoServiceImpl(productosClient, tokenService, catProductoRepository, productoMapper,
                tokenConfigurado, ID_DISTRIBUIDOR, CODIGO_DISPOSITIVO);
    }

    private static GestoPagoProductListResponse respuestaConProductos() {
        GestoPagoProducto producto = new GestoPagoProducto();
        producto.setIdProducto(185);
        producto.setIdServicio(56);
        producto.setIdCatTipoServicio(15);
        producto.setProducto("Agua Cancun");
        producto.setServicio("AGUAKAN (Cancun)");
        producto.setPrecio(new BigDecimal("10.0"));
        producto.setTipoFront(2);
        producto.setHasDigitoVerificador(false);
        producto.setShowAyuda(false);
        producto.setTipoReferencia("c");
        producto.setLegend("Leyenda de prueba");
        return respuesta(GestoPagoMensaje.CODIGO_EXITO, List.of(producto));
    }

    private static GestoPagoProductListResponse respuesta(String codigo, List<GestoPagoProducto> productos) {
        GestoPagoMensaje mensaje = new GestoPagoMensaje();
        mensaje.setCodigo(codigo);
        mensaje.setTexto("Texto de prueba");

        GestoPagoProductListResponse response = new GestoPagoProductListResponse();
        response.setMensaje(mensaje);
        response.setProductos(productos);
        return response;
    }

    private static GestoPagoToken token(String valor, boolean activo) {
        GestoPagoToken token = new GestoPagoToken();
        token.setToken(valor);
        token.setActivo(activo);
        return token;
    }

    private static RetryableException errorDeRed(Throwable causa) {
        return new RetryableException(-1, causa.getMessage(), Request.HttpMethod.GET, causa, (Long) null, request());
    }

    private static Request request() {
        return Request.create(Request.HttpMethod.GET, "/sistema/service/getProductList.do",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
    }
}
