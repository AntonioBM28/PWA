package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.enums.EstatusCuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Las busquedas combinables de cuentas (por cliente y por estatus, incluidas las cuentas activas)
 * se arman con CuentaSpecifications.
 */
@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Integer>, JpaSpecificationExecutor<Cuenta> {

    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cuenta c join fetch c.cliente where c.numeroCuenta = :numeroCuenta")
    Optional<Cuenta> bloquearPorNumeroCuenta(@Param("numeroCuenta") String numeroCuenta);

    /** Control de acceso: un CLIENTE solo puede consultar sus propias cuentas. */
    boolean existsByNumeroCuentaAndClienteId(String numeroCuenta, Integer clienteId);

    List<Cuenta> findByClienteIdOrderByFechaCreacion(Integer clienteId);

    @Query("select c.saldo from Cuenta c where c.numeroCuenta = :numeroCuenta")
    Optional<BigDecimal> consultarSaldo(@Param("numeroCuenta") String numeroCuenta);

    /** Usada en la baja logica: solo los clientes activos pueden tener cuentas activas. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Cuenta c set c.estatus = :estatus, c.fechaActualizacion = :fecha where c.cliente.id = :clienteId")
    int actualizarEstatusPorCliente(@Param("clienteId") Integer clienteId,
                                    @Param("estatus") EstatusCuenta estatus,
                                    @Param("fecha") OffsetDateTime fecha);
}
