package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.CodigoVerificacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, UUID> {

    /**
     * Se bloquea la fila mientras se valida: dos envios simultaneos del mismo codigo no pueden
     * usarlo dos veces ni saltarse el limite de intentos.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CodigoVerificacion c where c.id = :id")
    Optional<CodigoVerificacion> bloquear(@Param("id") UUID id);
}
