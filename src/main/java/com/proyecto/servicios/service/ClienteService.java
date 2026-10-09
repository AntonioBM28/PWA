package com.proyecto.servicios.service;

import com.proyecto.servicios.model.onboarding.ActualizaClienteRequest;
import com.proyecto.servicios.model.onboarding.ClienteDetalleResponse;
import com.proyecto.servicios.model.onboarding.ClienteFiltro;
import com.proyecto.servicios.model.onboarding.ClienteRequest;
import com.proyecto.servicios.model.onboarding.ClientesResponse;
import com.proyecto.servicios.model.onboarding.RegistroClienteResponse;

public interface ClienteService {

    RegistroClienteResponse registrarCliente(ClienteRequest request);

    ClientesResponse consultarClientes(ClienteFiltro filtro);

    ClienteDetalleResponse consultarCliente(Integer id);

    ClienteDetalleResponse actualizarCliente(Integer id, ActualizaClienteRequest request);
}
