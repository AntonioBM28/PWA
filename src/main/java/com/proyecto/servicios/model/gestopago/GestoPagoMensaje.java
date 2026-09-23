package com.proyecto.servicios.model.gestopago;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Data;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class GestoPagoMensaje {

    public static final String CODIGO_EXITO = "01";

    @XmlElement(name = "CODIGO")
    private String codigo;

    @XmlElement(name = "TEXTO")
    private String texto;

    public boolean isExitoso() {
        return CODIGO_EXITO.equals(codigo);
    }
}
