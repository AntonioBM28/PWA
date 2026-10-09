package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.model.onboarding.UsuarioResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    /** La contrasena no existe en UsuarioResponse, asi que nunca se copia. */
    @Mapping(target = "clienteId", source = "cliente.id")
    UsuarioResponse toResponse(Usuario usuario);

    List<UsuarioResponse> toResponseList(List<Usuario> usuarios);
}
