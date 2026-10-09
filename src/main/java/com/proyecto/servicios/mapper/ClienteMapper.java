package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Domicilio;
import com.proyecto.servicios.model.onboarding.ActualizaClienteRequest;
import com.proyecto.servicios.model.onboarding.ActualizaDomicilioRequest;
import com.proyecto.servicios.model.onboarding.ClienteRequest;
import com.proyecto.servicios.model.onboarding.ClienteResponse;
import com.proyecto.servicios.model.onboarding.DomicilioRequest;
import com.proyecto.servicios.model.onboarding.DomicilioResponse;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ClienteMapper {

    String VACIO_A_NULO = "vacioANulo";

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "domicilio", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    Cliente toEntity(ClienteRequest request);

    /** Estado, municipio y pais se asignan con los datos de la API de codigos postales. */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    @Mapping(target = "municipio", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "pais", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    Domicilio toEntity(DomicilioRequest request);

    /**
     * Copia solo los campos enviados (los nulos se ignoran). CURP y RFC nunca se copian; el
     * estatus (activo) y el domicilio los aplica el servicio.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "curp", ignore = true)
    @Mapping(target = "rfc", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "domicilio", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "segundoNombre", qualifiedByName = VACIO_A_NULO)
    @Mapping(target = "telefonoAlternativo", qualifiedByName = VACIO_A_NULO)
    void actualizar(ActualizaClienteRequest request, @MappingTarget Cliente cliente);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cliente", ignore = true)
    @Mapping(target = "municipio", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "pais", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    @Mapping(target = "numeroInterior", qualifiedByName = VACIO_A_NULO)
    void actualizar(ActualizaDomicilioRequest request, @MappingTarget Domicilio domicilio);

    ClienteResponse toResponse(Cliente cliente);

    List<ClienteResponse> toResponseList(List<Cliente> clientes);

    DomicilioResponse toResponse(Domicilio domicilio);

    /** En los campos opcionales, un valor vacio en la actualizacion elimina el dato. */
    @Named(VACIO_A_NULO)
    default String vacioANulo(String valor) {
        return StringUtils.trimToNull(valor);
    }
}
