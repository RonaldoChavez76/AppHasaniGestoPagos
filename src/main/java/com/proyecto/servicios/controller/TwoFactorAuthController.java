package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.ApiResponse;
import com.proyecto.servicios.model.seguridad.Solicitud2faRequest;
import com.proyecto.servicios.model.seguridad.TwoFactorOtpResponse;
import com.proyecto.servicios.service.seguridad.TwoFactorAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seguridad/2fa")
@Tag(name = "Seguridad - Doble Factor (2FA)", description = "Generación y verificación de códigos OTP de segundo factor para operaciones financieras")
public class TwoFactorAuthController {

    private final TwoFactorAuthService twoFactorAuthService;

    public TwoFactorAuthController(TwoFactorAuthService twoFactorAuthService) {
        this.twoFactorAuthService = twoFactorAuthService;
    }

    @PostMapping("/solicitar")
    @Operation(
            summary = "Solicitar código OTP de segundo factor (2FA)",
            description = "Genera un código OTP de 6 dígitos con expiración de 5 minutos almacenado en Redis para autorizar una transacción o pago de servicio."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Código OTP generado exitosamente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "La cuenta no existe o no está activa")
    })
    public ResponseEntity<ApiResponse<TwoFactorOtpResponse>> solicitarCodigo(
            @Valid @RequestBody Solicitud2faRequest request) {

        TwoFactorOtpResponse respuesta = twoFactorAuthService.solicitarCodigo(request);
        return ResponseEntity.ok(ApiResponse.success(200, respuesta.getMensaje(), respuesta));
    }
}
