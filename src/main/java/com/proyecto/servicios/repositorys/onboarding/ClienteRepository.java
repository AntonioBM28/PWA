package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Las busquedas combinables de clientes (nombre, apellidos, CURP, RFC, correo, numero de cuenta,
 * activos y rango de fechas) se arman con ClienteSpecifications. Todas las consultas traen el
 * domicilio en el mismo SELECT (EntityGraph) para evitar N+1.
 */
@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Integer>, JpaSpecificationExecutor<Cliente> {

    String DOMICILIO = "domicilio";

    @Override
    @EntityGraph(attributePaths = DOMICILIO)
    Page<Cliente> findAll(Specification<Cliente> specification, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = DOMICILIO)
    Optional<Cliente> findById(Integer id);

    boolean existsByCurp(String curp);

    boolean existsByRfc(String rfc);

    boolean existsByCorreo(String correo);

    boolean existsByCorreoAndIdNot(String correo, Integer id);
}
