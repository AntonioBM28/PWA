package com.proyecto.servicios.model.gestopago;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@XmlRootElement(name = "RESPONSE")
@XmlAccessorType(XmlAccessType.FIELD)
public class GestoPagoTransaccionResponse {
    @XmlElement(name = "ID_TX")
    private String idTx;
    @XmlElement(name = "NUM_AUTORIZACION")
    private String numeroAutorizacion;
    @XmlElement(name = "MONTO")
    private BigDecimal monto;
    @XmlElement(name = "PIN")
    private String pin;
    @XmlElement(name = "FOLIO")
    private String folio;
    @XmlElement(name = "legend")
    private String leyenda;
    @XmlElement(name = "MENSAJE")
    private GestoPagoTransaccionMensaje mensaje;

    @Getter
    @Setter
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class GestoPagoTransaccionMensaje {
        @XmlElement(name = "CODIGO")
        private String codigo;
        @XmlElement(name = "TEXTO")
        private String texto;
        @XmlElement(name = "ID_TX")
        private String idTx;
    }
}
