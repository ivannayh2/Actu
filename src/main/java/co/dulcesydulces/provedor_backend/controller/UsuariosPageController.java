package co.dulcesydulces.provedor_backend.controller;

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
    public String usuarios(Model model) {
        model.addAttribute("porRol", service.listarPorRol());
        return "usuarios";
    }
}
