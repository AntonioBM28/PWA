package com.proyecto.servicios.config;

import feign.Response;
import feign.codec.DecodeException;
import feign.codec.Decoder;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decodifica las respuestas XML de GestoPago con JAXB. Se deshabilitan DTD y entidades
 * externas para evitar ataques XXE.
 */
public class GestoPagoXmlDecoder implements Decoder {

    private final Map<Class<?>, JAXBContext> contextos = new ConcurrentHashMap<>();
    private final XMLInputFactory xmlInputFactory;

    public GestoPagoXmlDecoder() {
        xmlInputFactory = XMLInputFactory.newFactory();
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    }

    @Override
    public Object decode(Response response, Type type) throws IOException {
        if (response.body() == null) {
            return null;
        }
        if (!(type instanceof Class<?> clase)) {
            throw new DecodeException(response.status(), "Tipo no soportado para XML: " + type, response.request());
        }
        try (InputStream body = response.body().asInputStream()) {
            XMLStreamReader reader = xmlInputFactory.createXMLStreamReader(body);
            try {
                return obtenerContexto(clase).createUnmarshaller().unmarshal(reader, clase).getValue();
            } finally {
                reader.close();
            }
        } catch (JAXBException | XMLStreamException e) {
            throw new DecodeException(response.status(), "No fue posible leer el XML de GestoPago", response.request(), e);
        }
    }

    private JAXBContext obtenerContexto(Class<?> clase) throws JAXBException {
        JAXBContext contexto = contextos.get(clase);
        if (contexto == null) {
            contexto = JAXBContext.newInstance(clase);
            contextos.put(clase, contexto);
        }
        return contexto;
    }
}
