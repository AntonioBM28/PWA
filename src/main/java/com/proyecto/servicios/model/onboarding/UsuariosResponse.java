package com.proyecto.servicios.model.onboarding;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class UsuariosResponse extends RespuestaPaginada {
    private List<UsuarioResponse> usuarios;
}
