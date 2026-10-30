package com.proyecto.servicios.service.gestopago;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GestoPagoTransaccionXmlParserTest {
    private final GestoPagoTransaccionXmlParser parser = new GestoPagoTransaccionXmlParser();

    @Test
    void leeAprobacionXmlYCamposDeEntrega() {
        var response = parser.parse("""
                <?xml version='1.0' encoding='UTF-8'?>
                <RESPONSE><ID_TX>42</ID_TX><NUM_AUTORIZACION>AUTH-1</NUM_AUTORIZACION>
                  <MONTO>100.00</MONTO><PIN>ABCD</PIN><FOLIO>F-1</FOLIO><legend><![CDATA[Usa el código]]></legend>
                  <MENSAJE><CODIGO>01</CODIGO><TEXTO>OK</TEXTO></MENSAJE>
                </RESPONSE>
                """);

        assertEquals("01", response.getMensaje().getCodigo());
        assertEquals("42", response.getIdTx());
        assertEquals("AUTH-1", response.getNumeroAutorizacion());
        assertEquals("ABCD", response.getPin());
        assertEquals("F-1", response.getFolio());
        assertEquals("Usa el código", response.getLeyenda());
    }

    @Test
    void rechazaXmlConEntidadesExternas() {
        String xml = "<!DOCTYPE foo [<!ENTITY xxe SYSTEM 'file:///windows/win.ini'>]>"
                + "<RESPONSE><MENSAJE><CODIGO>01</CODIGO><TEXTO>&xxe;</TEXTO></MENSAJE></RESPONSE>";
        assertThrows(IllegalArgumentException.class, () -> parser.parse(xml));
    }
}
