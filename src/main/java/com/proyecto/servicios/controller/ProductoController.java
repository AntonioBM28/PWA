package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.CatalogoProductosResponse;
import com.proyecto.servicios.model.ProductoFiltro;
import com.proyecto.servicios.model.ProductosResponse;
import com.proyecto.servicios.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Catalogo de productos y servicios de GestoPago (solo lectura). */
@RestController
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @SecurityRequirements
    @GetMapping(value = "/productos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductosResponse> obtenerProductos() {
        return new ResponseEntity<>(productoService.obtenerProductos(), HttpStatus.OK);
    }

    @Operation(summary = "Consultar el catalogo con filtros",
            description = "Busqueda paginada y de solo lectura. Filtros combinables: texto (producto o servicio), "
                    + "servicio, tipo de servicio y rango de precio. Incluye la imagen de cada servicio y las "
                    + "opciones disponibles para los filtros")
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/productos/consulta", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CatalogoProductosResponse> consultarProductos(@Valid @RequestBody ProductoFiltro filtro) {
        return new ResponseEntity<>(productoService.consultarProductos(filtro), HttpStatus.OK);
    }
}
