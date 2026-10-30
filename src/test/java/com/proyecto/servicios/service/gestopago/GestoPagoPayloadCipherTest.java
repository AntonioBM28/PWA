package com.proyecto.servicios.service.gestopago;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GestoPagoPayloadCipherTest {
    private static String base64(int length, int value) {
        byte[] bytes = new byte[length];
        java.util.Arrays.fill(bytes, (byte) value);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @Test
    void cifraPayloadYLoProtegeConCifradoAutenticadoParaAlmacenamiento() {
        GestoPagoPayloadCipher cipher = new GestoPagoPayloadCipher(
                new ObjectMapper(), base64(32, 1), base64(16, 2), base64(32, 3));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("idServicio", 71);
        payload.put("telefono", "5512345678");

        String proveedor = cipher.cifrar(payload);
        String persistido = cipher.protegerParaAlmacenamiento(proveedor);

        assertNotEquals(proveedor, persistido);
        assertFalse(persistido.contains("5512345678"));
        assertEquals(proveedor, cipher.recuperarParaConfirmacion(persistido));
        assertThrows(IllegalStateException.class,
                () -> cipher.recuperarParaConfirmacion(persistido.substring(0, persistido.length() - 4) + "AAAA"));
    }

    @Test
    void rechazaClavesDeLongitudIncorrecta() {
        assertThrows(IllegalStateException.class, () -> new GestoPagoPayloadCipher(
                new ObjectMapper(), base64(16, 1), base64(16, 2), base64(32, 3)));
    }
}
