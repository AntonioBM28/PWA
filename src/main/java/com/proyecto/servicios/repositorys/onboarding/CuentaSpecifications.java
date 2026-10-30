package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.enums.EstatusCuenta;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros de busqueda de cuentas. Si el valor es nulo, el criterio no se aplica.
 * correoCliente identifica al propietario mediante su correo unico; estatus usa ix_cuentas_estatus.
 */
public final class CuentaSpecifications {

    private CuentaSpecifications() {
    }

    public static Specification<Cuenta> conNumeroCuenta(String numeroCuenta) {
        return (root, query, cb) -> numeroCuenta == null ? null
                : cb.equal(root.get("numeroCuenta"), numeroCuenta);
    }

    public static Specification<Cuenta> deCorreoCliente(String correoCliente) {
        return (root, query, cb) -> correoCliente == null ? null
                : cb.equal(cb.lower(root.get("cliente").get("correo")), correoCliente.toLowerCase());
    }

    public static Specification<Cuenta> conEstatus(EstatusCuenta estatus) {
        return (root, query, cb) -> estatus == null ? null : cb.equal(root.get("estatus"), estatus);
    }
}
