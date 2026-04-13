package co.dulcesydulces.provedor_backend.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import co.dulcesydulces.provedor_backend.domain.entidades.Usuarios;
import co.dulcesydulces.provedor_backend.repository.UsuarioRepository;
import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

    private final UsuarioRepository usuarioRepository;

    public HomeController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/home")
    public String home(HttpSession session) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String codigo = auth.getName();
            Usuarios user = usuarioRepository.findByCodigo(codigo).orElse(null);

            if (user != null) {
                session.setAttribute("usuario", user.getCodigo());
                session.setAttribute("rol", user.getRol());

                boolean isAdmin = "ADMINISTRADOR".equalsIgnoreCase(user.getRol());
                List<String> permisos = user.getPermisos() != null ? user.getPermisos() : List.of();

                boolean canImportFiles = permisos.stream()
                    .anyMatch(p -> "permImportarArchivosView".equalsIgnoreCase(p != null ? p.trim() : ""));

                boolean canComprobanteEgresos = permisos.stream()
                    .anyMatch(p -> "permComprobanteEgresosView".equalsIgnoreCase(p != null ? p.trim() : ""));

                boolean canPerfil = permisos.stream()
                    .anyMatch(p -> "permPerfilView".equalsIgnoreCase(p != null ? p.trim() : ""));

                if (!isAdmin && !canImportFiles) {
                    if (canComprobanteEgresos) {
                        return "redirect:/egresos";
                    }
                    if (canPerfil) {
                        return "redirect:/configuracion/perfil";
                    }
                    return "redirect:/login?sinpermisos";
                }
            }
        }

        return "home";
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }
}