package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;

/**
 * Filtros de busqueda de clientes. Cada metodo devuelve un criterio que se combina con AND; si el
 * valor del filtro es nulo, el criterio no se aplica.
 * <p>
 * Las busquedas por nombre son por prefijo sin distinguir mayusculas y usan lower(...) LIKE 'x%'
 * para aprovechar los indices ix_clientes_nombre / ix_clientes_apellido_* (text_pattern_ops).
 */
public final class ClienteSpecifications {

    private ClienteSpecifications() {
    }

    public static Specification<Cliente> nombreEmpiezaCon(String nombre) {
        return empiezaCon("nombre", nombre);
    }

    public static Specification<Cliente> apellidoPaternoEmpiezaCon(String apellidoPaterno) {
        return empiezaCon("apellidoPaterno", apellidoPaterno);
    }

    public static Specification<Cliente> apellidoMaternoEmpiezaCon(String apellidoMaterno) {
        return empiezaCon("apellidoMaterno", apellidoMaterno);
    }

    public static Specification<Cliente> curpIgual(String curp) {
        return igual("curp", curp);
    }

    public static Specification<Cliente> rfcIgual(String rfc) {
        return igual("rfc", rfc);
    }

    public static Specification<Cliente> correoIgual(String correo) {
        return igual("correo", correo);
    }

    public static Specification<Cliente> activo(Boolean activo) {
        return igual("activo", activo);
    }

    /** Clientes que tienen la cuenta indicada (EXISTS sobre cuentas, usa uq_cuentas_numero_cuenta). */
    public static Specification<Cliente> tieneCuenta(String numeroCuenta) {
        return (root, query, cb) -> {
            if (numeroCuenta == null) {
                return null;
            }
            Subquery<Integer> cuenta = query.subquery(Integer.class);
            var cuentaRoot = cuenta.from(Cuenta.class);
            cuenta.select(cuentaRoot.get("id"))
                    .where(cb.equal(cuentaRoot.get("cliente"), root),
                            cb.equal(cuentaRoot.get("numeroCuenta"), numeroCuenta));
            return cb.exists(cuenta);
        };
    }

    /** Registrados desde la fecha indicada, inclusive (usa ix_clientes_fecha_creacion). */
    public static Specification<Cliente> registradoDesde(OffsetDateTime desde) {
        return (root, query, cb) -> desde == null ? null
                : cb.greaterThanOrEqualTo(root.get("fechaCreacion"), desde);
    }

    /** Registrados antes de la fecha indicada, exclusive. */
    public static Specification<Cliente> registradoAntesDe(OffsetDateTime hasta) {
        return (root, query, cb) -> hasta == null ? null
                : cb.lessThan(root.get("fechaCreacion"), hasta);
    }

    private static Specification<Cliente> empiezaCon(String atributo, String valor) {
        return (root, query, cb) -> valor == null ? null
                : cb.like(cb.lower(root.get(atributo)), valor.toLowerCase() + "%");
    }

    private static Specification<Cliente> igual(String atributo, Object valor) {
        return (root, query, cb) -> valor == null ? null : cb.equal(root.get(atributo), valor);
    }
}
