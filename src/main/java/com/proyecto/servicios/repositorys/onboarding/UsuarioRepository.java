package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.enums.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Las busquedas combinables de usuarios (GET /usuarios/filtro) se arman con UsuarioSpecifications.
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer>, JpaSpecificationExecutor<Usuario> {

    /** Login: el correo es el nombre de usuario (usa uq_usuarios_correo). */
    Optional<Usuario> findByCorreo(String correo);

    boolean existsByCorreo(String correo);

    /** Regla: cada cliente puede tener unicamente un usuario (usa uq_usuarios_cliente). */
    boolean existsByClienteId(Integer clienteId);

    /**
     * Se consulta en cada peticion autenticada: un usuario inactivo ya no puede usar su token, y el
     * rol se toma de la base de datos (no del token) para que un cambio de rol aplique de inmediato.
     */
    @Query("select u.rol from Usuario u where u.id = :id and u.activo = true")
    Optional<Rol> buscarRolDeUsuarioActivo(@Param("id") Integer id);

    /** Usada en la baja logica: el usuario de un cliente inactivo queda inactivo. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Usuario u set u.activo = :activo, u.fechaActualizacion = :fecha where u.cliente.id = :clienteId")
    int actualizarActivoPorCliente(@Param("clienteId") Integer clienteId,
                                   @Param("activo") boolean activo,
                                   @Param("fecha") OffsetDateTime fecha);
}
