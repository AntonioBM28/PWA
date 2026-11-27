package com.proyecto.servicios.service.transaccion;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Transaccion;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.exception.TransaccionException;
import com.proyecto.servicios.model.transaccion.OperacionFiltro;
import com.proyecto.servicios.model.transaccion.OperacionResponse;
import com.proyecto.servicios.model.transaccion.OperacionesResponse;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.TransaccionRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperacionServiceTest {

    @Mock
    private TransaccionRepository transaccionRepository;
    @Mock
    private CuentaRepository cuentaRepository;
    @Mock
    private ClienteRepository clienteRepository;

    private OperacionService service;

    @BeforeEach
    void setUp() {
        service = new OperacionService(transaccionRepository, cuentaRepository, clienteRepository,
                "America/Mexico_City", "https://img.test/servicios");
    }

    @SuppressWarnings("unchecked")
    private void conTransacciones(Transaccion... transacciones) {
        when(transaccionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(transacciones), PageRequest.of(0, 20), transacciones.length));
        Cuenta cuenta = new Cuenta();
        cuenta.setId(3);
        cuenta.setNumeroCuenta("1000000009");
        when(cuentaRepository.findAllById(any())).thenReturn(List.of(cuenta));
    }

    @Test
    void ejecutivo_veOperacionesDeTodosConNombreDelCliente() {
        conTransacciones(transaccion());
        Cliente cliente = new Cliente();
        cliente.setId(7);
        cliente.setNombre("Jose");
        cliente.setApellidoPaterno("Balderas");
        cliente.setApellidoMaterno("Martinez");
        when(clienteRepository.findAllById(any())).thenReturn(List.of(cliente));

        OperacionesResponse response = service.consultar(new OperacionFiltro(), true, null);

        OperacionResponse op = response.getOperaciones().get(0);
        assertEquals("Jose Balderas Martinez", op.getNombreCliente());
        assertEquals("1000000009", op.getNumeroCuenta());
        assertEquals(r("Telcel $100"), op.getProducto());
        assertEquals("https://img.test/servicios/79.png", op.getImagenUrl());
        assertEquals("******5678", op.getReferencia());
        assertEquals("Operacion aprobada", op.getDetalle());
        assertEquals(1, response.getTotalElementos());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Pageable> pagina = ArgumentCaptor.forClass(Pageable.class);
        verify(transaccionRepository).findAll(any(Specification.class), pagina.capture());
        assertEquals(Sort.Direction.DESC, pagina.getValue().getSort().getOrderFor("fechaCreacion").getDirection());
    }

    @Test
    void cliente_soloVeLasSuyas_sinNombreDeCliente() {
        conTransacciones(transaccion());
        OperacionFiltro filtro = new OperacionFiltro();
        filtro.setNombreCliente("otro");

        OperacionesResponse response = service.consultar(filtro, false, 7);

        assertNull(response.getOperaciones().get(0).getNombreCliente());
        verify(clienteRepository, never()).findAllById(any());
    }

    @Test
    void cliente_noPuedePedirOperacionesDeOtroCliente() {
        OperacionFiltro filtro = new OperacionFiltro();
        filtro.setClienteId(8);

        assertThrows(TransaccionException.class, () -> service.consultar(filtro, false, 7));
        assertThrows(TransaccionException.class, () -> service.consultar(new OperacionFiltro(), false, null));
    }

    @Test
    void detalle_porEstado() {
        assertTrue(OperacionService.detalle(EstadoTransaccion.RECHAZADA).contains("devuelto"));
        assertTrue(OperacionService.detalle(EstadoTransaccion.CONCILIACION).contains("retenido"));
    }

    @Test
    void enmascarar_referencia() {
        assertEquals("******5678", TransaccionService.enmascarar("5512345678"));
        assertEquals("123", TransaccionService.enmascarar(" 123 "));
        assertNull(TransaccionService.enmascarar("  "));
    }

    @Test
    void filtro_validaFechasYTexto() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            OperacionFiltro filtro = new OperacionFiltro();
            filtro.setFechaDesde(LocalDate.of(2026, 11, 30));
            filtro.setFechaHasta(LocalDate.of(2026, 11, 1));
            filtro.setNombreCliente("Jose1");
            filtro.setTexto("<b>");
            Set<String> mensajes = factory.getValidator().validate(filtro).stream()
                    .map(ConstraintViolation::getMessage).collect(Collectors.toSet());
            assertTrue(mensajes.contains("La fecha inicial no puede ser posterior a la final"), mensajes::toString);
            assertTrue(mensajes.contains("El nombre del cliente solo admite letras y espacios"), mensajes::toString);
            assertTrue(mensajes.stream().anyMatch(m -> m.startsWith("El texto de busqueda")), mensajes::toString);
        }
    }

    private static String r(String s) {
        return s;
    }

    private static Transaccion transaccion() {
        Transaccion tx = new Transaccion();
        tx.setId(UUID.randomUUID());
        tx.setClienteId(7);
        tx.setCuentaId(3);
        tx.setMonto(new BigDecimal("100.00"));
        tx.setEstado(EstadoTransaccion.APROBADA);
        tx.setIdProducto(200);
        tx.setIdServicio(79);
        tx.setProducto("Telcel $100");
        tx.setServicio("Telcel");
        tx.setReferencia("******5678");
        tx.setFechaCreacion(OffsetDateTime.now());
        return tx;
    }
}
