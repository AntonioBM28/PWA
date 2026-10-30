package com.proyecto.servicios.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;

/** Cliente sin reintentos automáticos: un POST financiero solo se manda una vez. */
@Component
public class GestoPagoTransaccionesClient {
    private final RestClient client;
    private final String apiKey;

    public GestoPagoTransaccionesClient(@Value("${gestopago.productos.url}") String baseUrl,
                                        @Value("${gestopago.api-key:}") String apiKey,
                                        @Value("${gestopago.transacciones.timeout-ms:70000}") long timeoutMs) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(5))
                .setResponseTimeout(Timeout.ofMilliseconds(timeoutMs))
                .build();
        var apacheClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .disableAutomaticRetries()
                .build();
        var requestFactory = new HttpComponentsClientHttpRequestFactory(apacheClient);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
        this.apiKey = apiKey;
    }

    public String enviar(String token, String random, String signed) {
        return post("/sistema/service/sendTx.do", token, random, signed);
    }

    public String confirmar(String token, String random, String signed) {
        return post("/sistema/service/confirmTx.do", token, random, signed);
    }

    private String post(String path, String token, String random, String signed) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("random", random);
        form.add("signed", signed);
        try {
            return client.post().uri(path)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_XML)
                    .headers(headers -> {
                        headers.setBearerAuth(token);
                        if (!apiKey.isBlank()) headers.set("X-API-Key", apiKey);
                    })
                    .body(form)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException ex) {
            throw new ProviderHttpException(ex.getStatusCode().value());
        } catch (ResourceAccessException ex) {
            // No incluir URL, body, cabeceras ni la excepción de red en mensajes o logs.
            throw new ProviderCommunicationException();
        }
    }

    public static class ProviderHttpException extends RuntimeException {
        private final int status;
        public ProviderHttpException(int status) { super("Respuesta HTTP de GestoPago"); this.status = status; }
        public int getStatus() { return status; }
    }

    public static class ProviderCommunicationException extends RuntimeException {
        public ProviderCommunicationException() { super("No se pudo determinar la respuesta de GestoPago"); }
    }
}
