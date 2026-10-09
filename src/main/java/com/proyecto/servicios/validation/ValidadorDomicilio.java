package com.proyecto.servicios.validation;

import com.proyecto.servicios.entity.onboarding.Domicilio;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.CodigoPostalResponse;
import com.proyecto.servicios.service.CodigoPostalService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Valida el domicilio contra la API de codigos postales y lo completa con sus datos: estado y
 * municipio se toman de la API (no del request) y la colonia se guarda con el nombre exacto que
 * devuelve la API. La colonia se compara sin distinguir mayusculas, acentos ni espacios extra.
 */
@Component
public class ValidadorDomicilio {

    private final CodigoPostalService codigoPostalService;

    public ValidadorDomicilio(CodigoPostalService codigoPostalService) {
        this.codigoPostalService = codigoPostalService;
    }

    public void completar(Domicilio domicilio) {
        CodigoPostalResponse codigoPostal = codigoPostalService.consultarCodigoPostal(domicilio.getCodigoPostal());
        String colonia = codigoPostal.getColonias().stream()
                .filter(nombre -> normalizar(nombre).equals(normalizar(domicilio.getColonia())))
                .findFirst()
                .orElseThrow(() -> new ValidacionException("La colonia '" + domicilio.getColonia()
                        + "' no pertenece al codigo postal " + domicilio.getCodigoPostal()));
        domicilio.setColonia(colonia);
        domicilio.setMunicipio(codigoPostal.getMunicipio());
        domicilio.setEstado(codigoPostal.getEstado());
        domicilio.setPais(Domicilio.PAIS_MEXICO);
    }

    private static String normalizar(String texto) {
        String sinAcentos = Normalizer.normalize(StringUtils.defaultString(texto), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return StringUtils.normalizeSpace(sinAcentos).toLowerCase(Locale.ROOT);
    }
}
