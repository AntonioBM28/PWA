package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.exception.ClienteInactivoException;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.ContrasenaInvalidaException;
import com.proyecto.servicios.exception.UsuarioYaRegistradoException;
import com.proyecto.servicios.mapper.UsuarioMapper;
import com.proyecto.servicios.model.onboarding.AgregaUsuarioRequest;
import com.proyecto.servicios.model.onboarding.UsuarioDetalleResponse;
import com.proyecto.servicios.model.onboarding.UsuarioFiltro;
import com.proyecto.servicios.model.onboarding.UsuariosResponse;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.validation.PoliticaContrasena;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    private static final Integer ID_CLIENTE = 3;
    private static final String CORREO = "cliente.tres@mail.com";
    private static final String PASSWORD = "Segura#2026";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ClienteRepository clienteRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private UsuarioServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UsuarioServiceImpl(usuarioRepository, clienteRepository, Mappers.getMapper(UsuarioMapper.class),
                new PoliticaContrasena(), passwordEncoder);
    }

    @Test
    void consultarUsuarios_devuelvePaginaSinContrasenas() {
        UsuarioFiltro filtro = new UsuarioFiltro();
        filtro.setCorreo("Cliente");
        when(usuarioRepository.findAll(any(Specification.class), any(Pageable.class))).thenAnswer(inv ->
                new PageImpl<>(List.of(usuario(cliente(true))), inv.getArgument(1), 1));

        UsuariosResponse response = service.consultarUsuarios(filtro);

        assertEquals(1, response.getUsuarios().size());
        assertEquals(CORREO, response.getUsuarios().get(0).getCorreo());
        assertEquals(ID_CLIENTE, response.getUsuarios().get(0).getClienteId());
        assertEquals("cliente", filtro.getCorreo());
    }

    @Test
    void agregarUsuario_clienteSinUsuario_creaUsuarioActivoConPasswordCifrado() {
        when(clienteRepository.findById(ID_CLIENTE)).thenReturn(Optional.of(cliente(true)));
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioDetalleResponse response = service.agregarUsuario(request(PASSWORD));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(captor.capture());
        Usuario guardado = captor.getValue();
        assertEquals(CORREO, guardado.getCorreo());
        assertTrue(guardado.getActivo());
        assertTrue(passwordEncoder.matches(PASSWORD, guardado.getPassword()));
        assertEquals(UsuarioServiceImpl.MENSAJE_ALTA, response.getMensaje());
        assertEquals(CORREO, response.getUsuario().getCorreo());
    }

    @Test
    void agregarUsuario_clienteYaTieneUsuario() {
        when(clienteRepository.findById(ID_CLIENTE)).thenReturn(Optional.of(cliente(true)));
        when(usuarioRepository.existsByClienteId(ID_CLIENTE)).thenReturn(true);

        assertThrows(UsuarioYaRegistradoException.class, () -> service.agregarUsuario(request(PASSWORD)));
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void agregarUsuario_clienteInactivo() {
        when(clienteRepository.findById(ID_CLIENTE)).thenReturn(Optional.of(cliente(false)));

        assertThrows(ClienteInactivoException.class, () -> service.agregarUsuario(request(PASSWORD)));
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    void agregarUsuario_clienteNoExiste() {
        when(clienteRepository.findById(ID_CLIENTE)).thenReturn(Optional.empty());

        assertThrows(ClienteNoEncontradoException.class, () -> service.agregarUsuario(request(PASSWORD)));
    }

    @Test
    void agregarUsuario_contrasenaDebil_noConsultaNada() {
        assertThrows(ContrasenaInvalidaException.class, () -> service.agregarUsuario(request("debil")));
        verifyNoInteractions(clienteRepository, usuarioRepository);
    }

    private static AgregaUsuarioRequest request(String password) {
        AgregaUsuarioRequest request = new AgregaUsuarioRequest();
        request.setClienteId(ID_CLIENTE);
        request.setPassword(password);
        return request;
    }

    private static Cliente cliente(boolean activo) {
        Cliente cliente = new Cliente();
        cliente.setId(ID_CLIENTE);
        cliente.setCorreo(CORREO);
        cliente.setActivo(activo);
        return cliente;
    }

    private static Usuario usuario(Cliente cliente) {
        Usuario usuario = new Usuario();
        usuario.setId(30);
        usuario.setCliente(cliente);
        usuario.setCorreo(CORREO);
        usuario.setPassword("$2a$10$hash");
        usuario.setActivo(true);
        return usuario;
    }
}
