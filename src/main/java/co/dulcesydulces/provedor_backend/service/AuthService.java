package co.dulcesydulces.provedor_backend.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import co.dulcesydulces.provedor_backend.domain.dto.LoginRequest;
import co.dulcesydulces.provedor_backend.domain.dto.LoginResponse;
import co.dulcesydulces.provedor_backend.domain.entidades.Usuarios;
import co.dulcesydulces.provedor_backend.repository.UsuarioRepository;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder encoder;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder encoder) {
        this.usuarioRepository = usuarioRepository;
        this.encoder = encoder;
    }

    public Usuarios autenticar(String codigo, String password_hash) {
        Usuarios user = usuarioRepository.findByCodigo(codigo)
                .orElseThrow(() -> new RuntimeException("Usuario no existe"));

        // Verifica que el usuario esté activo
        if (!"activo".equals(user.getEstado_u())) {
            throw new RuntimeException("Usuario desactivado");
        }

        if (!encoder.matches(password_hash, user.getPassword_hash())) {
            throw new RuntimeException("Clave incorrecta");
        }

        return user;
    }

    public LoginResponse login(LoginRequest req) {
    try {
        autenticar(req.getCodigo(), req.getPasswor_hash());
        return new LoginResponse();
    } catch (RuntimeException ex) {
        return new LoginResponse();
    }
  }

}
