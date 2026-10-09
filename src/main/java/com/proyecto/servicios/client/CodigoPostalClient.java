package com.proyecto.servicios.client;

import com.proyecto.servicios.config.CodigoPostalFeignConfig;
import com.proyecto.servicios.model.codigopostal.CodigoPostalApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "codigoPostal", url = "${codigo-postal.url}", configuration = CodigoPostalFeignConfig.class)
public interface CodigoPostalClient {

    @GetMapping("/cp/{codigoPostal}")
    CodigoPostalApiResponse consultar(@PathVariable("codigoPostal") String codigoPostal);
}
