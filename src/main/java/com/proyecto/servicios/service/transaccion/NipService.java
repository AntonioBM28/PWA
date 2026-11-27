package com.proyecto.servicios.service.transaccion;

import com.proyecto.servicios.entity.onboarding.NipTransaccion;
import com.proyecto.servicios.exception.NipException;
import com.proyecto.servicios.repositorys.onboarding.NipTransaccionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * NIP de 4 digitos con el que el CLIENTE autoriza sus compras. Se guarda solo el hash BCrypt;
 * despues de {@value #MAX_INTENTOS} intentos fallidos queda bloqueado {@link #BLOQUEO}.
 */
@Service
@Slf4j
public class NipService {

    static final int MAX_INTENTOS = 3;
    static final Duration BLOQUEO = Duration.ofMinutes(15);
    private static final String SECUENCIA = "0123456789";

    private final NipTransaccionRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final Clock reloj;

    public NipService(NipTransaccionRepository repository, PasswordEncoder passwordEncoder, Clock reloj) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.reloj = reloj;
    }

    /** Estado del NIP del usuario: si ya lo configuro y, si esta bloqueado, hasta cuando. */
    public record EstadoNip(boolean configurado, OffsetDateTime bloqueadoHasta) {
    }

    @Transactional(readOnly = true)
    public EstadoNip estado(Integer usuarioId) {
        return repository.findById(usuarioId)
                .map(n -> new EstadoNip(true, bloqueoVigente(n) ? n.getBloqueadoHasta() : null))
                .orElse(new EstadoNip(false, null));
    }

    @Transactional
    public void crear(Integer usuarioId, String nip, String confirmacion) {
        if (repository.existsById(usuarioId)) {
            throw new NipException("Ya tienes un NIP configurado; si quieres cambiarlo usa la opcion Cambiar NIP",
                    HttpStatus.CONFLICT);
        }
        validarNuevo(nip, confirmacion);
        OffsetDateTime ahora = ahora();
        NipTransaccion registro = new NipTransaccion();
        registro.setUsuarioId(usuarioId);
        registro.setNipHash(passwordEncoder.encode(nip));
        registro.setFechaCreacion(ahora);
        registro.setFechaActualizacion(ahora);
        repository.save(registro);
        log.info("NIP de transacciones configurado: usuario id={}", usuarioId);
    }

    @Transactional(noRollbackFor = NipException.class)
    public void cambiar(Integer usuarioId, String nipActual, String nipNuevo, String confirmacion) {
        NipTransaccion registro = verificarEnTransaccion(usuarioId, nipActual);
        validarNuevo(nipNuevo, confirmacion);
        if (passwordEncoder.matches(nipNuevo, registro.getNipHash())) {
            throw new NipException("El NIP nuevo debe ser distinto al actual", HttpStatus.BAD_REQUEST);
        }
        registro.setNipHash(passwordEncoder.encode(nipNuevo));
        registro.setFechaActualizacion(ahora());
        log.info("NIP de transacciones cambiado: usuario id={}", usuarioId);
    }

    /**
     * Valida el NIP. noRollbackFor: el intento fallido debe guardarse aunque se lance la excepcion,
     * si no el limite de intentos no serviria.
     */
    @Transactional(noRollbackFor = NipException.class)
    public void verificar(Integer usuarioId, String nip) {
        verificarEnTransaccion(usuarioId, nip);
    }

    private NipTransaccion verificarEnTransaccion(Integer usuarioId, String nip) {
        NipTransaccion registro = repository.bloquear(usuarioId)
                .orElseThrow(() -> new NipException("Primero configura tu NIP de transacciones",
                        HttpStatus.CONFLICT));
        if (bloqueoVigente(registro)) {
            throw new NipException(mensajeBloqueo(registro), HttpStatus.TOO_MANY_REQUESTS);
        }
        if (nip == null || !passwordEncoder.matches(nip, registro.getNipHash())) {
            int intentos = registro.getIntentosFallidos() + 1;
            log.warn("NIP incorrecto: usuario id={}, intento={}", usuarioId, intentos);
            if (intentos >= MAX_INTENTOS) {
                registro.setIntentosFallidos((short) 0);
                registro.setBloqueadoHasta(ahora().plus(BLOQUEO));
                throw new NipException("NIP incorrecto. Por seguridad tu NIP quedo bloqueado "
                        + BLOQUEO.toMinutes() + " minutos", HttpStatus.TOO_MANY_REQUESTS);
            }
            registro.setIntentosFallidos((short) intentos);
            int restantes = MAX_INTENTOS - intentos;
            throw new NipException("NIP incorrecto. Te queda" + (restantes == 1 ? " 1 intento" : "n " + restantes
                    + " intentos"), HttpStatus.BAD_REQUEST);
        }
        registro.setIntentosFallidos((short) 0);
        registro.setBloqueadoHasta(null);
        return registro;
    }

    /** 4 digitos, sin repetir el mismo digito ni usar secuencias (1234, 9876). */
    static void validarNuevo(String nip, String confirmacion) {
        if (nip == null || !nip.matches("^[0-9]{4}$")) {
            throw new NipException("El NIP debe tener exactamente 4 digitos", HttpStatus.BAD_REQUEST);
        }
        if (!nip.equals(confirmacion)) {
            throw new NipException("La confirmacion no coincide con el NIP", HttpStatus.BAD_REQUEST);
        }
        if (nip.chars().distinct().count() == 1
                || SECUENCIA.contains(nip)
                || new StringBuilder(SECUENCIA).reverse().toString().contains(nip)) {
            throw new NipException("El NIP es muy facil de adivinar; no uses digitos repetidos ni consecutivos",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private boolean bloqueoVigente(NipTransaccion registro) {
        return registro.getBloqueadoHasta() != null && registro.getBloqueadoHasta().isAfter(ahora());
    }

    private String mensajeBloqueo(NipTransaccion registro) {
        long minutos = Math.max(1, Duration.between(ahora(), registro.getBloqueadoHasta()).toMinutes() + 1);
        return "Tu NIP esta bloqueado por intentos fallidos. Intenta de nuevo en " + minutos + " minuto"
                + (minutos == 1 ? "" : "s");
    }

    private OffsetDateTime ahora() {
        return OffsetDateTime.now(reloj);
    }
}
