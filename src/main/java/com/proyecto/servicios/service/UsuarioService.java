package com.proyecto.servicios.service;

import com.proyecto.servicios.model.onboarding.AgregaUsuarioRequest;
import com.proyecto.servicios.model.onboarding.UsuarioDetalleResponse;
import com.proyecto.servicios.model.onboarding.UsuarioFiltro;
import com.proyecto.servicios.model.onboarding.UsuariosResponse;

public interface UsuarioService {

    UsuariosResponse consultarUsuarios(UsuarioFiltro filtro);

    UsuarioDetalleResponse agregarUsuario(AgregaUsuarioRequest request);
}
