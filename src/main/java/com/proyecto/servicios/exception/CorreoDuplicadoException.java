package com.proyecto.servicios.exception;

public class CorreoDuplicadoException extends ClienteYaRegistradoException {

    public CorreoDuplicadoException() {
        super("Ya existe un cliente o usuario registrado con el correo electronico proporcionado");
    }
}
