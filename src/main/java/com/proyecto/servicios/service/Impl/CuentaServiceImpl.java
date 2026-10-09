package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.exception.ClienteInactivoException;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.mapper.CuentaMapper;
import com.proyecto.servicios.model.onboarding.ActualizaCuentaRequest;
import com.proyecto.servicios.model.onboarding.CrearCuentaRequest;
import com.proyecto.servicios.model.onboarding.CuentaDetalleResponse;
import com.proyecto.servicios.model.onboarding.CuentaFiltro;
import com.proyecto.servicios.model.onboarding.CuentasResponse;
import com.proyecto.servicios.model.onboarding.SaldoResponse;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.service.CuentaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.proyecto.servicios.repositorys.onboarding.CuentaSpecifications.conEstatus;
import static com.proyecto.servicios.repositorys.onboarding.CuentaSpecifications.deCliente;

/**
 * Consulta, apertura y cambio de estatus de cuentas. Regla de negocio: solo los clientes activos
 * pueden tener cuentas activas, por eso no se abre ni se activa una cuenta de un cliente inactivo.
 */
@Service
@Slf4j
public class CuentaServiceImpl implements CuentaService {

    static final int CODIGO_EXITO = 0;
    static final String MENSAJE_EXITO = "Exito";
    static final String MENSAJE_CREACION = "Cuenta creada correctamente";
    static final String MENSAJE_ACTUALIZACION = "Cuenta actualizada correctamente";

    private static final Sort ORDEN_CUENTAS = Sort.by("fechaCreacion", "id");

    private final CuentaRepository cuentaRepository;
    private final ClienteRepository clienteRepository;
    private final AperturaCuenta aperturaCuenta;
    private final CuentaMapper cuentaMapper;

    public CuentaServiceImpl(CuentaRepository cuentaRepository,
                             ClienteRepository clienteRepository,
                             AperturaCuenta aperturaCuenta,
                             CuentaMapper cuentaMapper) {
        this.cuentaRepository = cuentaRepository;
        this.clienteRepository = clienteRepository;
        this.aperturaCuenta = aperturaCuenta;
        this.cuentaMapper = cuentaMapper;
    }

    @Override
    public CuentaDetalleResponse consultarCuenta(String numeroCuenta) {
        return detalle(buscarCuenta(numeroCuenta), MENSAJE_EXITO);
    }

    @Override
    public CuentasResponse consultarCuentas(CuentaFiltro filtro) {
        if (filtro.getClienteId() != null && !clienteRepository.existsById(filtro.getClienteId())) {
            throw new ClienteNoEncontradoException(filtro.getClienteId());
        }
        Specification<Cuenta> criterios = Specification
                .where(deCliente(filtro.getClienteId()))
                .and(conEstatus(filtro.getEstatus()));
        Page<Cuenta> pagina = cuentaRepository.findAll(criterios,
                PageRequest.of(filtro.getPagina(), filtro.getTamanio(), ORDEN_CUENTAS));

        CuentasResponse response = new CuentasResponse();
        response.setCodigo(CODIGO_EXITO);
        response.setMensaje(MENSAJE_EXITO);
        response.setCuentas(cuentaMapper.toResponseList(pagina.getContent()));
        response.asignarPaginacion(pagina);
        return response;
    }

    @Override
    public SaldoResponse consultarSaldo(String numeroCuenta) {
        SaldoResponse response = new SaldoResponse();
        response.setCodigo(CODIGO_EXITO);
        response.setMensaje(MENSAJE_EXITO);
        response.setNumeroCuenta(numeroCuenta);
        response.setSaldo(cuentaRepository.consultarSaldo(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta)));
        return response;
    }

    @Override
    @Transactional
    public CuentaDetalleResponse crearCuenta(CrearCuentaRequest request) {
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ClienteNoEncontradoException(request.getClienteId()));
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ClienteInactivoException("No se puede abrir una cuenta: el cliente " + cliente.getId()
                    + " esta dado de baja");
        }
        Cuenta cuenta = aperturaCuenta.abrir(cliente);
        log.info("Cuenta abierta para el cliente id={}", cliente.getId());
        return detalle(cuenta, MENSAJE_CREACION);
    }

    @Override
    @Transactional
    public CuentaDetalleResponse actualizarCuenta(String numeroCuenta, ActualizaCuentaRequest request) {
        Cuenta cuenta = buscarCuenta(numeroCuenta);
        Cliente cliente = cuenta.getCliente();
        if (request.getEstatus() == EstatusCuenta.ACTIVA && !Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ClienteInactivoException("No se puede activar la cuenta: el cliente " + cliente.getId()
                    + " esta dado de baja. Primero reactive al cliente");
        }
        cuenta.setEstatus(request.getEstatus());
        Cuenta actualizada = cuentaRepository.saveAndFlush(cuenta);
        log.info("Cuenta del cliente id={} con estatus {}", cliente.getId(), request.getEstatus());
        return detalle(actualizada, MENSAJE_ACTUALIZACION);
    }

    private Cuenta buscarCuenta(String numeroCuenta) {
        return cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));
    }

    private CuentaDetalleResponse detalle(Cuenta cuenta, String mensaje) {
        CuentaDetalleResponse response = new CuentaDetalleResponse();
        response.setCodigo(CODIGO_EXITO);
        response.setMensaje(mensaje);
        response.setCuenta(cuentaMapper.toResponse(cuenta));
        return response;
    }
}
