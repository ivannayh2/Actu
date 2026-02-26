package co.dulcesydulces.provedor_backend.config;

import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import co.dulcesydulces.provedor_backend.repository.UsuarioModuloRepo;

@ControllerAdvice
public class GlobalModel {

  private final UsuarioModuloRepo repo;

  public GlobalModel(UsuarioModuloRepo repo) {
    this.repo = repo;
  }

  @ModelAttribute("modulosPermitidos")
  public Set<String> modulosPermitidos(Authentication auth) {

    if (auth == null || auth.getName() == null) {
      return Set.of();
    }

    String codigo = auth.getName();
    return new HashSet<>(repo.codigosPermitidos(codigo));
  }

  @ModelAttribute("nombreUsuario")
  public String nombreUsuario(Authentication auth) {
    return (auth != null) ? auth.getName() : "Usuario";
  }
}
