package com.proyecto.servicios.model.gestopago;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import lombok.Data;

import java.math.BigDecimal;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class GestoPagoProducto {

    @XmlAttribute
    private Integer idProducto;

    @XmlAttribute
    private Integer idServicio;

    @XmlAttribute
    private Integer idCatTipoServicio;

    @XmlAttribute
    private String producto;

    @XmlAttribute
    private String servicio;

    @XmlAttribute
    private BigDecimal precio;

    @XmlAttribute
    private Integer tipoFront;

    @XmlAttribute
    private Boolean hasDigitoVerificador;

    @XmlAttribute
    private Boolean showAyuda;

    @XmlAttribute
    private String tipoReferencia;

    @XmlElement
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    private String legend;
}
