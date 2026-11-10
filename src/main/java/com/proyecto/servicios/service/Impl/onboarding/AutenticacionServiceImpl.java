package com.proyecto.servicios.service.Impl.onboarding;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.entity.seguridad.SesionUsuario;
import com.proyecto.servicios.model.onboarding.*;
import com.proyecto.servicios.model.seguridad.Solicitud2faRequest;
import com.proyecto.servicios.model.seguridad.TwoFactorOtpResponse;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.service.onboarding.*;
import com.proyecto.servicios.service.seguridad.SesionUsuarioService;
import com.proyecto.servicios.service.seguridad.TwoFactorAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class AutenticacionServiceImpl implements AutenticacionService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final SesionUsuarioService sesionUsuarioService;
    private final TwoFactorAuthService twoFactorAuthService;
    private final CuentaRepository cuentaRepository;

    @Autowired
    public AutenticacionServiceImpl(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            @Autowired(required = false) SesionUsuarioService sesionUsuarioService,
            @Autowired(required = false) TwoFactorAuthService twoFactorAuthService,
            @Autowired(required = false) CuentaRepository cuentaRepository
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.sesionUsuarioService = sesionUsuarioService;
        this.twoFactorAuthService = twoFactorAuthService;
        this.cuentaRepository = cuentaRepository;
    }

    public AutenticacionServiceImpl(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService
    ) {
        this(usuarioRepository, passwordEncoder, jwtTokenService, null, null, null);
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public LoginResponse iniciarSesion(LoginRequest request) {
        Usuario usuario = validarCredencialesYEstado(request.correo(), request.password());
        Cliente cliente = usuario.getCliente();
        Optional<Cuenta> cuentaActiva = obtenerCuentaActiva(cliente.getId());

        if (request.codigo2FA() != null && !request.codigo2FA().isBlank() && twoFactorAuthService != null) {
            String cuentaOtp = cuentaActiva.map(Cuenta::getNumeroCuenta)
                    .orElseThrow(() -> new IllegalArgumentException("El cliente no posee una cuenta activa para validar 2FA."));
            twoFactorAuthService.validarYConsumirCodigo(cuentaOtp, request.codigo2FA());
        }

        if (sesionUsuarioService == null) {
            JwtTokenService.Token token = jwtTokenService.emit(
                    cliente.getCorreo(),
                    usuario.getId(),
                    cliente.getId()
            );
            return new LoginResponse(token.valor(), "Bearer", token.duracionSegundos(), token.expiraEn());
        }

        return emitirTokenYRegistrarSesion(
                usuario,
                cliente,
                cuentaActiva.orElse(null),
                request.plataforma(),
                Boolean.TRUE.equals(request.forzarCierreSesion())
        );
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public Login2faChallengeResponse solicitarLoginCon2FA(Login2faSolicitudRequest request) {
        Usuario usuario = validarCredencialesYEstado(request.correo(), request.password());
        Cliente cliente = usuario.getCliente();

        if (sesionUsuarioService != null) {
            sesionUsuarioService.verificarDisponibilidadOCerrar(
                    usuario.getId(),
                    request.plataforma(),
                    Boolean.TRUE.equals(request.forzarCierreSesion())
            );
        }

        Cuenta cuenta = obtenerCuentaActiva(cliente.getId())
                .orElseThrow(() -> new IllegalArgumentException("El cliente no tiene una cuenta activa registrada."));

        if (twoFactorAuthService == null) {
            throw new IllegalStateException("El servicio de autenticación de doble factor (2FA) no está disponible.");
        }

        TwoFactorOtpResponse otpResponse = twoFactorAuthService.solicitarCodigo(
                Solicitud2faRequest.builder().numeroCuenta(cuenta.getNumeroCuenta()).build()
        );

        return new Login2faChallengeResponse(
                true,
                cliente.getCorreo(),
                cuenta.getNumeroCuenta(),
                request.plataforma(),
                otpResponse.getCodigoOtp(),
                otpResponse.getExpiraEnSegundos(),
                "Credenciales válidas. Ingrese el código de verificación de doble factor (2FA) de 6 dígitos para iniciar sesión."
        );
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public LoginResponse verificarLoginCon2FA(Login2faVerificacionRequest request) {
        Usuario usuario = validarCredencialesYEstado(request.correo(), request.password());
        Cliente cliente = usuario.getCliente();

        Cuenta cuenta = obtenerCuentaActiva(cliente.getId())
                .orElseThrow(() -> new IllegalArgumentException("El cliente no tiene una cuenta activa registrada."));

        if (twoFactorAuthService == null) {
            throw new IllegalStateException("El servicio de autenticación de doble factor (2FA) no está disponible.");
        }

        // Validar y consumir el código OTP desde el servidor (PostgreSQL)
        twoFactorAuthService.validarYConsumirCodigo(cuenta.getNumeroCuenta(), request.codigo2FA());

        return emitirTokenYRegistrarSesion(
                usuario,
                cliente,
                cuenta,
                request.plataforma(),
                Boolean.TRUE.equals(request.forzarCierreSesion())
        );
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public void cerrarSesion(String authorizationHeader) {
        JwtTokenService.AuthenticatedUser user = extraerUsuarioAutenticado(authorizationHeader);
        if (sesionUsuarioService != null) {
            sesionUsuarioService.cerrarSesion(user.usuarioId(), user.sessionId());
        }
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public SesionActivaResponse consultarSesionActual(String authorizationHeader) {
        JwtTokenService.AuthenticatedUser user = extraerUsuarioAutenticado(authorizationHeader);

        if (sesionUsuarioService != null && user.sessionId() != null) {
            if (!sesionUsuarioService.esSesionValida(user.usuarioId(), user.sessionId())) {
                throw new SesionInvalidaException();
            }
        }

        Usuario usuario = usuarioRepository.findById(user.usuarioId())
                .orElseThrow(CredencialesInvalidasException::new);
        if (!usuario.isActivo() || !usuario.getCliente().isActivo()) {
            throw new UsuarioInactivoException();
        }

        Cliente cliente = usuario.getCliente();
        Optional<Cuenta> cuentaOpt = obtenerCuentaActiva(cliente.getId());
        Optional<SesionUsuario> sesionOpt = (sesionUsuarioService != null && user.sessionId() != null)
                ? sesionUsuarioService.buscarPorSessionId(user.sessionId())
                : Optional.empty();

        return new SesionActivaResponse(
                true,
                user.sessionId(),
                sesionOpt.map(SesionUsuario::getPlataforma).orElse(user.plataforma() != null ? user.plataforma() : "WEB"),
                usuario.getId(),
                cliente.getId(),
                construirNombreCompleto(cliente),
                cliente.getCorreo(),
                cuentaOpt.map(Cuenta::getNumeroCuenta).orElse(null),
                cuentaOpt.map(Cuenta::getSaldo).orElse(BigDecimal.ZERO),
                sesionOpt.map(SesionUsuario::getFechaInicio).orElse(null),
                sesionOpt.map(SesionUsuario::getFechaExpiracion).orElse(null)
        );
    }

    private Usuario validarCredencialesYEstado(String correoInput, String password) {
        if (password == null || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new CredencialesInvalidasException();
        }
        String correo = correoInput.trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository.findByCliente_Correo(correo)
                .orElseThrow(CredencialesInvalidasException::new);

        if (!passwordEncoder.matches(password, usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }
        if (!usuario.isActivo() || !usuario.getCliente().isActivo()) {
            throw new UsuarioInactivoException();
        }
        return usuario;
    }

    private LoginResponse emitirTokenYRegistrarSesion(
            Usuario usuario,
            Cliente cliente,
            Cuenta cuenta,
            String plataforma,
            boolean forzarCierreSesion
    ) {
        String sessionId = UUID.randomUUID().toString();
        String plataformaNormalizada = (plataforma == null || plataforma.isBlank())
                ? "WEB"
                : plataforma.trim().toUpperCase(Locale.ROOT);

        JwtTokenService.Token token = jwtTokenService.emit(
                cliente.getCorreo(),
                usuario.getId(),
                cliente.getId(),
                sessionId,
                plataformaNormalizada
        );

        if (sesionUsuarioService != null) {
            sesionUsuarioService.registrarNuevaSesion(
                    usuario.getId(),
                    cliente.getId(),
                    sessionId,
                    plataformaNormalizada,
                    token.duracionSegundos(),
                    forzarCierreSesion
            );
        }

        return new LoginResponse(
                token.valor(),
                "Bearer",
                token.duracionSegundos(),
                token.expiraEn(),
                sessionId,
                plataformaNormalizada,
                cliente.getId(),
                construirNombreCompleto(cliente),
                cliente.getCorreo(),
                cuenta != null ? cuenta.getNumeroCuenta() : null,
                cuenta != null ? cuenta.getSaldo() : null
        );
    }

    private Optional<Cuenta> obtenerCuentaActiva(Long clienteId) {
        if (cuentaRepository == null || clienteId == null) {
            return Optional.empty();
        }
        return cuentaRepository.findFirstByClienteIdAndEstatusIgnoreCaseOrderByIdAsc(clienteId, "ACTIVA");
    }

    private JwtTokenService.AuthenticatedUser extraerUsuarioAutenticado(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new SesionInvalidaException("Se requiere un token Bearer válido.");
        }
        String token = authorizationHeader.substring(7).trim();
        if (token.isEmpty()) {
            throw new SesionInvalidaException("Se requiere un token Bearer válido.");
        }
        return jwtTokenService.validar(token)
                .orElseThrow(() -> new SesionInvalidaException("El token es inválido o expiró."));
    }

    private String construirNombreCompleto(Cliente cliente) {
        return Stream.of(
                        cliente.getPrimerNombre(),
                        cliente.getSegundoNombre(),
                        cliente.getApellidoPaterno(),
                        cliente.getApellidoMaterno()
                )
                .filter(parte -> parte != null && !parte.isBlank())
                .collect(Collectors.joining(" "));
    }
}
