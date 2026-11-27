package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Transaccion;
import com.proyecto.servicios.enums.EstadoTransaccion;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.Locale;

/**
 * Filtros del historial de operaciones. Cada metodo devuelve un criterio que se combina con AND;
 * si el valor del filtro es nulo, el criterio no se aplica.
 */
public final class TransaccionSpecifications {

    private static final String CLIENTE_ID = "clienteId";
    private static final char ESCAPE = '\\';

    private TransaccionSpecifications() {
    }

    public static Specification<Transaccion> deCliente(Integer clienteId) {
        return (root, query, cb) -> clienteId == null ? null : cb.equal(root.get(CLIENTE_ID), clienteId);
    }

    public static Specification<Transaccion> conEstado(EstadoTransaccion estado) {
        return (root, query, cb) -> estado == null ? null : cb.equal(root.get("estado"), estado);
    }

    public static Specification<Transaccion> desde(OffsetDateTime desde) {
        return (root, query, cb) -> desde == null ? null
                : cb.greaterThanOrEqualTo(root.<OffsetDateTime>get("fechaCreacion"), desde);
    }

    /** Fecha exclusiva: antes de este instante. */
    public static Specification<Transaccion> antesDe(OffsetDateTime hasta) {
        return (root, query, cb) -> hasta == null ? null
                : cb.lessThan(root.<OffsetDateTime>get("fechaCreacion"), hasta);
    }

    /** Texto contenido en el nombre del producto o del servicio, sin distinguir mayusculas. */
    public static Specification<Transaccion> productoContiene(String texto) {
        return (root, query, cb) -> {
            if (texto == null) {
                return null;
            }
            String patron = contiene(texto);
            return cb.or(
                    cb.like(cb.lower(root.get("producto")), patron, ESCAPE),
                    cb.like(cb.lower(root.get("servicio")), patron, ESCAPE));
        };
    }

    /** Operaciones pagadas con la cuenta indicada. */
    public static Specification<Transaccion> deCuenta(String numeroCuenta) {
        return (root, query, cb) -> {
            if (numeroCuenta == null) {
                return null;
            }
            Subquery<Integer> cuentas = query.subquery(Integer.class);
            var cuenta = cuentas.from(Cuenta.class);
            cuentas.select(cuenta.get("id")).where(cb.equal(cuenta.get("numeroCuenta"), numeroCuenta));
            return root.get("cuentaId").in(cuentas);
        };
    }

    /** Operaciones de clientes cuyo nombre o apellidos contienen el texto. */
    public static Specification<Transaccion> nombreClienteContiene(String nombre) {
        return (root, query, cb) -> {
            if (nombre == null) {
                return null;
            }
            String patron = contiene(nombre);
            Subquery<Integer> clientes = query.subquery(Integer.class);
            var cliente = clientes.from(Cliente.class);
            clientes.select(cliente.get("id")).where(cb.or(
                    cb.like(cb.lower(cliente.get("nombre")), patron, ESCAPE),
                    cb.like(cb.lower(cliente.get("apellidoPaterno")), patron, ESCAPE),
                    cb.like(cb.lower(cliente.get("apellidoMaterno")), patron, ESCAPE)));
            return root.get(CLIENTE_ID).in(clientes);
        };
    }

    /** %texto% con los comodines del usuario escapados. */
    static String contiene(String texto) {
        String escapado = texto.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escapado + "%";
    }
}
