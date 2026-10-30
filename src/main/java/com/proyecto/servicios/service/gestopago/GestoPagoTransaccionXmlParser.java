package com.proyecto.servicios.service.gestopago;

import com.proyecto.servicios.config.GestoPagoXmlDecoder;
import com.proyecto.servicios.model.gestopago.GestoPagoTransaccionResponse;
import org.springframework.stereotype.Component;

@Component
public class GestoPagoTransaccionXmlParser {
    private final GestoPagoXmlDecoder decoder = new GestoPagoXmlDecoder();

    public GestoPagoTransaccionResponse parse(String xml) {
        return decoder.decode(xml, GestoPagoTransaccionResponse.class);
    }
}
