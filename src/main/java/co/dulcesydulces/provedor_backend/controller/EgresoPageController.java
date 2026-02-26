package co.dulcesydulces.provedor_backend.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import co.dulcesydulces.provedor_backend.domain.dto.EgresoCreateRequest;
import co.dulcesydulces.provedor_backend.domain.entidades.Egreso;
import co.dulcesydulces.provedor_backend.service.EgresoService;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/egresos")
public class EgresoPageController {

    private final EgresoService service;

    public EgresoPageController(EgresoService service) {
        this.service = service;
    }

    @GetMapping
    public String page(
        @RequestParam(required = false) String proveedor,
        @RequestParam(required = false) String numeroEgreso,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDocumento,
        Model model,
        Authentication auth
    ) {
        // (opcional) validar auth/roles aquí
        List<Egreso> egresos = service.buscar(proveedor, numeroEgreso, fechaDocumento);

        model.addAttribute("egresos", egresos);
        model.addAttribute("proveedor", proveedor);
        model.addAttribute("numeroEgreso", numeroEgreso);
        model.addAttribute("fechaDocumento", fechaDocumento);

        // para el modal
        model.addAttribute("nuevoEgreso", new EgresoCreateRequest());

        return "egresos";
    }

    @PostMapping
    public String crear(
        @Valid @ModelAttribute("nuevoEgreso") EgresoCreateRequest req,
        BindingResult br,
        @RequestParam(name = "soporte", required = false) MultipartFile soporte,
        RedirectAttributes ra
    ) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("error", "Datos inválidos, revisa el formulario.");
            return "redirect:/egresos";
        }

        try {
            service.crear(req, soporte);
            ra.addFlashAttribute("ok", "Egreso creado correctamente.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/egresos";
    }
}
