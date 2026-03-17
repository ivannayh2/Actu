package co.dulcesydulces.provedor_backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import co.dulcesydulces.provedor_backend.service.ConsultasDocumentosService;

@Controller
@RequestMapping("/historial")
public class HistorialPageController {

    private final ConsultasDocumentosService consultasDocumentosService;

    public HistorialPageController(ConsultasDocumentosService consultasDocumentosService) {
        this.consultasDocumentosService = consultasDocumentosService;
    }

    @GetMapping
    public String page(
        @RequestParam(required = false) String usuario,
        @RequestParam(required = false) String archivo,
        Model model
    ) {
        var historial = consultasDocumentosService.buscarHistorial(usuario, archivo);
        model.addAttribute("historial", historial);
        model.addAttribute("usuario", usuario);
        model.addAttribute("archivo", archivo);
        model.addAttribute("totalCargas", historial.size());
        return "historial";
    }
}