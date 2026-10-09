package com.proyecto.servicios.repositorys.onboarding;

import com.proyecto.servicios.entity.onboarding.Usuario;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros de busqueda de usuarios. Si el valor es nulo, el criterio no se aplica.
 */
public final class UsuarioSpecifications {

    private UsuarioSpecifications() {
    }

    private static final char ESCAPE = '\\';

    /**
     * Correo o inicio del correo (los correos se guardan en minusculas). '_' y '%' son validos en
     * un correo, por eso se escapan para que LIKE no los trate como comodines.
     */
    public static Specification<Usuario> correoEmpiezaCon(String correo) {
        return (root, query, cb) -> correo == null ? null
                : cb.like(root.get("correo"), escaparLike(correo) + "%", ESCAPE);
    }

    public static Specification<Usuario> activo(Boolean activo) {
        return (root, query, cb) -> activo == null ? null : cb.equal(root.get("activo"), activo);
    }

    public static Specification<Usuario> deCliente(Integer clienteId) {
        return (root, query, cb) -> clienteId == null ? null : cb.equal(root.get("cliente").get("id"), clienteId);
    }

    static String escaparLike(String valor) {
        return valor.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
