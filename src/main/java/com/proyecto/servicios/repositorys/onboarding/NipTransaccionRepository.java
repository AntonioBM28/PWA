package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.NipTransaccion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NipTransaccionRepository extends JpaRepository<NipTransaccion, Integer> {

    /** Bloquea la fila al validar: intentos simultaneos no pueden saltarse el limite. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from NipTransaccion n where n.usuarioId = :usuarioId")
    Optional<NipTransaccion> bloquear(@Param("usuarioId") Integer usuarioId);
}
