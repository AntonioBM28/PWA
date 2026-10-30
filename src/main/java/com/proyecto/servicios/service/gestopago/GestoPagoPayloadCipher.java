package com.proyecto.servicios.service.gestopago;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.exception.GestoPagoIntegrationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.security.SecureRandom;
import java.util.Arrays;

/** Cifrado del payload según el contrato de GestoPago: AES-256-CBC con padding PKCS5/PKCS7. */
@Component
public class GestoPagoPayloadCipher {
    private final ObjectMapper objectMapper;
    private final String keyBase64;
    private final String ivBase64;
    private final String replayKeyBase64;
    private volatile Keys keys;
    private static final SecureRandom RANDOM = new SecureRandom();

    public GestoPagoPayloadCipher(ObjectMapper objectMapper,
                                  @Value("${gestopago.payload.secret-key:}") String keyBase64,
                                  @Value("${gestopago.payload.iv:}") String ivBase64,
                                  @Value("${gestopago.payload.replay-key:}") String replayKeyBase64) {
        this.objectMapper = objectMapper;
        this.keyBase64 = keyBase64;
        this.ivBase64 = ivBase64;
        this.replayKeyBase64 = replayKeyBase64;
    }

    public String cifrar(Map<String, Object> payload) {
        Keys configured = keys();
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(configured.key(), "AES"), new IvParameterSpec(configured.iv()));
            byte[] encrypted = cipher.doFinal(objectMapper.writeValueAsBytes(payload));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception ex) {
            // No se propaga el payload ni datos del cliente en la excepción.
            throw new IllegalStateException("No fue posible proteger la solicitud para GestoPago");
        }
    }

    /** Cifra el payload ya protegido para GestoPago con AES-GCM al guardarlo para conciliación. */
    public String protegerParaAlmacenamiento(String providerCiphertext) {
        Keys configured = keys();
        try {
            byte[] nonce = new byte[12];
            RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(configured.replayKey(), "AES"), new GCMParameterSpec(128, nonce));
            byte[] encrypted = cipher.doFinal(providerCiphertext.getBytes(StandardCharsets.UTF_8));
            byte[] envelope = new byte[nonce.length + encrypted.length];
            System.arraycopy(nonce, 0, envelope, 0, nonce.length);
            System.arraycopy(encrypted, 0, envelope, nonce.length, encrypted.length);
            return Base64.getEncoder().encodeToString(envelope);
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible proteger la solicitud para conciliación");
        }
    }

    public String recuperarParaConfirmacion(String storedCiphertext) {
        Keys configured = keys();
        try {
            byte[] envelope = Base64.getDecoder().decode(storedCiphertext);
            if (envelope.length < 29) throw new IllegalArgumentException();
            byte[] nonce = Arrays.copyOfRange(envelope, 0, 12);
            byte[] encrypted = Arrays.copyOfRange(envelope, 12, envelope.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(configured.replayKey(), "AES"), new GCMParameterSpec(128, nonce));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible recuperar la solicitud para conciliación");
        }
    }

    private Keys keys() {
        Keys current = keys;
        if (current != null) return current;
        synchronized (this) {
            if (keys != null) return keys;
            try {
                byte[] key = Base64.getDecoder().decode(keyBase64);
                byte[] iv = Base64.getDecoder().decode(ivBase64);
                byte[] replayKey = Base64.getDecoder().decode(replayKeyBase64);
                if (key.length != 32 || iv.length != 16 || replayKey.length != 32) {
                    throw new IllegalArgumentException();
                }
                keys = new Keys(key, iv, replayKey);
                return keys;
            } catch (IllegalArgumentException ex) {
                throw new GestoPagoIntegrationException(
                        "La configuración de cifrado de GestoPago no está disponible o no es válida",
                        HttpStatus.SERVICE_UNAVAILABLE);
            }
        }
    }

    private record Keys(byte[] key, byte[] iv, byte[] replayKey) { }
}
