package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.onboarding.*;
import com.proyecto.servicios.service.onboarding.AutenticacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/auth", "/api/auth"})
@Tag(name = "Autenticación y Sesiones (Web & Móvil)", description = "Inicio de sesión compartido, doble factor (2FA) y control de sesión única activa entre plataformas")
public class AuthController {

    private final AutenticacionService autenticacionService;

    public AuthController(AutenticacionService autenticacionService) {
        this.autenticacionService = autenticacionService;
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Iniciar sesión con control de sesión única",
            description = "Autentica al usuario y registra su sesión activa en la plataforma (WEB o MOVIL). Si ya tiene una sesión activa en otra plataforma y forzarCierreSesion=false, retorna 409 Conflict."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inicio de sesión exitoso"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Credenciales inválidas"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Ya existe una sesión activa en otra plataforma")
    })
    public ResponseEntity<ApiResponse<LoginResponse>> iniciarSesion(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                200, "Inicio de sesión exitoso.", autenticacionService.iniciarSesion(request)));
    }

    @PostMapping(value = "/login/2fa/solicitar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Paso 1 Login 2FA: Validar credenciales, verificar sesión única y solicitar código OTP",
            description = "Valida correo y contraseña, verifica que no exista otra sesión activa simultánea (o la cierra si forzarCierreSesion=true) y genera un código OTP de 6 dígitos en el servidor."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Credenciales válidas y código 2FA generado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Credenciales inválidas"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Sesión activa detectada en otra plataforma")
    })
    public ResponseEntity<ApiResponse<Login2faChallengeResponse>> solicitarLogin2FA(
            @Valid @RequestBody Login2faSolicitudRequest request) {
        Login2faChallengeResponse response = autenticacionService.solicitarLoginCon2FA(request);
        return ResponseEntity.ok(ApiResponse.success(200, response.mensaje(), response));
    }

    @PostMapping(value = "/login/2fa/verificar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Paso 2 Login 2FA: Verificar código OTP en el servidor y emitir token de sesión única",
            description = "Valida y consume el código OTP de 6 dígitos en el servidor, registra la sesión única activa en la plataforma (WEB o MOVIL) y emite el JWT."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "2FA verificado y sesión iniciada exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Código 2FA inválido o expirado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Sesión activa detectada en otra plataforma")
    })
    public ResponseEntity<ApiResponse<LoginResponse>> verificarLogin2FA(
            @Valid @RequestBody Login2faVerificacionRequest request) {
        LoginResponse response = autenticacionService.verificarLoginCon2FA(request);
        return ResponseEntity.ok(ApiResponse.success(
                200, "Autenticación de doble factor completada. Inicio de sesión exitoso.", response));
    }

    @PostMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Cerrar sesión activa de manera segura",
            description = "Finaliza e invalida la sesión activa del usuario en el servidor para permitir el ingreso desde cualquier otra plataforma."
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Void>> cerrarSesion(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        autenticacionService.cerrarSesion(authorization);
        return ResponseEntity.ok(ApiResponse.success(200, "Sesión cerrada correctamente.", null));
    }

    @GetMapping(value = "/sesion", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Consultar estado de la sesión activa y cuenta del usuario",
            description = "Verifica que la sesión asociada al JWT siga activa y no haya sido reemplazada desde otro dispositivo, devolviendo los datos y saldo actualizado."
    )
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<SesionActivaResponse>> consultarSesion(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        SesionActivaResponse response = autenticacionService.consultarSesionActual(authorization);
        return ResponseEntity.ok(ApiResponse.success(200, "Sesión activa válida.", response));
    }
}
