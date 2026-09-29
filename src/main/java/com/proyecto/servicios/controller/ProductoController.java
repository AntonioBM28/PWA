package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.ProductosResponse;
import com.proyecto.servicios.service.ProductoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping(value = "/productos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductosResponse> obtenerProductos() {
        return new ResponseEntity<>(productoService.obtenerProductos(), HttpStatus.OK);
    }
}
