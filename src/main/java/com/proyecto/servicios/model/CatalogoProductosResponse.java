package com.proyecto.servicios.model;

import com.proyecto.servicios.model.onboarding.RespuestaPaginada;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Pagina de productos del catalogo, mas las opciones disponibles para los filtros (servicios y
 * tipos de servicio existentes en todo el catalogo).
 */
@Getter
@Setter
@NoArgsConstructor
public class CatalogoProductosResponse extends RespuestaPaginada {
    private List<ProductoResponse> productos;
    private List<ServicioCatalogo> servicios;
    private List<Integer> tiposServicio;
}
