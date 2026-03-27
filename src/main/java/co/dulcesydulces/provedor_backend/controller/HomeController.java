package co.dulcesydulces.provedor_backend.controller;

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
                String rol = user.getRol() != null ? user.getRol().toUpperCase() : "";
                if (rol.equals("PROVEEDOR") || rol.equals("PROVEEDORES")) {
                    return "redirect:/egresos";
                }
            }
        }
        return "home"; // home.html
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/home";
    }
}
