package com.proyecto.servicios.exception;

public class CurpDuplicadaException extends ClienteYaRegistradoException {

    public CurpDuplicadaException() {
        super("Ya existe un cliente registrado con la CURP proporcionada");
    }
}
