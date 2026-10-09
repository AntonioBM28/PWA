package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.model.onboarding.CuentaResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CuentaMapper {

    /** cliente.id se lee del proxy de Hibernate sin cargar al cliente. */
    @Mapping(target = "clienteId", source = "cliente.id")
    CuentaResponse toResponse(Cuenta cuenta);

    List<CuentaResponse> toResponseList(List<Cuenta> cuentas);
}
