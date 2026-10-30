package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Transaccion;
import com.proyecto.servicios.enums.EstadoTransaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

public interface TransaccionRepository extends JpaRepository<Transaccion, UUID> {
    Optional<Transaccion> findByClienteIdAndClaveIdempotencia(Integer clienteId, String claveIdempotencia);
    Optional<Transaccion> findByIdAndClienteId(UUID id, Integer clienteId);
    List<Transaccion> findTop100ByEstadoAndFechaCreacionBetweenOrderByFechaCreacionAsc(
            EstadoTransaccion estado, OffsetDateTime desde, OffsetDateTime hasta);

    List<Transaccion> findTop100ByEstadoInAndFechaCreacionBetweenOrderByFechaCreacionAsc(
            List<EstadoTransaccion> estados, OffsetDateTime desde, OffsetDateTime hasta);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Transaccion t where t.id = :id")
    Optional<Transaccion> bloquearPorId(@Param("id") UUID id);

    @Query("select t from Transaccion t where t.clienteId = :clienteId and t.claveIdempotencia = :clave")
    Optional<Transaccion> buscarPorClave(@Param("clienteId") Integer clienteId, @Param("clave") String clave);
}
