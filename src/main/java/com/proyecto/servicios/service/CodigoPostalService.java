package com.proyecto.servicios.service;

import com.proyecto.servicios.model.CodigoPostalResponse;

public interface CodigoPostalService {
    CodigoPostalResponse consultarCodigoPostal(String codigoPostal);
}
