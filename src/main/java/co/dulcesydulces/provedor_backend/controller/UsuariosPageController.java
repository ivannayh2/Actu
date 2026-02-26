package co.dulcesydulces.provedor_backend.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import co.dulcesydulces.provedor_backend.service.UsuarioService;

@Controller
public class UsuariosPageController {

    private final UsuarioService service;

    public UsuariosPageController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping("/usuarios")
    public String usuarios(Model model, Authentication auth) {

        //Si no está logueado, Spring Security redirige solo.
        // Esto es solo por seguridad extra:
        if (auth == null || !auth.isAuthenticated()) {
            return "redirect:/login";
        }

        //Validar rol/authority con Security (no con session)
        boolean esAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ADMINISTRADOR"));

        if (!esAdmin) {
            return "redirect:/home"; // o devolver 403
        }

        model.addAttribute("porRol", service.listarPorRol());
        return "usuarios";
    }
}
