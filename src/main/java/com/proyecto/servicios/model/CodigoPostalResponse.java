package com.proyecto.servicios.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CodigoPostalResponse extends GenericResponse {
    private String codigoPostal;
    private String estado;
    private String municipio;
    private List<String> colonias;
}
