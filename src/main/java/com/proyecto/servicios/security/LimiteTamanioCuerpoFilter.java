package com.proyecto.servicios.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.exception.CuerpoDemasiadoGrandeException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Limita el tamano del cuerpo de las peticiones para que nadie pueda consumir la memoria del servidor
 * enviando JSON gigantes (los requests reales pesan alrededor de 1 KB).
 * <ul>
 *   <li>Si la peticion declara Content-Length mayor al maximo, se responde 413 sin leer el cuerpo.</li>
 *   <li>Si no lo declara (envio por partes), se cuenta lo leido y se corta al superar el maximo.</li>
 * </ul>
 * Se ejecuta antes que cualquier otro filtro, incluida la seguridad.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class LimiteTamanioCuerpoFilter extends OncePerRequestFilter {

    private final long maximoBytes;
    private final ObjectMapper objectMapper;

    public LimiteTamanioCuerpoFilter(@Value("${onboarding.limite.cuerpo-max-kb}") long maximoKb,
                                     ObjectMapper objectMapper) {
        this.maximoBytes = maximoKb * 1024;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > maximoBytes) {
            log.warn("Peticion rechazada por tamano: {} bytes en {}", request.getContentLengthLong(), request.getServletPath());
            RespuestaJson.escribirError(response, objectMapper, HttpStatus.PAYLOAD_TOO_LARGE,
                    "El cuerpo de la peticion excede el maximo de " + (maximoBytes / 1024) + " KB");
            return;
        }
        chain.doFilter(new CuerpoLimitado(request, maximoBytes), response);
    }

    /** Envuelve la peticion para que su cuerpo no pueda leerse mas alla del maximo. */
    static class CuerpoLimitado extends HttpServletRequestWrapper {

        private final long maximoBytes;
        private ServletInputStream stream;

        CuerpoLimitado(HttpServletRequest request, long maximoBytes) {
            super(request);
            this.maximoBytes = maximoBytes;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (stream == null) {
                stream = new StreamLimitado(super.getInputStream(), maximoBytes);
            }
            return stream;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            String encoding = getCharacterEncoding();
            Charset charset = encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
    }

    static class StreamLimitado extends ServletInputStream {

        private final ServletInputStream original;
        private final long maximoBytes;
        private long leidos;

        StreamLimitado(ServletInputStream original, long maximoBytes) {
            this.original = original;
            this.maximoBytes = maximoBytes;
        }

        @Override
        public int read() throws IOException {
            int valor = original.read();
            if (valor != -1) {
                contar(1);
            }
            return valor;
        }

        @Override
        public int read(byte[] buffer, int inicio, int longitud) throws IOException {
            int n = original.read(buffer, inicio, longitud);
            if (n > 0) {
                contar(n);
            }
            return n;
        }

        private void contar(int bytes) throws CuerpoDemasiadoGrandeException {
            leidos += bytes;
            if (leidos > maximoBytes) {
                throw new CuerpoDemasiadoGrandeException(maximoBytes);
            }
        }

        @Override
        public boolean isFinished() {
            return original.isFinished();
        }

        @Override
        public boolean isReady() {
            return original.isReady();
        }

        @Override
        public void setReadListener(ReadListener listener) {
            original.setReadListener(listener);
        }
    }
}
