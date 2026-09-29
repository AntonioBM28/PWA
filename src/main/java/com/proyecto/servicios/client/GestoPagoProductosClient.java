package com.proyecto.servicios.client;

import com.proyecto.servicios.config.GestoPagoProductosFeignConfig;
import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "gestoPagoProductos", url = "${gestopago.productos.url}",
        configuration = GestoPagoProductosFeignConfig.class)
public interface GestoPagoProductosClient {

    @GetMapping(value = "/sistema/service/getProductList.do", produces = MediaType.APPLICATION_XML_VALUE)
    GestoPagoProductListResponse getProductList(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization);
}
