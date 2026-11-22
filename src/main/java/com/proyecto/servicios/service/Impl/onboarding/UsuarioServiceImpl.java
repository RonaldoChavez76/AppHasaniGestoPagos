package com.proyecto.servicios.service.Impl.onboarding;

import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.model.onboarding.CambiarPasswordRequest;
import com.proyecto.servicios.model.onboarding.UsuarioResponse;
import com.proyecto.servicios.repositorys.onboarding.UsuarioRepository;
import com.proyecto.servicios.service.onboarding.CredencialesInvalidasException;
import com.proyecto.servicios.service.onboarding.UsuarioInactivoException;
import com.proyecto.servicios.service.onboarding.UsuarioNoEncontradoException;
import com.proyecto.servicios.service.onboarding.UsuarioService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private static final int LONGITUD_MAXIMA_BCRYPT_BYTES = 72;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager", readOnly = true)
    public UsuarioResponse obtenerPorId(Long id) {
        Usuario usuario = cargarUsuario(id);
        return respuesta(usuario);
    }

    @Override
    @Transactional(transactionManager = "sfTransactionManager")
    public void cambiarPassword(Long id, CambiarPasswordRequest request) {
        Usuario usuario = cargarUsuario(id);
        if (!usuario.isActivo() || !usuario.getCliente().isActivo()) {
            throw new UsuarioInactivoException();
        }
        if (request.passwordActual().getBytes(StandardCharsets.UTF_8).length > LONGITUD_MAXIMA_BCRYPT_BYTES) {
            throw new CredencialesInvalidasException();
        }
        if (!passwordEncoder.matches(request.passwordActual(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }
        if (request.passwordNueva().getBytes(StandardCharsets.UTF_8).length > LONGITUD_MAXIMA_BCRYPT_BYTES) {
            throw new IllegalArgumentException("La contraseña excede el máximo admitido por BCrypt (72 bytes UTF-8).");
        }
        if (passwordEncoder.matches(request.passwordNueva(), usuario.getPasswordHash())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser diferente de la actual.");
        }
        usuario.setPasswordHash(passwordEncoder.encode(request.passwordNueva()));
        usuarioRepository.save(usuario);
    }

    private Usuario cargarUsuario(Long id) {
        return usuarioRepository.findWithClienteById(id).orElseThrow(() -> new UsuarioNoEncontradoException(id));
    }

    private UsuarioResponse respuesta(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getCliente().getId(),
                usuario.getCliente().getCorreo(),
                usuario.isActivo(),
                usuario.getFechaCreacion(),
                usuario.getFechaActualizacion()
        );
    }
}
