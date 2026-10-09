package com.proyecto.servicios.service.Impl.onboarding;

import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.model.onboarding.LoginResponse;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.service.onboarding.AutenticacionService;
import com.proyecto.servicios.service.onboarding.CredencialesInvalidasException;
import com.proyecto.servicios.service.onboarding.JwtTokenService;
import com.proyecto.servicios.service.onboarding.UsuarioInactivoException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AutenticacionServiceImpl implements AutenticacionService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AutenticacionServiceImpl(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager", readOnly = true)
    public LoginResponse iniciarSesion(LoginRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new CredencialesInvalidasException();
        }
        String correo = request.correo().trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository.findByCliente_Correo(correo)
                .orElseThrow(CredencialesInvalidasException::new);

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }
        if (!usuario.isActivo() || !usuario.getCliente().isActivo()) {
            throw new UsuarioInactivoException();
        }

        JwtTokenService.Token token = jwtTokenService.emit(
                usuario.getCliente().getCorreo(),
                usuario.getId(),
                usuario.getCliente().getId()
        );
        return new LoginResponse(token.valor(), "Bearer", token.duracionSegundos(), token.expiraEn());
    }
}
