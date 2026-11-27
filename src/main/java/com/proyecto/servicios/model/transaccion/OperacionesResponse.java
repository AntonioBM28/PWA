package com.proyecto.servicios.model.transaccion;

import com.proyecto.servicios.model.onboarding.RespuestaPaginada;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class OperacionesResponse extends RespuestaPaginada {
    private List<OperacionResponse> operaciones;
}
