package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GestoPagoAuthResponse {

    /** Credencial de acceso a GestoPago: se excluye de toString() para que nunca llegue a un log. */
    @ToString.Exclude
    private String token;

    private String message;

    private Integer status;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private Long expiresIn;
}
