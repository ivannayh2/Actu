package co.dulcesydulces.provedor_backend.config;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import co.dulcesydulces.provedor_backend.domain.entidades.Usuarios;
import co.dulcesydulces.provedor_backend.repository.UsuarioRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final UsuarioRepository usuarioRepository;

  public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
    this.usuarioRepository = usuarioRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    // username = codigo
    Usuarios u = usuarioRepository.findByCodigo(username)
      .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

    // Authority: usa exactamente lo que tienes en BD (ADMINISTRADOR, etc.)
    // Si luego quieres hasRole, usa "ROLE_" + u.getRol()
    List<SimpleGrantedAuthority> auths = List.of(new SimpleGrantedAuthority(u.getRol()));

    return new org.springframework.security.core.userdetails.User(
      u.getCodigo(),
      u.getPassword_hash(),   // aquí va el hash BCrypt ($2a$10$...)
      auths
    );
  }
}
