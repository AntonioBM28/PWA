package com.proyecto.servicios.service.transaccion;

import com.proyecto.servicios.entity.onboarding.AutorizacionTransaccion;
import com.proyecto.servicios.exception.NipException;
import com.proyecto.servicios.model.transaccion.AutorizacionTransaccionRequest;
import com.proyecto.servicios.model.transaccion.AutorizacionTransaccionResponse;
import com.proyecto.servicios.model.transaccion.CrearTransaccionRequest;
import com.proyecto.servicios.repositorys.onboarding.AutorizacionTransaccionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Token dinamico de las compras, como el token movil integrado de las apps bancarias: despues de
 * validar el NIP se emite un codigo de 8 digitos que la app usa automaticamente (nunca se muestra
 * al cliente). Vence en segundos, sirve una sola vez y solo para la operacion que se autorizo
 * (misma cuenta, producto, destino, referencia y monto). Solo se guarda su hash.
 */
@Service
@Slf4j
public class AutorizacionTransaccionService {

    static final int MAX_INTENTOS = 3;
    static final String CODIGO_EXITO_MENSAJE = "Operacion autorizada";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AutorizacionTransaccionRepository repository;
    private final NipService nipService;
    private final TransaccionService transaccionService;
    private final PasswordEncoder passwordEncoder;
    private final Clock reloj;
    private final Duration vigencia;

    public AutorizacionTransaccionService(AutorizacionTransaccionRepository repository,
                                          NipService nipService,
                                          TransaccionService transaccionService,
                                          PasswordEncoder passwordEncoder,
                                          Clock reloj,
                                          @Value("${onboarding.token-dinamico.vigencia:60s}") Duration vigencia) {
        this.repository = repository;
        this.nipService = nipService;
        this.transaccionService = transaccionService;
        this.passwordEncoder = passwordEncoder;
        this.reloj = reloj;
        this.vigencia = vigencia;
    }

    /**
     * Valida la compra y el NIP, y emite el token dinamico para esa operacion. La app lo guarda en
     * memoria y lo envia al confirmar; no lo muestra.
     */
    @Transactional(noRollbackFor = NipException.class)
    public AutorizacionTransaccionResponse autorizar(AutorizacionTransaccionRequest request, JwtAuthenticationToken auth) {
        Integer usuarioId = usuarioId(auth);
        // Primero los datos de la compra: no se gasta un intento de NIP en una operacion invalida
        transaccionService.validarCompra(request);
        nipService.verificar(usuarioId, request.getNip());

        String token = String.format("%08d", RANDOM.nextInt(100_000_000));
        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        AutorizacionTransaccion autorizacion = new AutorizacionTransaccion();
        autorizacion.setId(UUID.randomUUID());
        autorizacion.setUsuarioId(usuarioId);
        autorizacion.setHuellaSolicitud(transaccionService.huella(request.getNumeroCuenta(), request));
        autorizacion.setTokenHash(passwordEncoder.encode(token));
        autorizacion.setFechaCreacion(ahora);
        autorizacion.setFechaExpiracion(ahora.plus(vigencia));
        repository.save(autorizacion);
        log.info("Token dinamico emitido: usuario id={}", usuarioId);

        AutorizacionTransaccionResponse response = new AutorizacionTransaccionResponse();
        response.setCodigo(0);
        response.setMensaje(CODIGO_EXITO_MENSAJE);
        response.setAutorizacionId(autorizacion.getId());
        response.setTokenDinamico(token);
        response.setVigenciaSegundos(vigencia.toSeconds());
        response.setExpiraEn(autorizacion.getFechaExpiracion());
        return response;
    }

    /**
     * Valida y consume el token antes de ejecutar la compra. Reintentar la misma compra con la
     * misma Idempotency-Key no vuelve a pedir token (devuelve el resultado original).
     */
    @Transactional(noRollbackFor = NipException.class)
    public void consumir(CrearTransaccionRequest request, String idempotencyKey, JwtAuthenticationToken auth) {
        Integer usuarioId = usuarioId(auth);
        AutorizacionTransaccion autorizacion = repository.bloquear(request.getAutorizacionId())
                .filter(a -> a.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new NipException("La autorizacion no es valida; confirma de nuevo con tu NIP",
                        HttpStatus.GONE));
        if (autorizacion.getFechaUso() != null) {
            if (idempotencyKey != null && idempotencyKey.equals(autorizacion.getClaveIdempotencia())) {
                return;
            }
            throw new NipException("Este token ya se utilizo; confirma de nuevo con tu NIP", HttpStatus.GONE);
        }
        if (autorizacion.getIntentosFallidos() >= MAX_INTENTOS) {
            throw new NipException("Superaste los intentos del token; confirma de nuevo con tu NIP", HttpStatus.GONE);
        }
        if (!autorizacion.getFechaExpiracion().isAfter(OffsetDateTime.now(reloj))) {
            throw new NipException("El token dinamico expiro; confirma de nuevo con tu NIP", HttpStatus.GONE);
        }
        if (!autorizacion.getHuellaSolicitud().equals(transaccionService.huella(request.getNumeroCuenta(), request))) {
            throw new NipException("Los datos de la operacion cambiaron; confirma de nuevo con tu NIP",
                    HttpStatus.CONFLICT);
        }
        if (!passwordEncoder.matches(request.getTokenDinamico(), autorizacion.getTokenHash())) {
            autorizacion.setIntentosFallidos((short) (autorizacion.getIntentosFallidos() + 1));
            throw new NipException("El token dinamico es incorrecto", HttpStatus.BAD_REQUEST);
        }
        autorizacion.setFechaUso(OffsetDateTime.now(reloj));
        autorizacion.setClaveIdempotencia(idempotencyKey);
    }


    static Integer usuarioId(JwtAuthenticationToken auth) {
        try {
            return Integer.valueOf(auth.getToken().getSubject());
        } catch (NumberFormatException | NullPointerException e) {
            throw new NipException("Sesion invalida", HttpStatus.UNAUTHORIZED);
        }
    }
}
