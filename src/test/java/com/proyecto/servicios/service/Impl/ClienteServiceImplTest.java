package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Domicilio;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CodigoPostalException;
import com.proyecto.servicios.exception.ContrasenaInvalidaException;
import com.proyecto.servicios.exception.CorreoDuplicadoException;
import com.proyecto.servicios.exception.CurpDuplicadaException;
import com.proyecto.servicios.exception.RfcDuplicadoException;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.mapper.CuentaMapper;
import com.proyecto.servicios.model.CodigoPostalResponse;
import com.proyecto.servicios.model.onboarding.ActualizaClienteRequest;
import com.proyecto.servicios.model.onboarding.ActualizaDomicilioRequest;
import com.proyecto.servicios.model.onboarding.ClienteDetalleResponse;
import com.proyecto.servicios.model.onboarding.ClienteFiltro;
import com.proyecto.servicios.model.onboarding.ClienteRequest;
import com.proyecto.servicios.model.onboarding.ClienteRequestFixture;
import com.proyecto.servicios.model.onboarding.ClientesResponse;
import com.proyecto.servicios.model.onboarding.RegistroClienteResponse;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.service.CodigoPostalService;
import com.proyecto.servicios.validation.PoliticaContrasena;
import com.proyecto.servicios.validation.ValidadorDomicilio;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceImplTest {

    private static final BigDecimal SALDO_INICIAL = new BigDecimal("500.00");
    private static final String NUMERO_CUENTA = "1000000000";
    private static final Integer ID_CLIENTE = 5;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private CodigoPostalService codigoPostalService;

    @Mock
    private PlatformTransactionManager transactionManager;

    private final ClienteMapper clienteMapper = Mappers.getMapper(ClienteMapper.class);
    private final CuentaMapper cuentaMapper = Mappers.getMapper(CuentaMapper.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private ClienteServiceImpl service;
    private ClienteRequest request;

    @BeforeEach
    void setUp() {
        service = crearServicio(SALDO_INICIAL);
        request = ClienteRequestFixture.requestValido();
    }

    @Test
    void registrarCliente_creaClienteDomicilioCuentaYUsuario() {
        when(codigoPostalService.consultarCodigoPostal("37806")).thenReturn(codigoPostal());
        when(clienteRepository.saveAndFlush(any(Cliente.class))).thenAnswer(inv -> {
            Cliente cliente = inv.getArgument(0);
            cliente.setId(7);
            return cliente;
        });
        when(cuentaRepository.saveAndFlush(any(Cuenta.class))).thenAnswer(inv -> {
            Cuenta cuenta = inv.getArgument(0);
            cuenta.setNumeroCuenta(NUMERO_CUENTA);
            return cuenta;
        });
        when(usuarioRepository.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistroClienteResponse response = service.registrarCliente(request);

        assertEquals(ClienteServiceImpl.CODIGO_EXITO, response.getCodigo());
        assertEquals(7, response.getCliente().getId());
        assertEquals("BAMJ900512HGTLRS09", response.getCliente().getCurp());

        // Domicilio completado con la API: colonia con el nombre exacto, estado y municipio de la API
        assertEquals("Ampliación 15 de Septiembre", response.getCliente().getDomicilio().getColonia());
        assertEquals("Guanajuato", response.getCliente().getDomicilio().getEstado());
        assertEquals("Dolores Hidalgo", response.getCliente().getDomicilio().getMunicipio());
        assertEquals("MEX", response.getCliente().getDomicilio().getPais());

        // Cuenta ACTIVA con el saldo inicial configurado
        assertEquals(NUMERO_CUENTA, response.getCuenta().getNumeroCuenta());
        assertEquals(EstatusCuenta.ACTIVA, response.getCuenta().getEstatus());
        assertEquals(SALDO_INICIAL, response.getCuenta().getSaldo());
        assertEquals(7, response.getCuenta().getClienteId());

        // Usuario activo, con el correo del cliente y la contrasena cifrada con BCrypt
        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).saveAndFlush(usuarioCaptor.capture());
        Usuario usuario = usuarioCaptor.getValue();
        assertEquals("jose@mail.com", usuario.getCorreo());
        assertEquals("jose@mail.com", response.getUsuario());
        assertTrue(usuario.getActivo());
        assertNotEquals(request.getPassword(), usuario.getPassword());
        assertTrue(passwordEncoder.matches(request.getPassword(), usuario.getPassword()));
    }

    @Test
    void registrarCliente_curpDuplicada() {
        when(clienteRepository.existsByCurp(request.getCurp())).thenReturn(true);

        assertThrows(CurpDuplicadaException.class, () -> service.registrarCliente(request));
        verifyNoGuardado();
    }

    @Test
    void registrarCliente_rfcDuplicado() {
        when(clienteRepository.existsByRfc(request.getRfc())).thenReturn(true);

        assertThrows(RfcDuplicadoException.class, () -> service.registrarCliente(request));
        verifyNoGuardado();
    }

    @Test
    void registrarCliente_correoDuplicadoEnClientes() {
        when(clienteRepository.existsByCorreo(request.getCorreo())).thenReturn(true);

        assertThrows(CorreoDuplicadoException.class, () -> service.registrarCliente(request));
        verifyNoGuardado();
    }

    @Test
    void registrarCliente_correoDuplicadoEnUsuarios() {
        when(usuarioRepository.existsByCorreo(request.getCorreo())).thenReturn(true);

        assertThrows(CorreoDuplicadoException.class, () -> service.registrarCliente(request));
        verifyNoGuardado();
    }

    @Test
    void registrarCliente_contrasenaInvalida_noConsultaNada() {
        request.setPassword("sinmayusculas1!");

        assertThrows(ContrasenaInvalidaException.class, () -> service.registrarCliente(request));
        verifyNoInteractions(clienteRepository, codigoPostalService);
        verifyNoGuardado();
    }

    @Test
    void registrarCliente_coloniaQueNoPerteneceAlCodigoPostal() {
        request.getDomicilio().setColonia("Polanco");
        when(codigoPostalService.consultarCodigoPostal("37806")).thenReturn(codigoPostal());

        ValidacionException ex = assertThrows(ValidacionException.class, () -> service.registrarCliente(request));

        assertTrue(ex.getMessage().contains("no pertenece al codigo postal 37806"));
        verifyNoGuardado();
    }

    @Test
    void registrarCliente_codigoPostalInexistente() {
        when(codigoPostalService.consultarCodigoPostal("37806"))
                .thenThrow(new CodigoPostalException("El codigo postal no existe", HttpStatus.NOT_FOUND));

        assertThrows(CodigoPostalException.class, () -> service.registrarCliente(request));
        verifyNoGuardado();
    }

    @Test
    void registrarCliente_registroSimultaneo_traduceLaRestriccionUnica() {
        when(codigoPostalService.consultarCodigoPostal("37806")).thenReturn(codigoPostal());
        when(clienteRepository.saveAndFlush(any(Cliente.class))).thenThrow(new DataIntegrityViolationException("duplicado",
                new ConstraintViolationException("duplicado", new SQLException("duplicado"), "uq_clientes_rfc")));

        assertThrows(RfcDuplicadoException.class, () -> service.registrarCliente(request));
        verify(cuentaRepository, never()).saveAndFlush(any());
    }

    @Test
    void saldoInicialNegativo_impideIniciarElServicio() {
        BigDecimal negativo = new BigDecimal("-1");

        assertThrows(IllegalStateException.class, () -> crearServicio(negativo));
    }

    // ===================================================================== Consultas

    @Test
    void consultarClientes_devuelvePaginaOrdenadaPorApellidos() {
        ClienteFiltro filtro = new ClienteFiltro();
        filtro.setApellidoPaterno("bal");
        filtro.setPagina(1);
        filtro.setTamanio(5);
        when(clienteRepository.findAll(any(Specification.class), any(Pageable.class))).thenAnswer(inv -> {
            Pageable pageable = inv.getArgument(1);
            return new PageImpl<>(List.of(clienteExistente(true)), pageable, 6);
        });

        ClientesResponse response = service.consultarClientes(filtro);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(clienteRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(1, pageable.getPageNumber());
        assertEquals(5, pageable.getPageSize());
        assertEquals("apellidoPaterno", pageable.getSort().iterator().next().getProperty());

        assertEquals(1, response.getClientes().size());
        assertEquals(ID_CLIENTE, response.getClientes().get(0).getId());
        assertEquals(1, response.getPagina());
        assertEquals(6, response.getTotalElementos());
        assertEquals(2, response.getTotalPaginas());
    }

    @Test
    void consultarClientes_rangoDeFechasInvertido() {
        ClienteFiltro filtro = new ClienteFiltro();
        filtro.setFechaRegistroDesde(LocalDate.of(2026, 12, 31));
        filtro.setFechaRegistroHasta(LocalDate.of(2026, 1, 1));

        assertThrows(ValidacionException.class, () -> service.consultarClientes(filtro));
        verifyNoInteractions(clienteRepository);
    }

    @Test
    void consultarCliente_incluyeDomicilioYCuentas() {
        when(clienteRepository.findById(ID_CLIENTE)).thenReturn(Optional.of(clienteExistente(true)));
        when(cuentaRepository.findByClienteIdOrderByFechaCreacion(ID_CLIENTE)).thenReturn(List.of(cuenta()));

        ClienteDetalleResponse response = service.consultarCliente(ID_CLIENTE);

        assertEquals("Guanajuato", response.getCliente().getDomicilio().getEstado());
        assertEquals(NUMERO_CUENTA, response.getCuentas().get(0).getNumeroCuenta());
    }

    @Test
    void consultarCliente_noExiste() {
        when(clienteRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ClienteNoEncontradoException.class, () -> service.consultarCliente(99));
    }

    // ===================================================================== Actualizacion

    @Test
    void actualizarCliente_sinDatos_rechazaLaPeticion() {
        ActualizaClienteRequest cambios = new ActualizaClienteRequest();

        assertThrows(ValidacionException.class, () -> service.actualizarCliente(ID_CLIENTE, cambios));
        verifyNoInteractions(clienteRepository);
    }

    @Test
    void actualizarCliente_clienteNoExiste() {
        ActualizaClienteRequest cambios = new ActualizaClienteRequest();
        cambios.setOcupacion("Arquitecto");
        when(clienteRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ClienteNoEncontradoException.class, () -> service.actualizarCliente(99, cambios));
    }

    @Test
    void actualizarCliente_modificaSoloLosCamposEnviados() {
        prepararActualizacion(clienteExistente(true));
        ActualizaClienteRequest cambios = new ActualizaClienteRequest();
        cambios.setTelefonoMovil("3311112222");
        cambios.setSegundoNombre("");
        cambios.setIngresoMensual(new BigDecimal("40000.00"));
        ActualizaDomicilioRequest domicilio = new ActualizaDomicilioRequest();
        domicilio.setCalle("Juarez");
        cambios.setDomicilio(domicilio);

        ClienteDetalleResponse response = service.actualizarCliente(ID_CLIENTE, cambios);

        Cliente guardado = clienteGuardado();
        assertEquals("3311112222", guardado.getTelefonoMovil());
        assertNull(guardado.getSegundoNombre());
        assertEquals(new BigDecimal("40000.00"), guardado.getIngresoMensual());
        assertEquals("Juarez", guardado.getDomicilio().getCalle());
        // Lo que no se envio no cambia
        assertEquals("Jose", guardado.getNombre());
        assertEquals("BAMJ900512HGTLRS09", guardado.getCurp());
        assertEquals("Ampliación 15 de Septiembre", guardado.getDomicilio().getColonia());
        assertEquals(ClienteServiceImpl.MENSAJE_ACTUALIZACION, response.getMensaje());
        // Cambiar solo la calle no requiere consultar la API de codigos postales
        verifyNoInteractions(codigoPostalService);
        verify(cuentaRepository, never()).actualizarEstatusPorCliente(any(), any(), any());
        verify(usuarioRepository, never()).actualizarActivoPorCliente(any(), anyBoolean(), any());
    }

    @Test
    void actualizarCliente_cambioDeColonia_seValidaConLaApi() {
        prepararActualizacion(clienteExistente(true));
        when(codigoPostalService.consultarCodigoPostal("37806")).thenReturn(codigoPostal());
        ActualizaClienteRequest cambios = new ActualizaClienteRequest();
        ActualizaDomicilioRequest domicilio = new ActualizaDomicilioRequest();
        domicilio.setColonia("dos plazas");
        cambios.setDomicilio(domicilio);

        service.actualizarCliente(ID_CLIENTE, cambios);

        assertEquals("Dos Plazas", clienteGuardado().getDomicilio().getColonia());
    }

    @Test
    void actualizarCliente_coloniaQueNoPerteneceAlCodigoPostal() {
        when(clienteRepository.findById(ID_CLIENTE)).thenReturn(Optional.of(clienteExistente(true)));
        when(codigoPostalService.consultarCodigoPostal("37806")).thenReturn(codigoPostal());
        ActualizaClienteRequest cambios = new ActualizaClienteRequest();
        ActualizaDomicilioRequest domicilio = new ActualizaDomicilioRequest();
        domicilio.setColonia("Polanco");
        cambios.setDomicilio(domicilio);

        assertThrows(ValidacionException.class, () -> service.actualizarCliente(ID_CLIENTE, cambios));
        verify(clienteRepository, never()).saveAndFlush(any());
    }

    @Test
    void actualizarCliente_correoDeOtroCliente() {
        when(clienteRepository.findById(ID_CLIENTE)).thenReturn(Optional.of(clienteExistente(true)));
        when(clienteRepository.existsByCorreoAndIdNot("otro@mail.com", ID_CLIENTE)).thenReturn(true);
        ActualizaClienteRequest cambios = new ActualizaClienteRequest();
        cambios.setCorreo("Otro@Mail.com");

        assertThrows(CorreoDuplicadoException.class, () -> service.actualizarCliente(ID_CLIENTE, cambios));
        verify(clienteRepository, never()).saveAndFlush(any());
    }

    @Test
    void actualizarCliente_bajaLogica_inactivaCuentasYUsuario() {
        prepararActualizacion(clienteExistente(true));
        ActualizaClienteRequest cambios = new ActualizaClienteRequest();
        cambios.setActivo(false);

        ClienteDetalleResponse response = service.actualizarCliente(ID_CLIENTE, cambios);

        assertFalse(clienteGuardado().getActivo());
        verify(cuentaRepository).actualizarEstatusPorCliente(eq(ID_CLIENTE), eq(EstatusCuenta.INACTIVA), any());
        verify(usuarioRepository).actualizarActivoPorCliente(eq(ID_CLIENTE), eq(false), any());
        assertEquals(ClienteServiceImpl.MENSAJE_BAJA, response.getMensaje());
    }

    @Test
    void actualizarCliente_reactivacion_reactivaUsuarioPeroNoLasCuentas() {
        prepararActualizacion(clienteExistente(false));
        ActualizaClienteRequest cambios = new ActualizaClienteRequest();
        cambios.setActivo(true);

        ClienteDetalleResponse response = service.actualizarCliente(ID_CLIENTE, cambios);

        assertTrue(clienteGuardado().getActivo());
        verify(usuarioRepository).actualizarActivoPorCliente(eq(ID_CLIENTE), eq(true), any());
        verify(cuentaRepository, never()).actualizarEstatusPorCliente(any(), any(), any());
        assertEquals(ClienteServiceImpl.MENSAJE_REACTIVACION, response.getMensaje());
    }

    @Test
    void actualizarCliente_bajaDeClienteYaInactivo_noRepiteLaBaja() {
        prepararActualizacion(clienteExistente(false));
        ActualizaClienteRequest cambios = new ActualizaClienteRequest();
        cambios.setActivo(false);

        service.actualizarCliente(ID_CLIENTE, cambios);

        verify(cuentaRepository, never()).actualizarEstatusPorCliente(any(), any(), any());
        verify(usuarioRepository, never()).actualizarActivoPorCliente(any(), anyBoolean(), any());
    }

    private void prepararActualizacion(Cliente existente) {
        when(clienteRepository.findById(ID_CLIENTE)).thenReturn(Optional.of(existente));
        when(clienteRepository.saveAndFlush(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cuentaRepository.findByClienteIdOrderByFechaCreacion(ID_CLIENTE)).thenReturn(List.of(cuenta()));
    }

    private Cliente clienteGuardado() {
        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private Cliente clienteExistente(boolean activo) {
        Cliente cliente = clienteMapper.toEntity(ClienteRequestFixture.requestValido());
        cliente.setId(ID_CLIENTE);
        cliente.setActivo(activo);
        Domicilio domicilio = clienteMapper.toEntity(ClienteRequestFixture.requestValido().getDomicilio());
        domicilio.setColonia("Ampliación 15 de Septiembre");
        domicilio.setMunicipio("Dolores Hidalgo");
        domicilio.setEstado("Guanajuato");
        cliente.asignarDomicilio(domicilio);
        return cliente;
    }

    private Cuenta cuenta() {
        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta(NUMERO_CUENTA);
        cuenta.setEstatus(EstatusCuenta.ACTIVA);
        cuenta.setSaldo(SALDO_INICIAL);
        return cuenta;
    }

    private void verifyNoGuardado() {
        verify(clienteRepository, never()).saveAndFlush(any());
        verify(cuentaRepository, never()).saveAndFlush(any());
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    private ClienteServiceImpl crearServicio(BigDecimal saldoInicial) {
        return new ClienteServiceImpl(clienteRepository, cuentaRepository, usuarioRepository, clienteMapper,
                cuentaMapper, new AperturaCuenta(cuentaRepository, saldoInicial),
                new ValidadorDomicilio(codigoPostalService), new PoliticaContrasena(), passwordEncoder,
                transactionManager, ZoneId.of("America/Mexico_City"));
    }

    private static CodigoPostalResponse codigoPostal() {
        CodigoPostalResponse response = new CodigoPostalResponse();
        response.setCodigoPostal("37806");
        response.setEstado("Guanajuato");
        response.setMunicipio("Dolores Hidalgo");
        response.setColonias(List.of("15 de Septiembre", "Ampliación 15 de Septiembre", "Dos Plazas"));
        return response;
    }
}
