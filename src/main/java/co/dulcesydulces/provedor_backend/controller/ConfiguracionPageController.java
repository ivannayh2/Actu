package co.dulcesydulces.provedor_backend.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import co.dulcesydulces.provedor_backend.domain.entidades.Usuarios;
import co.dulcesydulces.provedor_backend.repository.UsuarioRepository;

@Controller
@RequestMapping("/configuracion")
public class ConfiguracionPageController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public ConfiguracionPageController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String page() {
        return "configuracion";
    }

    @GetMapping("/perfil")
    public String perfil(Authentication authentication, org.springframework.ui.Model model) {
        String codigo = authentication != null ? authentication.getName() : null;
        if (codigo != null) {
            usuarioRepository.findByCodigo(codigo).ifPresent(usuario -> {
                model.addAttribute("codigo", usuario.getCodigo());
                model.addAttribute("nombreUsuario", usuario.getNombreUsuario());
                model.addAttribute("email", usuario.getEmail());
            });
        }
        return "configuracion-perfil";
    }

    @PostMapping("/perfil")
    public String cambiarClave(
        Authentication authentication,
        @RequestParam("claveActual") String claveActual,
        @RequestParam("claveNueva") String claveNueva,
        @RequestParam("claveNueva2") String claveNueva2,
        RedirectAttributes ra
    ) {
        if (authentication == null || authentication.getName() == null) {
            ra.addFlashAttribute("error", "No fue posible identificar el usuario autenticado.");
            return "redirect:/configuracion/perfil";
        }

        String actual = claveActual == null ? "" : claveActual.trim();
        String nueva = claveNueva == null ? "" : claveNueva.trim();
        String nueva2 = claveNueva2 == null ? "" : claveNueva2.trim();

        if (actual.isEmpty() || nueva.isEmpty() || nueva2.isEmpty()) {
            ra.addFlashAttribute("error", "Todos los campos de clave son obligatorios.");
            return "redirect:/configuracion/perfil";
        }
        if (nueva.length() < 6) {
            ra.addFlashAttribute("error", "La nueva clave debe tener al menos 6 caracteres.");
            return "redirect:/configuracion/perfil";
        }
        if (!nueva.equals(nueva2)) {
            ra.addFlashAttribute("error", "La nueva clave y la confirmación no coinciden.");
            return "redirect:/configuracion/perfil";
        }

        Usuarios usuario = usuarioRepository.findByCodigo(authentication.getName()).orElse(null);
        if (usuario == null) {
            ra.addFlashAttribute("error", "Usuario no encontrado.");
            return "redirect:/configuracion/perfil";
        }
        if (usuario.getPassword_hash() == null || !passwordEncoder.matches(actual, usuario.getPassword_hash())) {
            ra.addFlashAttribute("error", "La clave actual no es correcta.");
            return "redirect:/configuracion/perfil";
        }

        usuario.setPassword_hash(passwordEncoder.encode(nueva));
        usuarioRepository.save(usuario);
        ra.addFlashAttribute("ok", "Clave actualizada correctamente.");
        return "redirect:/configuracion/perfil";
    }
}
