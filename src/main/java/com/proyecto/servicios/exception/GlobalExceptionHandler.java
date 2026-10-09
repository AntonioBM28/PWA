package com.proyecto.servicios.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.proyecto.servicios.model.GenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones: atrapa las excepciones que lanzan los controllers y servicios
 * de toda la aplicacion y las convierte en GenericResponse con el estatus HTTP correspondiente.
 * Asi los controllers y servicios no necesitan try/catch para armar respuestas de error: solo
 * lanzan la excepcion que describe el problema.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    static final int CODIGO_ERROR = 1;
    static final String SEPARADOR = "; ";
    static final String MENSAJE_ERROR_INTERNO = "Ocurrio un error inesperado, intente mas tarde";
    static final String MENSAJE_ACCESO_DENEGADO = "No tiene permiso para realizar esta operacion o acceder a estos datos";

    // ===================================================================== Negocio

    /** Reglas de negocio del onboarding; cada subclase define su estatus HTTP. */
    @ExceptionHandler(OnboardingException.class)
    public ResponseEntity<GenericResponse> handleOnboarding(OnboardingException ex) {
        return error(ex.getMessage(), ex.getHttpStatus());
    }

    /** 429 con el header Retry-After, para que el cliente sepa cuanto esperar. */
    @ExceptionHandler(DemasiadosIntentosException.class)
    public ResponseEntity<GenericResponse> handleDemasiadosIntentos(DemasiadosIntentosException ex) {
        GenericResponse response = new GenericResponse();
        response.setCodigo(CODIGO_ERROR);
        response.setMensaje(ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getSegundosParaReintentar()))
                .body(response);
    }

    // ===================================================================== Autorizacion

    /**
     * @PreAuthorize nego el acceso: el usuario esta autenticado pero su rol no lo permite, o intenta
     * acceder a datos de otro cliente.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<GenericResponse> handleAccesoDenegado(AccessDeniedException ex) {
        return error(MENSAJE_ACCESO_DENEGADO, HttpStatus.FORBIDDEN);
    }

    // ===================================================================== Servicios externos

    @ExceptionHandler(CodigoPostalException.class)
    public ResponseEntity<GenericResponse> handleCodigoPostal(CodigoPostalException ex) {
        return error(ex.getMessage(), ex.getHttpStatus());
    }

    @ExceptionHandler(GestoPagoIntegrationException.class)
    public ResponseEntity<GenericResponse> handleGestoPago(GestoPagoIntegrationException ex) {
        return error(ex.getMessage(), ex.getHttpStatus());
    }

    @ExceptionHandler(CatalogoProductosException.class)
    public ResponseEntity<GenericResponse> handleCatalogo(CatalogoProductosException ex) {
        return error(ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
    }

    // ===================================================================== Validaciones de entrada

    /**
     * Errores de Bean Validation en el cuerpo (@Valid @RequestBody) o en los filtros de consulta
     * (@Valid @ParameterObject). Si un filtro no se pudo convertir (por ejemplo una fecha "ayer"), el
     * mensaje de Spring menciona clases internas, por eso se reemplaza por uno propio.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse> handleValidacionCuerpo(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(error -> error.isBindingFailure()
                        ? "Valor '" + error.getRejectedValue() + "' no valido para el parametro " + error.getField()
                        : error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining(SEPARADOR));
        return error(mensaje, HttpStatus.BAD_REQUEST);
    }

    /** Errores de Bean Validation en parametros (@PathVariable, @RequestParam). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<GenericResponse> handleValidacionParametros(HandlerMethodValidationException ex) {
        String mensaje = ex.getAllErrors().stream()
                .map(MessageSourceResolvable::getDefaultMessage)
                .distinct()
                .collect(Collectors.joining(SEPARADOR));
        return error(mensaje, HttpStatus.BAD_REQUEST);
    }

    /** Parametro de ruta o de consulta con un tipo invalido, por ejemplo /clientes/abc. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GenericResponse> handleTipoParametro(MethodArgumentTypeMismatchException ex) {
        return error("Valor '" + ex.getValue() + "' no valido para el parametro " + ex.getName(), HttpStatus.BAD_REQUEST);
    }

    /** JSON mal formado o con valores que no corresponden al tipo (fechas, enums, numeros). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GenericResponse> handleCuerpoIlegible(HttpMessageNotReadableException ex) {
        CuerpoDemasiadoGrandeException demasiadoGrande =
                ExceptionUtils.throwableOfType(ex, CuerpoDemasiadoGrandeException.class);
        if (demasiadoGrande != null) {
            return error(demasiadoGrande.getMessage(), HttpStatus.PAYLOAD_TOO_LARGE);
        }
        String mensaje = "El cuerpo de la peticion no es un JSON valido";
        if (ex.getCause() instanceof InvalidFormatException formato) {
            mensaje = "Valor '" + formato.getValue() + "' no valido para el campo " + campo(formato);
            if (formato.getTargetType().isEnum()) {
                mensaje += ". Valores permitidos: " + Arrays.toString(formato.getTargetType().getEnumConstants());
            }
        }
        return error(mensaje, HttpStatus.BAD_REQUEST);
    }

    // ===================================================================== Ultimas barreras

    /** Una restriccion de la base de datos rechazo la operacion. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<GenericResponse> handleIntegridad(DataIntegrityViolationException ex) {
        log.error("Restriccion de base de datos violada: {}", ex.getMostSpecificCause().getClass().getSimpleName());
        return error("La informacion no cumple las reglas de la base de datos", HttpStatus.CONFLICT);
    }

    /**
     * Cualquier otra excepcion. Las de Spring MVC (ruta inexistente, metodo no permitido, etc.)
     * conservan su estatus con un mensaje propio en espanol, sin detalles internos; las inesperadas
     * responden 500 con un mensaje generico y el detalle solo se registra en el log.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse> handleGeneral(Exception ex) {
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatus status = HttpStatus.valueOf(errorResponse.getStatusCode().value());
            return error(mensajeDeEstatus(status), status);
        }
        log.error("Error no controlado", ex);
        return error(MENSAJE_ERROR_INTERNO, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    static String mensajeDeEstatus(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "El recurso solicitado no existe";
            case METHOD_NOT_ALLOWED -> "El metodo HTTP no esta permitido en esta ruta";
            case UNSUPPORTED_MEDIA_TYPE -> "Tipo de contenido no soportado; envie application/json";
            case NOT_ACCEPTABLE -> "Formato de respuesta no soportado; use application/json";
            case PAYLOAD_TOO_LARGE -> "El cuerpo de la peticion es demasiado grande";
            case BAD_REQUEST -> "La peticion es invalida o le faltan datos";
            default -> status.is5xxServerError() ? MENSAJE_ERROR_INTERNO : "No fue posible procesar la peticion";
        };
    }

    private static String campo(JsonMappingException ex) {
        return ex.getPath().stream()
                .map(referencia -> referencia.getFieldName() != null ? referencia.getFieldName() : "[" + referencia.getIndex() + "]")
                .collect(Collectors.joining("."));
    }

    private static ResponseEntity<GenericResponse> error(String mensaje, HttpStatus status) {
        GenericResponse response = new GenericResponse();
        response.setCodigo(CODIGO_ERROR);
        response.setMensaje(mensaje);
        return new ResponseEntity<>(response, status);
    }
}
