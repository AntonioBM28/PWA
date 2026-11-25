package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.Plataforma;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.onboarding.DesafioVerificacionResponse;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.model.onboarding.LoginResponse;
import com.proyecto.servicios.model.onboarding.ReenviarCodigoRequest;
import com.proyecto.servicios.model.onboarding.SesionResponse;
import com.proyecto.servicios.model.onboarding.VerificarCodigoRequest;
import com.proyecto.servicios.security.CookieSesion;
import com.proyecto.servicios.service.AuthService;
import com.proyecto.servicios.service.SesionIniciada;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inicio de sesion en dos pasos (contrasena y codigo por correo), consulta y cierre de sesion.
 */
@RestController
public class AuthController {

    private final AuthService authService;
    private final CookieSesion cookieSesion;

    public AuthController(AuthService authService, CookieSesion cookieSesion) {
        this.authService = authService;
        this.cookieSesion = cookieSesion;
    }

    @Operation(summary = "Iniciar sesion (paso 1 de 2)",
            description = "Valida correo y contrasena y envia un codigo de 6 digitos al correo. Responde 409 si el "
                    + "usuario ya tiene una sesion abierta en otro dispositivo: repetir con cerrarSesionAnterior=true")
    @SecurityRequirements
    @PostMapping(value = "/auth/login", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DesafioVerificacionResponse> iniciarSesion(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(authService.iniciarSesion(request));
    }

    @Operation(summary = "Verificar el codigo (paso 2 de 2)",
            description = "Abre la sesion. MOVIL: devuelve el token para usarlo como Authorization: Bearer (en Swagger, "
                    + "copielo en Authorize). WEB: el token se entrega en una cookie HttpOnly y no aparece en el cuerpo")
    @SecurityRequirements
    @PostMapping(value = "/auth/verificar-codigo", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> verificarCodigo(@Valid @RequestBody VerificarCodigoRequest request) {
        SesionIniciada sesion = authService.verificarCodigo(request);
        ResponseEntity.BodyBuilder respuesta = ResponseEntity.ok();
        if (sesion.plataforma() == Plataforma.WEB) {
            respuesta.header(HttpHeaders.SET_COOKIE, cookieSesion.crear(sesion.token(), sesion.vigencia()).toString());
        }
        return respuesta.body(sesion.respuesta());
    }

    @Operation(summary = "Reenviar el codigo de verificacion",
            description = "Envia un codigo nuevo; el anterior deja de servir. Hay que esperar entre envios y el numero "
                    + "de reenvios es limitado")
    @SecurityRequirements
    @PostMapping(value = "/auth/reenviar-codigo", produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DesafioVerificacionResponse> reenviarCodigo(@Valid @RequestBody ReenviarCodigoRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(authService.reenviarCodigo(request));
    }

    @Operation(summary = "Consultar la sesion actual",
            description = "La usa la app web al recargar la pagina para recuperar la sesion desde la cookie")
    @GetMapping(value = "/auth/sesion", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SesionResponse> consultarSesion(JwtAuthenticationToken autenticacion) {
        return ResponseEntity.ok(authService.consultarSesion(autenticacion));
    }

    @Operation(summary = "Cerrar sesion", description = "El token deja de funcionar de inmediato en el servidor")
    @PostMapping(value = "/auth/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> cerrarSesion(JwtAuthenticationToken autenticacion) {
        authService.cerrarSesion(autenticacion);
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(0);
        respuesta.setMensaje("Sesion cerrada");
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieSesion.borrar().toString())
                .body(respuesta);
    }
}
