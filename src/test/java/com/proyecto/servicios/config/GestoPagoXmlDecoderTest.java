package com.proyecto.servicios.config;

import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;
import com.proyecto.servicios.model.gestopago.GestoPagoProducto;
import feign.Request;
import feign.Response;
import feign.codec.DecodeException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GestoPagoXmlDecoderTest {

    private static final String XML_PRODUCTOS = """
            <?xml version='1.0' encoding='UTF-8'?>
            <RESPONSE>
                <MENSAJE>
                    <CODIGO>01</CODIGO>
                    <TEXTO>Operacion realizada con exito</TEXTO>
                </MENSAJE>
                <PRODUCTOS>
                    <producto servicio='AGUAKAN (Cancun)' producto='Agua Cancun (Mun. de Benito Juarez y de Isla Mujeres)' idServicio='56' idProducto='185' idCatTipoServicio='15' tipoFront='2' hasDigitoVerificador='false' precio='10.0' showAyuda='false' tipoReferencia='c'>
                        <legend>
                            <![CDATA[Para cualquier duda o aclaracion con tu pago, comunicate al telefono 073.]]>
                        </legend>
                    </producto>
                    <producto servicio='Amazon' producto='Amazon $100' idServicio='71' idProducto='200' idCatTipoServicio='10' tipoFront='1' hasDigitoVerificador='false' precio='100.0' showAyuda='false' tipoReferencia='a'>
                        <legend>
                            <![CDATA[Para usar tu tarjeta ingresa a www.amazon.com.mx/gc/redeem/]]>
                        </legend>
                    </producto>
                </PRODUCTOS>
            </RESPONSE>
            """;

    private final GestoPagoXmlDecoder decoder = new GestoPagoXmlDecoder();

    @Test
    void decode_respuestaDeProductos() throws IOException {
        GestoPagoProductListResponse response =
                (GestoPagoProductListResponse) decoder.decode(response(XML_PRODUCTOS), GestoPagoProductListResponse.class);

        assertEquals("01", response.getMensaje().getCodigo());
        assertEquals("Operacion realizada con exito", response.getMensaje().getTexto());
        assertTrue(response.getMensaje().isExitoso());
        assertEquals(2, response.getProductos().size());

        GestoPagoProducto producto = response.getProductos().get(0);
        assertEquals(185, producto.getIdProducto());
        assertEquals(56, producto.getIdServicio());
        assertEquals(15, producto.getIdCatTipoServicio());
        assertEquals("AGUAKAN (Cancun)", producto.getServicio());
        assertEquals("Agua Cancun (Mun. de Benito Juarez y de Isla Mujeres)", producto.getProducto());
        assertEquals(new BigDecimal("10.0"), producto.getPrecio());
        assertEquals(2, producto.getTipoFront());
        assertFalse(producto.getHasDigitoVerificador());
        assertFalse(producto.getShowAyuda());
        assertEquals("c", producto.getTipoReferencia());
        assertEquals("Para cualquier duda o aclaracion con tu pago, comunicate al telefono 073.", producto.getLegend());
    }

    @Test
    void decode_xmlMalformado_lanzaDecodeException() {
        assertThrows(DecodeException.class,
                () -> decoder.decode(response("<RESPONSE><MENSAJE>"), GestoPagoProductListResponse.class));
    }

    @Test
    void decode_xmlConDtd_esRechazado() {
        String xmlConEntidadExterna = """
                <?xml version="1.0"?>
                <!DOCTYPE RESPONSE [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <RESPONSE><MENSAJE><CODIGO>&xxe;</CODIGO></MENSAJE></RESPONSE>
                """;

        assertThrows(DecodeException.class,
                () -> decoder.decode(response(xmlConEntidadExterna), GestoPagoProductListResponse.class));
    }

    private static Response response(String body) {
        Request request = Request.create(Request.HttpMethod.GET, "/sistema/service/getProductList.do",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        return Response.builder()
                .status(200)
                .request(request)
                .headers(Collections.emptyMap())
                .body(body, StandardCharsets.UTF_8)
                .build();
    }
}
