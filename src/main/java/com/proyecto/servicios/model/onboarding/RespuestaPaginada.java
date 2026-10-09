package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.model.GenericResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

/** Datos de paginacion comunes a las respuestas de listas. */
@Getter
@Setter
@NoArgsConstructor
public abstract class RespuestaPaginada extends GenericResponse {
    private int pagina;
    private int tamanio;
    private long totalElementos;
    private int totalPaginas;

    public void asignarPaginacion(Page<?> page) {
        pagina = page.getNumber();
        tamanio = page.getSize();
        totalElementos = page.getTotalElements();
        totalPaginas = page.getTotalPages();
    }
}
