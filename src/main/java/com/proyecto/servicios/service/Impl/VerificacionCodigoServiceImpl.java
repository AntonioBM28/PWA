package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.config.ConfiguracionVerificacion;
import com.proyecto.servicios.entity.onboarding.CodigoVerificacion;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.enums.Plataforma;
import com.proyecto.servicios.exception.CodigoVerificacionException;
import com.proyecto.servicios.exception.DemasiadosIntentosException;
import com.proyecto.servicios.model.onboarding.DesafioVerificacionResponse;
import com.proyecto.servicios.repositorys.onboarding.CodigoVerificacionRepository;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.security.GeneradorCodigoVerificacion;
import com.proyecto.servicios.service.NotificadorCorreo;
import com.proyecto.servicios.service.VerificacionCodigoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Ciclo de vida del codigo de verificacion: se guarda solo su hash BCrypt, vence en pocos minutos,
 * se puede usar una sola vez, admite pocos intentos y los reenvios tienen espera y limite.
 */
@Service
@Slf4j
public class VerificacionCodigoServiceImpl implements VerificacionCodigoService {

    static final int CODIGO_EXITO = 0;
    static final String MENSAJE_ENVIADO = "Te enviamos un codigo de verificacion a tu correo";
    static final String MENSAJE_NO_VALIDO = "El codigo ya no es valido; inicia sesion de nuevo";
    static final String MENSAJE_SIN_INTENTOS = "Superaste los intentos permitidos; inicia sesion de nuevo";
    /** Despues de este tiempo desde el login ya no se reenvia el codigo: hay que volver a empezar. */
    static final Duration VIDA_MAXIMA_DESAFIO = Duration.ofMinutes(30);

    private final CodigoVerificacionRepository codigoRepository;
    private final UsuarioRepository usuarioRepository;
    private final GeneradorCodigoVerificacion generador;
    private final NotificadorCorreo notificador;
    private final PasswordEncoder passwordEncoder;
    private final ConfiguracionVerificacion config;
    private final Clock reloj;

    public VerificacionCodigoServiceImpl(CodigoVerificacionRepository codigoRepository,
                                         UsuarioRepository usuarioRepository,
                                         GeneradorCodigoVerificacion generador,
                                         NotificadorCorreo notificador,
                                         PasswordEncoder passwordEncoder,
                                         ConfiguracionVerificacion config,
                                         Clock reloj) {
        this.codigoRepository = codigoRepository;
        this.usuarioRepository = usuarioRepository;
        this.generador = generador;
        this.notificador = notificador;
        this.passwordEncoder = passwordEncoder;
        this.config = config;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public DesafioVerificacionResponse crearYEnviar(Usuario usuario, Plataforma plataforma, String dispositivo,
                                                    boolean cerrarSesionAnterior) {
        OffsetDateTime ahora = ahora();
        CodigoVerificacion desafio = new CodigoVerificacion();
        desafio.setId(UUID.randomUUID());
        desafio.setUsuarioId(usuario.getId());
        desafio.setPlataforma(plataforma);
        desafio.setDispositivo(dispositivo);
        desafio.setCerrarSesionAnterior(cerrarSesionAnterior);
        desafio.setFechaCreacion(ahora);

        String codigo = asignarCodigoNuevo(desafio, ahora);
        codigoRepository.save(desafio);
        // Si el correo falla, la transaccion se revierte y el desafio no queda guardado
        notificador.enviarCodigoVerificacion(usuario.getCorreo(), codigo, config.vigencia());
        log.info("Codigo de verificacion enviado: usuario id={}", usuario.getId());
        return respuesta(desafio, usuario.getCorreo(), ahora);
    }

    @Override
    @Transactional(noRollbackFor = CodigoVerificacionException.class)
    public CodigoVerificacion validar(UUID desafioId, String codigo) {
        OffsetDateTime ahora = ahora();
        CodigoVerificacion desafio = codigoRepository.bloquear(desafioId)
                .orElseThrow(() -> new CodigoVerificacionException(MENSAJE_NO_VALIDO, HttpStatus.GONE));

        if (desafio.getFechaUso() != null) {
            throw new CodigoVerificacionException("Este codigo ya se utilizo; inicia sesion de nuevo",
                    HttpStatus.GONE);
        }
        if (desafio.getIntentosFallidos() >= config.maxIntentos()) {
            throw new CodigoVerificacionException(MENSAJE_SIN_INTENTOS, HttpStatus.TOO_MANY_REQUESTS);
        }
        if (!desafio.getFechaExpiracion().isAfter(ahora)) {
            throw new CodigoVerificacionException("El codigo expiro; solicita uno nuevo", HttpStatus.BAD_REQUEST);
        }
        if (!passwordEncoder.matches(codigo, desafio.getCodigoHash())) {
            int intentos = desafio.getIntentosFallidos() + 1;
            desafio.setIntentosFallidos((short) intentos);
            log.warn("Codigo de verificacion incorrecto: usuario id={}, intento={}", desafio.getUsuarioId(), intentos);
            int restantes = config.maxIntentos() - intentos;
            throw restantes > 0
                    ? new CodigoVerificacionException("El codigo es incorrecto. Te quedan " + restantes
                    + (restantes == 1 ? " intento" : " intentos"), HttpStatus.BAD_REQUEST)
                    : new CodigoVerificacionException(MENSAJE_SIN_INTENTOS, HttpStatus.TOO_MANY_REQUESTS);
        }
        desafio.setFechaUso(ahora);
        return desafio;
    }

    @Override
    @Transactional
    public DesafioVerificacionResponse reenviar(UUID desafioId) {
        OffsetDateTime ahora = ahora();
        // Un codigo expirado se puede reemplazar ("Solicita uno nuevo"), pero el desafio completo
        // tiene una vida maxima para que no sirva indefinidamente
        CodigoVerificacion desafio = codigoRepository.bloquear(desafioId)
                .filter(d -> d.getFechaUso() == null
                        && d.getIntentosFallidos() < config.maxIntentos()
                        && d.getFechaCreacion().plus(VIDA_MAXIMA_DESAFIO).isAfter(ahora))
                .orElseThrow(() -> new CodigoVerificacionException(MENSAJE_NO_VALIDO, HttpStatus.GONE));

        long espera = segundosParaReenviar(desafio, ahora);
        if (espera > 0) {
            throw new DemasiadosIntentosException("Espera " + espera + " segundos para solicitar otro codigo", espera);
        }
        if (desafio.getReenvios() >= config.maxReenvios()) {
            throw new CodigoVerificacionException("Ya no es posible reenviar el codigo; inicia sesion de nuevo",
                    HttpStatus.TOO_MANY_REQUESTS);
        }

        String correo = usuarioRepository.findById(desafio.getUsuarioId())
                .orElseThrow(() -> new CodigoVerificacionException(MENSAJE_NO_VALIDO, HttpStatus.GONE))
                .getCorreo();
        // El codigo anterior deja de servir: el hash se reemplaza
        String codigo = asignarCodigoNuevo(desafio, ahora);
        desafio.setReenvios((short) (desafio.getReenvios() + 1));
        notificador.enviarCodigoVerificacion(correo, codigo, config.vigencia());
        log.info("Codigo de verificacion reenviado: usuario id={}", desafio.getUsuarioId());
        return respuesta(desafio, correo, ahora);
    }

    private String asignarCodigoNuevo(CodigoVerificacion desafio, OffsetDateTime ahora) {
        String codigo = generador.generar();
        desafio.setCodigoHash(passwordEncoder.encode(codigo));
        desafio.setFechaEnvio(ahora);
        desafio.setFechaExpiracion(ahora.plus(config.vigencia()));
        return codigo;
    }

    private long segundosParaReenviar(CodigoVerificacion desafio, OffsetDateTime ahora) {
        OffsetDateTime permitido = desafio.getFechaEnvio().plus(config.esperaReenvio());
        return Math.max(0, Duration.between(ahora, permitido).toSeconds());
    }

    private DesafioVerificacionResponse respuesta(CodigoVerificacion desafio, String correo, OffsetDateTime ahora) {
        DesafioVerificacionResponse response = new DesafioVerificacionResponse();
        response.setCodigo(CODIGO_EXITO);
        response.setMensaje(MENSAJE_ENVIADO);
        response.setDesafioId(desafio.getId());
        response.setCorreoEnmascarado(enmascarar(correo));
        response.setExpiraEnSegundos(Duration.between(ahora, desafio.getFechaExpiracion()).toSeconds());
        response.setReenviarEnSegundos(segundosParaReenviar(desafio, ahora));
        return response;
    }

    private OffsetDateTime ahora() {
        return OffsetDateTime.now(reloj);
    }

    /** maria.garcia@mail.com -> ma***@mail.com */
    static String enmascarar(String correo) {
        int arroba = correo.indexOf('@');
        if (arroba <= 0) {
            return "***";
        }
        String visible = correo.substring(0, Math.min(2, arroba));
        return visible + "***" + correo.substring(arroba);
    }
}
