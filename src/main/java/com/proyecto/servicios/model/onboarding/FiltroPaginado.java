package com.proyecto.servicios.model.onboarding;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;

/** Parametros de paginacion comunes a las consultas de listas. */
@Getter
public abstract class FiltroPaginado {

    static final int TAMANIO_DEFAULT = 20;
    static final int TAMANIO_MAXIMO = 100;

    /** Con 100 elementos por pagina, el desplazamiento maximo queda muy por debajo de Integer.MAX_VALUE. */
    static final int PAGINA_MAXIMA = 1_000_000;

    @Schema(description = "Numero de pagina, inicia en 0", defaultValue = "0")
    @Min(value = 0, message = "La pagina no puede ser negativa")
    @Max(value = PAGINA_MAXIMA, message = "La pagina admite maximo 1000000")
    private Integer pagina = 0;

    @Schema(description = "Elementos por pagina (maximo 100)", defaultValue = "20")
    @Min(value = 1, message = "El tamanio de pagina debe ser al menos 1")
    @Max(value = TAMANIO_MAXIMO, message = "El tamanio de pagina admite maximo 100")
    private Integer tamanio = TAMANIO_DEFAULT;

    public void setPagina(Integer pagina) {
        this.pagina = pagina == null ? 0 : pagina;
    }

    public void setTamanio(Integer tamanio) {
        this.tamanio = tamanio == null ? TAMANIO_DEFAULT : tamanio;
    }
}
