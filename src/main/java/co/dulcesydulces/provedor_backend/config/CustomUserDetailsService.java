package co.dulcesydulces.provedor_backend.config;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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

    // Normaliza el rol para tolerar datos legacy (minusculas, espacios, alias).
    String rol = normalizeAuthority(u.getRol());
    Set<String> authorities = new LinkedHashSet<>();
    if (!rol.isBlank()) {
      authorities.add(rol);
      addLegacyAliases(authorities, rol);
    }

    // Also expose user-level permissions as Spring authorities.
    if (u.getPermisos() != null) {
      u.getPermisos().stream()
        .filter(p -> p != null && !p.isBlank())
        .map(String::trim)
        .forEach(authorities::add);
    }

    List<SimpleGrantedAuthority> auths = authorities.stream()
      .map(SimpleGrantedAuthority::new)
      .toList();

    boolean enabled = !"inactivo".equalsIgnoreCase(
        u.getEstado_u() == null ? "" : u.getEstado_u().trim());

    return new org.springframework.security.core.userdetails.User(
      u.getCodigo(),
      u.getPassword_hash(),   // aquí va el hash BCrypt ($2a$10$...)
      enabled,
      true,
      true,
      true,
      auths
    );
  }

  private String normalizeAuthority(String rawRole) {
    if (rawRole == null) {
      return "";
    }
    String role = rawRole.trim().toUpperCase(Locale.ROOT);
    if ("PROVEEDOR".equals(role)) {
      return "PROVEEDORES";
    }
    return role;
  }

  private void addLegacyAliases(Set<String> authorities, String role) {
    switch (role) {
      case "ADMINISTRADOR" -> {
        authorities.add("HOME");
        authorities.add("EGRESOS");
        authorities.add("HISTORIAL");
        authorities.add("USUARIOS");
        authorities.add("MODULO_HOME");
        authorities.add("MODULO_EGRESOS");
        authorities.add("MODULO_HISTORIAL");
        authorities.add("MODULO_USUARIOS");
      }
      case "PUBLICADOR", "PROVEEDORES" -> {
        authorities.add("HOME");
        authorities.add("EGRESOS");
        authorities.add("HISTORIAL");
        authorities.add("MODULO_HOME");
        authorities.add("MODULO_EGRESOS");
        authorities.add("MODULO_HISTORIAL");
      }
      case "MODULO_HOME" -> authorities.add("HOME");
      case "MODULO_EGRESOS" -> authorities.add("EGRESOS");
      case "MODULO_HISTORIAL" -> authorities.add("HISTORIAL");
      case "MODULO_USUARIOS" -> authorities.add("USUARIOS");
      case "HOME" -> authorities.add("MODULO_HOME");
      case "EGRESOS" -> authorities.add("MODULO_EGRESOS");
      case "HISTORIAL" -> authorities.add("MODULO_HISTORIAL");
      case "USUARIOS" -> authorities.add("MODULO_USUARIOS");
      default -> {
      }
    }
  }
}
