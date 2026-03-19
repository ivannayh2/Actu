package co.dulcesydulces.provedor_backend.controller;

import java.math.BigDecimal;
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
import co.dulcesydulces.provedor_backend.domain.dto.EgresoPlanoResumen;
import co.dulcesydulces.provedor_backend.service.EgresoService;
import co.dulcesydulces.provedor_backend.service.ProveedoresService;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/egresos")
public class EgresoPageController {

    private final EgresoService service;
    private final ProveedoresService proveedoresService;

    public EgresoPageController(EgresoService service, ProveedoresService proveedoresService) {
        this.service = service;
        this.proveedoresService = proveedoresService;
    }

    @GetMapping
    public String page(
            @RequestParam(required = false) String proveedor,
            @RequestParam(required = false) String numeroEgreso,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDocumento,
            Model model,
            Authentication auth
    ) {
        List<EgresoPlanoResumen> detalle = service.buscarPlanoSegunUsuario(auth, proveedor, numeroEgreso, fechaDocumento);

        BigDecimal totalVlrEgreso = detalle.stream()
                .map(EgresoPlanoResumen::getVlrEgreso)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMINISTRADOR"));

        boolean esPublicador = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("PUBLICADOR"));

        boolean puedeFiltrarProveedor = esAdmin || esPublicador;
        boolean puedeCrearEgreso = esAdmin || esPublicador;

        model.addAttribute("egresos", detalle);
        model.addAttribute("detalle", detalle);
        model.addAttribute("totalVlrEgreso", totalVlrEgreso);
        model.addAttribute("proveedor", proveedor);
        model.addAttribute("numeroEgreso", numeroEgreso);
        model.addAttribute("fechaDocumento", fechaDocumento);
        model.addAttribute("puedeFiltrarProveedor", puedeFiltrarProveedor);
        model.addAttribute("puedeCrearEgreso", puedeCrearEgreso);
        model.addAttribute("proveedoresOptions", proveedoresService.getListaEnOptions());
        model.addAttribute("nuevoEgreso", new EgresoCreateRequest());

        return "egresos";
    }

    @GetMapping("/detallado")
    public String verDetalleEgreso(
            @RequestParam("doctoEgreso") String doctoEgreso,
            Model model
    ) {
        var detalles = service.buscarDetallePorDoctoEgreso(doctoEgreso);

        BigDecimal totalDetalleEgreso = detalles.stream()
                .map(d -> d.getVlrEgreso())
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("doctoEgreso", doctoEgreso);
        model.addAttribute("detalles", detalles);
        model.addAttribute("totalDetalleEgreso", totalDetalleEgreso);

        return "egresosDetallado";
    }

    @GetMapping("/detalles")
    public String verDetalleFactura(
            @RequestParam("doctoCausacion") String doctoCausacion,
            Model model
    ) {
        model.addAttribute("doctoCausacion", doctoCausacion);
        model.addAttribute("facturas", service.buscarFacturasPorDoctoCausacion(doctoCausacion));
        return "detalleFactura";
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