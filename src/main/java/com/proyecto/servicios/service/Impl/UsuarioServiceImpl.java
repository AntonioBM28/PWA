package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.enums.Rol;
import com.proyecto.servicios.exception.ClienteInactivoException;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CorreoDuplicadoException;
import com.proyecto.servicios.exception.UsuarioYaRegistradoException;
import com.proyecto.servicios.mapper.UsuarioMapper;
import com.proyecto.servicios.model.onboarding.AgregaUsuarioRequest;
import com.proyecto.servicios.model.onboarding.UsuarioDetalleResponse;
import com.proyecto.servicios.model.onboarding.UsuarioFiltro;
import com.proyecto.servicios.model.onboarding.UsuariosResponse;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.service.UsuarioService;
import com.proyecto.servicios.validation.PoliticaContrasena;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.proyecto.servicios.repositorys.onboarding.UsuarioSpecifications.activo;
import static com.proyecto.servicios.repositorys.onboarding.UsuarioSpecifications.correoEmpiezaCon;
import static com.proyecto.servicios.repositorys.onboarding.UsuarioSpecifications.deCliente;

/**
 * Consulta de usuarios y alta del usuario de un cliente que no tiene uno (el registro de clientes
 * ya crea el usuario automaticamente). Reglas: un usuario por cliente, correo unico (es el del
 * cliente), contrasena con la politica vigente y cifrada con BCrypt.
 */
@Service
@Slf4j
public class UsuarioServiceImpl implements UsuarioService {

    static final int CODIGO_EXITO = 0;
    static final String MENSAJE_EXITO = "Exito";
    static final String MENSAJE_ALTA = "Usuario creado correctamente";

    private static final Sort ORDEN_USUARIOS = Sort.by("correo");

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioMapper usuarioMapper;
    private final PoliticaContrasena politicaContrasena;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository,
                              ClienteRepository clienteRepository,
                              UsuarioMapper usuarioMapper,
                              PoliticaContrasena politicaContrasena,
                              PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioMapper = usuarioMapper;
        this.politicaContrasena = politicaContrasena;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UsuariosResponse consultarUsuarios(UsuarioFiltro filtro) {
        Specification<Usuario> criterios = Specification
                .where(correoEmpiezaCon(filtro.getCorreo()))
                .and(activo(filtro.getActivo()))
                .and(deCliente(filtro.getClienteId()));
        Page<Usuario> pagina = usuarioRepository.findAll(criterios,
                PageRequest.of(filtro.getPagina(), filtro.getTamanio(), ORDEN_USUARIOS));

        UsuariosResponse response = new UsuariosResponse();
        response.setCodigo(CODIGO_EXITO);
        response.setMensaje(MENSAJE_EXITO);
        response.setUsuarios(usuarioMapper.toResponseList(pagina.getContent()));
        response.asignarPaginacion(pagina);
        return response;
    }

    @Override
    @Transactional
    public UsuarioDetalleResponse agregarUsuario(AgregaUsuarioRequest request) {
        politicaContrasena.validar(request.getPassword());
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ClienteNoEncontradoException(request.getClienteId()));
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ClienteInactivoException("No se puede crear el usuario: el cliente " + cliente.getId()
                    + " esta dado de baja");
        }
        if (usuarioRepository.existsByClienteId(cliente.getId())) {
            throw new UsuarioYaRegistradoException(cliente.getId());
        }
        if (usuarioRepository.existsByCorreo(cliente.getCorreo())) {
            throw new CorreoDuplicadoException();
        }

        Usuario usuario = new Usuario();
        usuario.setCliente(cliente);
        usuario.setCorreo(cliente.getCorreo());
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setRol(Rol.CLIENTE);
        usuario.setActivo(true);
        Usuario guardado = usuarioRepository.saveAndFlush(usuario);
        log.info("Usuario creado para el cliente id={}", cliente.getId());

        UsuarioDetalleResponse response = new UsuarioDetalleResponse();
        response.setCodigo(CODIGO_EXITO);
        response.setMensaje(MENSAJE_ALTA);
        response.setUsuario(usuarioMapper.toResponse(guardado));
        return response;
    }
}
