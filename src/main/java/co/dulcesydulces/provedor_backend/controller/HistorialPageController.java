package co.dulcesydulces.provedor_backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import co.dulcesydulces.provedor_backend.repository.UsuarioRepository;
import co.dulcesydulces.provedor_backend.domain.entidades.Usuarios;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import co.dulcesydulces.provedor_backend.service.ConsultasDocumentosService;

@Controller
@RequestMapping("/historial")

public class HistorialPageController {
    private final ConsultasDocumentosService consultasDocumentosService;
    private final UsuarioRepository usuarioRepository;

    public HistorialPageController(ConsultasDocumentosService consultasDocumentosService, UsuarioRepository usuarioRepository) {
        this.consultasDocumentosService = consultasDocumentosService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public String page(
        @RequestParam(required = false) String usuario,
        @RequestParam(required = false) String fecha,
        @RequestParam(required = false) String tipoMovimiento,
        Authentication authentication,
        Model model
    ) {
        String codigo = authentication != null ? authentication.getName() : null;
        Usuarios usuarioActual = null;
        if (codigo != null) {
            usuarioActual = usuarioRepository.findByCodigo(codigo).orElse(null);
        }
        String rol = usuarioActual != null ? usuarioActual.getRol() : null;

        // Si es publicador, solo ve movimientos hechos por usuarios con rol proveedor
        String usuarioFiltro = (usuario != null && !usuario.isBlank()) ? usuario : null;
        String tipoFiltro = (tipoMovimiento != null && !tipoMovimiento.isBlank()) ? tipoMovimiento : null;
        boolean soloProveedores = rol != null && rol.equalsIgnoreCase("publicador");

        var historial = consultasDocumentosService.buscarHistorial(
            usuarioFiltro,
            (fecha != null && !fecha.isBlank()) ? fecha : null,
            tipoFiltro,
            soloProveedores
        );
        model.addAttribute("historial", historial);
        model.addAttribute("usuario", usuario);
        model.addAttribute("fecha", fecha);
        model.addAttribute("tipoMovimiento", tipoMovimiento);
        model.addAttribute("totalCargas", historial.size());
        return "historial";
    }
}