package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Sesion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SesionRepository extends JpaRepository<Sesion, UUID> {

    /**
     * Sesiones abiertas y vigentes de un usuario, bloqueadas hasta el fin de la transaccion: dos
     * inicios de sesion simultaneos del mismo usuario no pueden dejar dos sesiones abiertas.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sesion s where s.usuarioId = :usuarioId and s.fechaCierre is null "
            + "and s.fechaExpiracion > :ahora")
    List<Sesion> bloquearAbiertas(@Param("usuarioId") Integer usuarioId, @Param("ahora") OffsetDateTime ahora);

    @Query("select s from Sesion s where s.usuarioId = :usuarioId and s.fechaCierre is null "
            + "and s.fechaExpiracion > :ahora")
    List<Sesion> buscarAbiertas(@Param("usuarioId") Integer usuarioId, @Param("ahora") OffsetDateTime ahora);
}
