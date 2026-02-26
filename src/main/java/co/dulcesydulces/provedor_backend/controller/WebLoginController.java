package co.dulcesydulces.provedor_backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import co.dulcesydulces.provedor_backend.domain.entidades.Usuarios;
import co.dulcesydulces.provedor_backend.service.AuthService;
import jakarta.servlet.http.HttpSession;

@Controller
public class WebLoginController {

    private final AuthService authService;

    public WebLoginController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

   @PostMapping("/login")
public String doLogin(@RequestParam("codigo") String codigo,
                      @RequestParam("clave") String clave,
                      HttpSession session,
                      Model model) {
    try {
        Usuarios user = authService.autenticar(codigo, clave);
        session.setAttribute("usuario", user.getCodigo());
        session.setAttribute("rol", user.getRol());
        return "redirect:/home"; // o /usuarios si prefieres
    } catch (Exception e) {
        model.addAttribute("error", e.getMessage());
        return "login";
    }
}
}