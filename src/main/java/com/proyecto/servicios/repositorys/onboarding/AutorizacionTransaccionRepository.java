package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.AutorizacionTransaccion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AutorizacionTransaccionRepository extends JpaRepository<AutorizacionTransaccion, UUID> {

    /** Bloquea la fila al consumir el token: no puede usarse dos veces en paralelo. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AutorizacionTransaccion a where a.id = :id")
    Optional<AutorizacionTransaccion> bloquear(@Param("id") UUID id);
}
