package com.proyecto.servicios.security;

import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.enums.Rol;
import com.proyecto.servicios.exception.ContrasenaInvalidaException;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.validation.PoliticaContrasena;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InicializadorEjecutivoTest {

    private static final String PASSWORD = "Ejecutivo#2026";

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    @Test
    void sinEjecutivo_loCreaActivoConPasswordCifrado() {
        when(usuarioRepository.existsByCorreo("ejecutivo@banco.com")).thenReturn(false);

        inicializador(" Ejecutivo@Banco.com ", PASSWORD).run(null);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        Usuario ejecutivo = captor.getValue();
        assertEquals("ejecutivo@banco.com", ejecutivo.getCorreo());
        assertEquals(Rol.EJECUTIVO, ejecutivo.getRol());
        assertNull(ejecutivo.getCliente());
        assertTrue(ejecutivo.getActivo());
        assertTrue(passwordEncoder.matches(PASSWORD, ejecutivo.getPassword()));
    }

    @Test
    void ejecutivoExistente_noSeModifica() {
        when(usuarioRepository.existsByCorreo("ejecutivo@banco.com")).thenReturn(true);

        inicializador("ejecutivo@banco.com", PASSWORD).run(null);

        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void contrasenaDebil_impideCrearlo() {
        when(usuarioRepository.existsByCorreo("ejecutivo@banco.com")).thenReturn(false);
        InicializadorEjecutivo inicializador = inicializador("ejecutivo@banco.com", "debil");

        assertThrows(ContrasenaInvalidaException.class, () -> inicializador.run(null));
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    private InicializadorEjecutivo inicializador(String correo, String password) {
        return new InicializadorEjecutivo(usuarioRepository, passwordEncoder, new PoliticaContrasena(), correo, password);
    }
}
