package co.dulcesydulces.provedor_backend.config;

import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import co.dulcesydulces.provedor_backend.repository.UsuarioModuloRepo;
import co.dulcesydulces.provedor_backend.repository.UsuarioRepository;
import java.util.Optional;

@ControllerAdvice
public class GlobalModel {

  private final UsuarioModuloRepo repo;
  private final UsuarioRepository usuarioRepository;

  public GlobalModel(UsuarioModuloRepo repo, UsuarioRepository usuarioRepository) {
    this.repo = repo;
    this.usuarioRepository = usuarioRepository;
  }

  @ModelAttribute("modulosPermitidos")
  public Set<String> modulosPermitidos(Authentication auth) {

    if (auth == null || auth.getName() == null) {
      return Set.of();
    }

    String codigo = auth.getName();
    return new HashSet<>(repo.codigosPermitidos(codigo));
  }


  @ModelAttribute("codigo")
  public String codigo(Authentication auth) {
    return (auth != null) ? auth.getName() : null;
  }

  @ModelAttribute("nombreUsuario")
  public String nombreUsuario(Authentication auth) {
    if (auth == null || auth.getName() == null) return "Usuario";
    Optional<co.dulcesydulces.provedor_backend.domain.entidades.Usuarios> u = usuarioRepository.findByCodigo(auth.getName());
    return u.map(co.dulcesydulces.provedor_backend.domain.entidades.Usuarios::getNombreUsuario).orElse("Usuario");
  }

  @ModelAttribute("fotoPerfil")
  public String fotoPerfil(Authentication auth) {
    if (auth == null || auth.getName() == null) return null;
    Optional<co.dulcesydulces.provedor_backend.domain.entidades.Usuarios> u = usuarioRepository.findByCodigo(auth.getName());
    return u.map(co.dulcesydulces.provedor_backend.domain.entidades.Usuarios::getFotoPerfil).orElse(null);
  }

  @ModelAttribute("rol")
  public String rol(Authentication auth) {
    if (auth == null || auth.getName() == null) return null;
    Optional<co.dulcesydulces.provedor_backend.domain.entidades.Usuarios> u = usuarioRepository.findByCodigo(auth.getName());
    return u.map(co.dulcesydulces.provedor_backend.domain.entidades.Usuarios::getRol).orElse(null);
  }
}
