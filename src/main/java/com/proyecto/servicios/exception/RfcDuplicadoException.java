package com.proyecto.servicios.exception;

public class RfcDuplicadoException extends ClienteYaRegistradoException {

    public RfcDuplicadoException() {
        super("Ya existe un cliente registrado con el RFC proporcionado");
    }
}
