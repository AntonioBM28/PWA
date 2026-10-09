package com.proyecto.servicios.model.codigopostal;

import lombok.Data;

import java.util.List;

/**
 * Respuesta de la API de codigos postales (postali.app). Solo se mapean los campos que usa
 * la aplicacion; los demas se ignoran.
 */
@Data
public class CodigoPostalApiResponse {
    private String cp;
    private String estado;
    private String municipio;
    private List<AsentamientoApi> asentamientos;
}
