package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Domicilio;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.enums.Rol;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CorreoDuplicadoException;
import com.proyecto.servicios.exception.CurpDuplicadaException;
import com.proyecto.servicios.exception.RfcDuplicadoException;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.mapper.CuentaMapper;
import com.proyecto.servicios.model.onboarding.ActualizaClienteRequest;
import com.proyecto.servicios.model.onboarding.ClienteDetalleResponse;
import com.proyecto.servicios.model.onboarding.ClienteFiltro;
import com.proyecto.servicios.model.onboarding.ClienteRequest;
import com.proyecto.servicios.model.onboarding.ClientesResponse;
import com.proyecto.servicios.model.onboarding.RegistroClienteResponse;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.service.ClienteService;
import com.proyecto.servicios.validation.PoliticaContrasena;
import com.proyecto.servicios.validation.ValidadorDomicilio;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.function.Supplier;

import static com.proyecto.servicios.repositorys.onboarding.ClienteSpecifications.activo;
import static com.proyecto.servicios.repositorys.onboarding.ClienteSpecifications.apellidoMaternoEmpiezaCon;
import static com.proyecto.servicios.repositorys.onboarding.ClienteSpecifications.apellidoPaternoEmpiezaCon;
import static com.proyecto.servicios.repositorys.onboarding.ClienteSpecifications.correoIgual;
import static com.proyecto.servicios.repositorys.onboarding.ClienteSpecifications.curpIgual;
import static com.proyecto.servicios.repositorys.onboarding.ClienteSpecifications.nombreEmpiezaCon;
import static com.proyecto.servicios.repositorys.onboarding.ClienteSpecifications.registradoAntesDe;
import static com.proyecto.servicios.repositorys.onboarding.ClienteSpecifications.registradoDesde;
import static com.proyecto.servicios.repositorys.onboarding.ClienteSpecifications.rfcIgual;
import static com.proyecto.servicios.repositorys.onboarding.ClienteSpecifications.tieneCuenta;

/**
 * Registro, consulta y actualizacion de clientes. La cuenta del registro la abre AperturaCuenta.
 * <p>
 * Las validaciones (incluida la consulta a la API de codigos postales) se hacen antes de abrir la
 * transaccion para no retener una conexion a la base de datos durante una llamada HTTP externa.
 */
@Service
@Slf4j
public class ClienteServiceImpl implements ClienteService {

    static final int CODIGO_EXITO = 0;
    static final String MENSAJE_EXITO = "Exito";
    static final String MENSAJE_REGISTRO = "Cliente registrado correctamente";
    static final String MENSAJE_ACTUALIZACION = "Cliente actualizado correctamente";
    static final String MENSAJE_BAJA = "Cliente dado de baja correctamente; sus cuentas y su usuario quedaron inactivos";
    static final String MENSAJE_REACTIVACION = "Cliente reactivado correctamente junto con su usuario; "
            + "sus cuentas deben reactivarse individualmente";

    private static final Sort ORDEN_CLIENTES = Sort.by("apellidoPaterno", "apellidoMaterno", "nombre", "id");

    /** Si dos operaciones simultaneas pasan la validacion, la restriccion UNIQUE decide cual falla. */
    private static final Map<String, Supplier<RuntimeException>> RESTRICCIONES_UNICAS = Map.of(
            "uq_clientes_curp", CurpDuplicadaException::new,
            "uq_clientes_rfc", RfcDuplicadoException::new,
            "uq_clientes_correo", CorreoDuplicadoException::new,
            "uq_usuarios_correo", CorreoDuplicadoException::new);

    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteMapper clienteMapper;
    private final CuentaMapper cuentaMapper;
    private final AperturaCuenta aperturaCuenta;
    private final ValidadorDomicilio validadorDomicilio;
    private final PoliticaContrasena politicaContrasena;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;
    private final ZoneId zonaHoraria;

    public ClienteServiceImpl(ClienteRepository clienteRepository,
                              CuentaRepository cuentaRepository,
                              UsuarioRepository usuarioRepository,
                              ClienteMapper clienteMapper,
                              CuentaMapper cuentaMapper,
                              AperturaCuenta aperturaCuenta,
                              ValidadorDomicilio validadorDomicilio,
                              PoliticaContrasena politicaContrasena,
                              PasswordEncoder passwordEncoder,
                              PlatformTransactionManager transactionManager,
                              @Value("${onboarding.zona-horaria}") ZoneId zonaHoraria) {
        this.clienteRepository = clienteRepository;
        this.cuentaRepository = cuentaRepository;
        this.usuarioRepository = usuarioRepository;
        this.clienteMapper = clienteMapper;
        this.cuentaMapper = cuentaMapper;
        this.aperturaCuenta = aperturaCuenta;
        this.validadorDomicilio = validadorDomicilio;
        this.politicaContrasena = politicaContrasena;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.zonaHoraria = zonaHoraria;
    }

    // ===================================================================== Registro

    @Override
    public RegistroClienteResponse registrarCliente(ClienteRequest request) {
        log.info("Inicio registro de cliente");
        politicaContrasena.validar(request.getPassword());
        validarDatosUnicos(request);

        Cliente cliente = clienteMapper.toEntity(request);
        Domicilio domicilio = clienteMapper.toEntity(request.getDomicilio());
        validadorDomicilio.completar(domicilio);
        cliente.asignarDomicilio(domicilio);
        String passwordCifrado = passwordEncoder.encode(request.getPassword());

        RegistroClienteResponse response = transactionTemplate.execute(status -> guardarRegistro(cliente, passwordCifrado));
        log.info("Fin registro de cliente: id={}", cliente.getId());
        return response;
    }

    private void validarDatosUnicos(ClienteRequest request) {
        if (clienteRepository.existsByCurp(request.getCurp())) {
            throw new CurpDuplicadaException();
        }
        if (clienteRepository.existsByRfc(request.getRfc())) {
            throw new RfcDuplicadoException();
        }
        if (clienteRepository.existsByCorreo(request.getCorreo()) || usuarioRepository.existsByCorreo(request.getCorreo())) {
            throw new CorreoDuplicadoException();
        }
    }

    private RegistroClienteResponse guardarRegistro(Cliente cliente, String passwordCifrado) {
        try {
            clienteRepository.saveAndFlush(cliente);
            Cuenta cuenta = aperturaCuenta.abrir(cliente);
            Usuario usuario = usuarioRepository.saveAndFlush(crearUsuario(cliente, passwordCifrado));

            RegistroClienteResponse response = new RegistroClienteResponse();
            response.setCodigo(CODIGO_EXITO);
            response.setMensaje(MENSAJE_REGISTRO);
            response.setCliente(clienteMapper.toResponse(cliente));
            response.setCuenta(cuentaMapper.toResponse(cuenta));
            response.setUsuario(usuario.getCorreo());
            return response;
        } catch (DataIntegrityViolationException e) {
            throw traducirViolacion(e);
        }
    }

    private Usuario crearUsuario(Cliente cliente, String passwordCifrado) {
        Usuario usuario = new Usuario();
        usuario.setCliente(cliente);
        usuario.setCorreo(cliente.getCorreo());
        usuario.setPassword(passwordCifrado);
        usuario.setRol(Rol.CLIENTE);
        usuario.setActivo(true);
        return usuario;
    }

    // ===================================================================== Consultas

    @Override
    public ClientesResponse consultarClientes(ClienteFiltro filtro) {
        validarRangoFechas(filtro.getFechaRegistroDesde(), filtro.getFechaRegistroHasta());
        Specification<Cliente> criterios = Specification
                .where(nombreEmpiezaCon(filtro.getNombre()))
                .and(apellidoPaternoEmpiezaCon(filtro.getApellidoPaterno()))
                .and(apellidoMaternoEmpiezaCon(filtro.getApellidoMaterno()))
                .and(curpIgual(filtro.getCurp()))
                .and(rfcIgual(filtro.getRfc()))
                .and(correoIgual(filtro.getCorreo()))
                .and(tieneCuenta(filtro.getNumeroCuenta()))
                .and(activo(filtro.getActivo()))
                .and(registradoDesde(inicioDelDia(filtro.getFechaRegistroDesde())))
                .and(registradoAntesDe(inicioDelDiaSiguiente(filtro.getFechaRegistroHasta())));

        Page<Cliente> pagina = clienteRepository.findAll(criterios,
                PageRequest.of(filtro.getPagina(), filtro.getTamanio(), ORDEN_CLIENTES));

        ClientesResponse response = new ClientesResponse();
        response.setCodigo(CODIGO_EXITO);
        response.setMensaje(MENSAJE_EXITO);
        response.setClientes(clienteMapper.toResponseList(pagina.getContent()));
        response.asignarPaginacion(pagina);
        return response;
    }

    @Override
    public ClienteDetalleResponse consultarCliente(Integer id) {
        Cliente cliente = buscarCliente(id);
        return detalle(cliente, MENSAJE_EXITO);
    }

    private void validarRangoFechas(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ValidacionException("La fecha de registro inicial no puede ser posterior a la final");
        }
    }

    private OffsetDateTime inicioDelDia(LocalDate fecha) {
        return fecha == null ? null : fecha.atStartOfDay(zonaHoraria).toOffsetDateTime();
    }

    private OffsetDateTime inicioDelDiaSiguiente(LocalDate fecha) {
        return fecha == null ? null : inicioDelDia(fecha.plusDays(1));
    }

    // ===================================================================== Actualizacion

    @Override
    public ClienteDetalleResponse actualizarCliente(Integer id, ActualizaClienteRequest request) {
        if (request.sinCambios()) {
            throw new ValidacionException("No se envio ningun dato para actualizar");
        }
        log.info("Inicio actualizacion de cliente: id={}", id);
        Cliente cliente = buscarCliente(id);

        if (request.getCorreo() != null && clienteRepository.existsByCorreoAndIdNot(request.getCorreo(), id)) {
            throw new CorreoDuplicadoException();
        }
        clienteMapper.actualizar(request, cliente);
        actualizarDomicilio(request, cliente);

        Boolean activo = request.getActivo();
        boolean cambiaEstatus = activo != null && !activo.equals(cliente.getActivo());
        if (cambiaEstatus) {
            cliente.setActivo(activo);
        }

        ClienteDetalleResponse response = transactionTemplate.execute(status -> {
            Cliente actualizado = guardarCambios(cliente);
            if (cambiaEstatus) {
                aplicarCambioDeEstatus(id, activo);
            }
            return detalle(actualizado, mensajeActualizacion(cambiaEstatus, activo));
        });
        log.info("Fin actualizacion de cliente: id={}, cambioEstatus={}", id, cambiaEstatus);
        return response;
    }

    /** Si cambia el codigo postal o la colonia, el domicilio se vuelve a validar con la API. */
    private void actualizarDomicilio(ActualizaClienteRequest request, Cliente cliente) {
        if (request.getDomicilio() == null || request.getDomicilio().sinCambios()) {
            return;
        }
        clienteMapper.actualizar(request.getDomicilio(), cliente.getDomicilio());
        if (request.getDomicilio().cambiaUbicacion()) {
            validadorDomicilio.completar(cliente.getDomicilio());
        }
    }

    private Cliente guardarCambios(Cliente cliente) {
        try {
            return clienteRepository.saveAndFlush(cliente);
        } catch (DataIntegrityViolationException e) {
            throw traducirViolacion(e);
        }
    }

    /**
     * Baja logica: solo los clientes activos pueden tener cuentas activas, por eso sus cuentas
     * pasan a INACTIVA, y su usuario queda inactivo (ya no puede iniciar sesion). Al reactivar,
     * se reactiva el usuario; las cuentas se reactivan una por una.
     */
    private void aplicarCambioDeEstatus(Integer clienteId, boolean activo) {
        OffsetDateTime ahora = OffsetDateTime.now();
        if (!activo) {
            cuentaRepository.actualizarEstatusPorCliente(clienteId, EstatusCuenta.INACTIVA, ahora);
        }
        usuarioRepository.actualizarActivoPorCliente(clienteId, activo, ahora);
    }

    private static String mensajeActualizacion(boolean cambiaEstatus, Boolean activo) {
        if (!cambiaEstatus) {
            return MENSAJE_ACTUALIZACION;
        }
        return Boolean.TRUE.equals(activo) ? MENSAJE_REACTIVACION : MENSAJE_BAJA;
    }

    // ===================================================================== Comunes

    private Cliente buscarCliente(Integer id) {
        return clienteRepository.findById(id).orElseThrow(() -> new ClienteNoEncontradoException(id));
    }

    private ClienteDetalleResponse detalle(Cliente cliente, String mensaje) {
        ClienteDetalleResponse response = new ClienteDetalleResponse();
        response.setCodigo(CODIGO_EXITO);
        response.setMensaje(mensaje);
        response.setCliente(clienteMapper.toResponse(cliente));
        response.setCuentas(cuentaMapper.toResponseList(
                cuentaRepository.findByClienteIdOrderByFechaCreacion(cliente.getId())));
        return response;
    }

    private static RuntimeException traducirViolacion(DataIntegrityViolationException e) {
        ConstraintViolationException violacion = ExceptionUtils.throwableOfType(e, ConstraintViolationException.class);
        String restriccion = violacion == null ? null : violacion.getConstraintName();
        Supplier<RuntimeException> excepcion = restriccion == null ? null : RESTRICCIONES_UNICAS.get(restriccion);
        return excepcion == null ? e : excepcion.get();
    }
}
