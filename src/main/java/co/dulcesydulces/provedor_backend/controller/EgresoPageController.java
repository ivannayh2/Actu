package co.dulcesydulces.provedor_backend.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
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
import co.dulcesydulces.provedor_backend.domain.entidades.EgresoPlano;
import co.dulcesydulces.provedor_backend.service.EgresoService;
import co.dulcesydulces.provedor_backend.service.ProveedoresService;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/egresos")
@PreAuthorize("hasAnyAuthority('ADMINISTRADOR','permComprobanteEgresosView')")
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
            @RequestParam(required = false) String doctoSa,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDocumento,
            Model model,
            Authentication auth
    ) {
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMINISTRADOR"));

        boolean esPublicador = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("PUBLICADOR"));

        boolean puedeFiltrarProveedor = esAdmin || esPublicador;
        boolean puedeCrearEgreso = esAdmin || esPublicador;

        model.addAttribute("proveedor", proveedor);
        model.addAttribute("numeroEgreso", numeroEgreso);
        model.addAttribute("doctoSa", doctoSa);
        model.addAttribute("fechaDocumento", fechaDocumento);
        model.addAttribute("puedeFiltrarProveedor", puedeFiltrarProveedor);
        model.addAttribute("puedeCrearEgreso", puedeCrearEgreso);
        model.addAttribute("proveedoresOptions", proveedoresService.getListaEnOptions());
        model.addAttribute("nuevoEgreso", new EgresoCreateRequest());

        if (doctoSa != null && !doctoSa.trim().isEmpty()) {
            var detalle = service.buscarPlanoSegunUsuario(
                    auth,
                    proveedor,
                    numeroEgreso,
                    doctoSa,
                    fechaDocumento
            );

            BigDecimal totalVlrEgreso = detalle.stream()
                    .map(d -> d.getVlrEgreso() != null ? d.getVlrEgreso() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            model.addAttribute("detalle", detalle);
            model.addAttribute("totalVlrEgreso", totalVlrEgreso);

            return "egresos";
        }

        List<EgresoPlano> detalles = service.buscarDetalleSegunUsuario(
                auth,
                proveedor,
                numeroEgreso,
                null,
                fechaDocumento
        );

        cargarTotalesDetalle(model, detalles);
        model.addAttribute("detalles", detalles);

        return "egresosDetallado";
    }

    @GetMapping("/detallado")
    public String verDetalleEgreso(
            @RequestParam("doctoEgreso") String doctoEgreso,
            Model model
    ) {
        List<EgresoPlano> detalles = service.buscarDetallePorDoctoEgreso(doctoEgreso);

        model.addAttribute("doctoEgreso", doctoEgreso);
        model.addAttribute("detalles", detalles);

        cargarTotalesDetalle(model, detalles);

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
            @RequestParam(name = "soportes", required = false) MultipartFile[] soportes,
            RedirectAttributes ra
    ) {
        if (br.hasErrors()) {
            ra.addFlashAttribute("error", "Datos inválidos, revisa el formulario.");
            return "redirect:/egresos";
        }

        try {
            service.crear(req, soportes);
            ra.addFlashAttribute("ok", "Egreso creado correctamente.");
        } catch (RuntimeException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        }

        return "redirect:/egresos";
    }

    private void cargarTotalesDetalle(Model model, List<EgresoPlano> detalles) {
        BigDecimal totalDebitos = detalles.stream()
                .map(this::valorAjustado)
                .filter(v -> v.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCreditos = detalles.stream()
                .map(this::valorAjustado)
                .filter(v -> v.compareTo(BigDecimal.ZERO) < 0)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalProntoPago = detalles.stream()
                .map(d -> nvl(d.getProntoPago()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalValorDocto = detalles.stream()
                .map(EgresoPlano::getValorDocto)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(BigDecimal.ZERO);

        model.addAttribute("totalDebitos", totalDebitos);
        model.addAttribute("totalCreditos", totalCreditos);
        model.addAttribute("totalProntoPago", totalProntoPago);
        model.addAttribute("totalValorDocto", totalValorDocto);
    }

    private BigDecimal valorAjustado(EgresoPlano d) {
        return nvl(d.getVlrEgreso()).add(nvl(d.getProntoPago()));
    }

    private BigDecimal nvl(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }
}