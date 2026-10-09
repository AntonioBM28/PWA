package com.proyecto.servicios.security;

import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.enums.Rol;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.validation.PoliticaContrasena;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crea al arrancar el usuario EJECUTIVO inicial (onboarding.ejecutivo.correo / password) si todavia
 * no existe. Asi la contrasena no queda en una migracion y se guarda cifrada con BCrypt. Si el
 * usuario ya existe no se modifica, aunque cambie la propiedad.
 */
@Component
@Slf4j
public class InicializadorEjecutivo implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final PoliticaContrasena politicaContrasena;
    private final String correo;
    private final String password;

    public InicializadorEjecutivo(UsuarioRepository usuarioRepository,
                                  PasswordEncoder passwordEncoder,
                                  PoliticaContrasena politicaContrasena,
                                  @Value("${onboarding.ejecutivo.correo}") String correo,
                                  @Value("${onboarding.ejecutivo.password}") String password) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.politicaContrasena = politicaContrasena;
        this.correo = StringUtils.lowerCase(StringUtils.trim(correo));
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.existsByCorreo(correo)) {
            return;
        }
        politicaContrasena.validar(password);
        Usuario ejecutivo = new Usuario();
        ejecutivo.setCorreo(correo);
        ejecutivo.setPassword(passwordEncoder.encode(password));
        ejecutivo.setRol(Rol.EJECUTIVO);
        ejecutivo.setActivo(true);
        usuarioRepository.saveAndFlush(ejecutivo);
        log.info("Usuario EJECUTIVO inicial creado: id={}", ejecutivo.getId());
    }
}
