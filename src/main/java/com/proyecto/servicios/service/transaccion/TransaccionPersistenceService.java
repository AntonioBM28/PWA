package com.proyecto.servicios.service.transaccion;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Transaccion;
import com.proyecto.servicios.enums.EstatusCuenta;
import com.proyecto.servicios.enums.EstadoTransaccion;
import com.proyecto.servicios.exception.TransaccionException;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.TransaccionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TransaccionPersistenceService {
    private final CuentaRepository cuentaRepository;
    private final TransaccionRepository transaccionRepository;

    public TransaccionPersistenceService(CuentaRepository cuentaRepository,
                                         TransaccionRepository transaccionRepository) {
        this.cuentaRepository = cuentaRepository;
        this.transaccionRepository = transaccionRepository;
    }

    @Transactional
    public Reserva reservar(String numeroCuenta, Integer clienteAutenticado, boolean ejecutivo,
                            String clave, String huella, String upc, String random, String signed,
                            BigDecimal monto) {
        Cuenta cuenta = cuentaRepository.bloquearPorNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new TransaccionException("La cuenta no existe", HttpStatus.NOT_FOUND));
        Integer clienteId = cuenta.getCliente().getId();
        if (!ejecutivo && (clienteAutenticado == null || !clienteAutenticado.equals(clienteId))) {
            throw new TransaccionException("No tiene autorización para operar esta cuenta", HttpStatus.FORBIDDEN);
        }
        var existente = transaccionRepository.findByClienteIdAndClaveIdempotencia(clienteId, clave);
        if (existente.isPresent()) {
            Transaccion tx = existente.get();
            if (!tx.getHuellaSolicitud().equals(huella)) {
                throw new TransaccionException("La clave de idempotencia ya se usó con otros datos", HttpStatus.CONFLICT);
            }
            return new Reserva(tx, true);
        }
        if (!Boolean.TRUE.equals(cuenta.getCliente().getActivo())) {
            throw new TransaccionException("El cliente no está activo", HttpStatus.FORBIDDEN);
        }
        if (cuenta.getEstatus() != EstatusCuenta.ACTIVA) {
            throw new TransaccionException("La cuenta no está activa", HttpStatus.FORBIDDEN);
        }
        if (cuenta.getSaldo() == null || cuenta.getSaldo().compareTo(monto) < 0) {
            throw new TransaccionException("Saldo insuficiente", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        // El débito y el registro idempotente se confirman juntos antes de la llamada HTTP.
        cuenta.setSaldo(cuenta.getSaldo().subtract(monto));
        Transaccion tx = new Transaccion();
        tx.setId(UUID.randomUUID());
        tx.setClienteId(clienteId);
        tx.setCuentaId(cuenta.getId());
        tx.setClaveIdempotencia(clave);
        tx.setHuellaSolicitud(huella);
        tx.setUpc(upc);
        tx.setRandomProveedor(random);
        tx.setPayloadCifrado(signed);
        tx.setMonto(monto);
        tx.setEstado(EstadoTransaccion.PENDIENTE);
        transaccionRepository.saveAndFlush(tx);
        return new Reserva(tx, false);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<Transaccion> buscarPorClave(String numeroCuenta, Integer clienteAutenticado,
                                                          boolean ejecutivo, String clave) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new TransaccionException("La cuenta no existe", HttpStatus.NOT_FOUND));
        Integer clienteId = cuenta.getCliente().getId();
        if (!ejecutivo && (clienteAutenticado == null || !clienteAutenticado.equals(clienteId))) {
            throw new TransaccionException("No tiene autorización para operar esta cuenta", HttpStatus.FORBIDDEN);
        }
        return transaccionRepository.buscarPorClave(clienteId, clave);
    }

    @Transactional
    public Transaccion finalizar(UUID id, EstadoTransaccion estado, String codigo,
                                 String mensaje, String idTx, String autorizacion,
                                 String pin, String folio, String leyenda) {
        Transaccion tx = transaccionRepository.bloquearPorId(id)
                .orElseThrow(() -> new TransaccionException("La transacción no existe", HttpStatus.NOT_FOUND));
        if (tx.getEstado() == EstadoTransaccion.APROBADA || tx.getEstado() == EstadoTransaccion.RECHAZADA) {
            return tx;
        }
        if (estado == EstadoTransaccion.RECHAZADA) {
            Cuenta cuenta = cuentaRepository.findById(tx.getCuentaId())
                    .orElseThrow(() -> new IllegalStateException("La cuenta asociada no existe"));
            cuenta.setSaldo(cuenta.getSaldo().add(tx.getMonto()));
        }
        tx.setEstado(estado);
        tx.setCodigoProveedor(codigo);
        tx.setMensajeProveedor(limitar(mensaje, 500));
        tx.setIdTx(limitar(idTx, 32));
        tx.setNumeroAutorizacion(limitar(autorizacion, 60));
        tx.setPin(limitar(pin, 250));
        tx.setFolio(limitar(folio, 250));
        tx.setLeyenda(limitar(leyenda, 5000));
        return transaccionRepository.save(tx);
    }

    @Transactional
    public Transaccion dejarEnConciliacion(UUID id, String codigo, String mensaje) {
        Transaccion tx = transaccionRepository.bloquearPorId(id)
                .orElseThrow(() -> new TransaccionException("La transacción no existe", HttpStatus.NOT_FOUND));
        if (tx.getEstado() == EstadoTransaccion.PENDIENTE || tx.getEstado() == EstadoTransaccion.CONCILIACION) {
            tx.setEstado(EstadoTransaccion.CONCILIACION);
            tx.setCodigoProveedor(limitar(codigo, 8));
            // Solo una descripción interna controlada; jamás se refleja en la API.
            tx.setMensajeProveedor(limitar(mensaje, 500));
            return transaccionRepository.save(tx);
        }
        return tx;
    }

    @Transactional(readOnly = true)
    public Transaccion buscar(UUID id, Integer clienteAutenticado, boolean ejecutivo) {
        Transaccion tx = transaccionRepository.findById(id)
                .orElseThrow(() -> new TransaccionException("La transacción no existe", HttpStatus.NOT_FOUND));
        if (!ejecutivo && !tx.getClienteId().equals(clienteAutenticado)) {
            throw new TransaccionException("No tiene autorización para consultar esta transacción", HttpStatus.FORBIDDEN);
        }
        return tx;
    }

    @Transactional(readOnly = true)
    public List<Transaccion> pendientesParaConfirmar(OffsetDateTime desde, OffsetDateTime hasta) {
        // PENDIENTE también se consulta: el proceso puede caer después del débito
        // y antes de persistir que la llamada HTTP comenzó.
        return transaccionRepository.findTop100ByEstadoInAndFechaCreacionBetweenOrderByFechaCreacionAsc(
                List.of(EstadoTransaccion.PENDIENTE, EstadoTransaccion.CONCILIACION), desde, hasta);
    }

    @Transactional
    public Transaccion marcarPendiente(UUID id) {
        Transaccion tx = transaccionRepository.findById(id).orElseThrow();
        if (tx.getEstado() == EstadoTransaccion.PENDIENTE || tx.getEstado() == EstadoTransaccion.CONCILIACION) {
            tx.setEstado(EstadoTransaccion.CONCILIACION);
            return transaccionRepository.save(tx);
        }
        return tx;
    }

    private static String limitar(String value, int max) {
        if (value == null || value.length() <= max) return value;
        return value.substring(0, max);
    }

    public record Reserva(Transaccion transaccion, boolean repetida) { }
}
