package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Sesion;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.enums.MotivoCierreSesion;
import com.proyecto.servicios.enums.Plataforma;
import com.proyecto.servicios.exception.SesionActivaException;
import com.proyecto.servicios.repositorys.onboarding.SesionRepository;
import com.proyecto.servicios.service.SesionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
public class SesionServiceImpl implements SesionService {

    private final SesionRepository sesionRepository;
    private final Duration vigencia;
    private final Clock reloj;

    public SesionServiceImpl(SesionRepository sesionRepository,
                             @Value("${onboarding.jwt.expiracion-minutos}") long vigenciaMinutos,
                             Clock reloj) {
        this.sesionRepository = sesionRepository;
        this.vigencia = Duration.ofMinutes(vigenciaMinutos);
        this.reloj = reloj;
    }

    @Override
    @Transactional(readOnly = true)
    public void validarDisponible(Integer usuarioId, String dispositivo, boolean cerrarSesionAnterior) {
        if (cerrarSesionAnterior) {
            return;
        }
        sesionRepository.buscarAbiertas(usuarioId, ahora()).stream()
                .filter(sesion -> !esMismoDispositivo(sesion, dispositivo))
                .findFirst()
                .ifPresent(sesion -> {
                    throw new SesionActivaException(sesion.getPlataforma(), sesion.getFechaCreacion());
                });
    }

    @Override
    @Transactional
    public Sesion abrir(Usuario usuario, Plataforma plataforma, String dispositivo, boolean cerrarSesionAnterior) {
        OffsetDateTime ahora = ahora();
        List<Sesion> abiertas = sesionRepository.bloquearAbiertas(usuario.getId(), ahora);
        for (Sesion abierta : abiertas) {
            // Otro dispositivo pudo iniciar sesion mientras se verificaba el codigo
            if (!cerrarSesionAnterior && !esMismoDispositivo(abierta, dispositivo)) {
                throw new SesionActivaException(abierta.getPlataforma(), abierta.getFechaCreacion());
            }
            abierta.cerrar(MotivoCierreSesion.REEMPLAZADA, ahora);
            log.info("Sesion reemplazada por un nuevo inicio de sesion: usuario id={}", usuario.getId());
        }

        Sesion sesion = new Sesion();
        sesion.setId(UUID.randomUUID());
        sesion.setUsuarioId(usuario.getId());
        sesion.setPlataforma(plataforma);
        sesion.setDispositivo(dispositivo);
        sesion.setFechaCreacion(ahora);
        sesion.setFechaExpiracion(ahora.plus(vigencia));
        return sesionRepository.save(sesion);
    }

    @Override
    @Transactional(readOnly = true)
    public EstadoSesion estado(UUID sesionId, Integer usuarioId) {
        return sesionRepository.findById(sesionId)
                .filter(sesion -> sesion.getUsuarioId().equals(usuarioId))
                .map(sesion -> {
                    if (sesion.estaAbierta(ahora())) {
                        return EstadoSesion.ABIERTA;
                    }
                    return sesion.getMotivoCierre() == MotivoCierreSesion.REEMPLAZADA
                            ? EstadoSesion.REEMPLAZADA
                            : EstadoSesion.INVALIDA;
                })
                .orElse(EstadoSesion.INVALIDA);
    }

    @Override
    @Transactional
    public void cerrar(UUID sesionId, Integer usuarioId) {
        sesionRepository.findById(sesionId)
                .filter(sesion -> sesion.getUsuarioId().equals(usuarioId) && sesion.getFechaCierre() == null)
                .ifPresent(sesion -> sesion.cerrar(MotivoCierreSesion.LOGOUT, ahora()));
    }

    /** Un mismo dispositivo puede volver a iniciar sesion (por ejemplo, tras reinstalar la app). */
    private static boolean esMismoDispositivo(Sesion sesion, String dispositivo) {
        return dispositivo != null && Objects.equals(sesion.getDispositivo(), dispositivo);
    }

    private OffsetDateTime ahora() {
        return OffsetDateTime.now(reloj);
    }
}
