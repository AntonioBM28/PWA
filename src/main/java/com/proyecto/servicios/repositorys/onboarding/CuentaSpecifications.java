package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.enums.EstatusCuenta;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros de busqueda de cuentas. Si el valor es nulo, el criterio no se aplica.
 * clienteId usa ix_cuentas_cliente y estatus usa ix_cuentas_estatus.
 */
public final class CuentaSpecifications {

    private CuentaSpecifications() {
    }

    public static Specification<Cuenta> deCliente(Integer clienteId) {
        return (root, query, cb) -> clienteId == null ? null : cb.equal(root.get("cliente").get("id"), clienteId);
    }

    public static Specification<Cuenta> conEstatus(EstatusCuenta estatus) {
        return (root, query, cb) -> estatus == null ? null : cb.equal(root.get("estatus"), estatus);
    }
}
