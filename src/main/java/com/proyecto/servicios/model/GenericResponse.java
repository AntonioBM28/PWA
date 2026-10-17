package com.proyecto.servicios.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class GenericResponse {
    private Integer codigo;
    private String mensaje;

    /** Errores de validacion agrupados por campo; se omite en respuestas sin errores de campos. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, List<String>> errores;
}
