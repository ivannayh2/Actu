package co.dulcesydulces.provedor_backend.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.List;
import java.util.Objects;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import co.dulcesydulces.provedor_backend.domain.dto.EgresoDetalleView;
import co.dulcesydulces.provedor_backend.domain.dto.EgresoPlanoResumen;
import co.dulcesydulces.provedor_backend.domain.entidades.EgresoPlano;
import co.dulcesydulces.provedor_backend.service.EgresoExportService;
import co.dulcesydulces.provedor_backend.service.EgresoService;
import co.dulcesydulces.provedor_backend.service.ProveedoresService;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/egresos")
@PreAuthorize("hasAnyAuthority('ADMINISTRADOR','permComprobanteEgresosView')")
public class EgresoPageController {

    private final EgresoService service;
    private final EgresoExportService egresoExportService;
    private final ProveedoresService proveedoresService;

    public EgresoPageController(
            EgresoService service,
            EgresoExportService egresoExportService,
            ProveedoresService proveedoresService
    ) {
        this.service = service;
        this.egresoExportService = egresoExportService;
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

                BigDecimal totalVlrEgreso = calcularTotalVlrEgreso(detalle);

            model.addAttribute("detalle", detalle);
            model.addAttribute("totalVlrEgreso", totalVlrEgreso);

            return "egresos";
        }

        List<EgresoPlano> detallesPlano = service.buscarDetalleSegunUsuario(
                auth,
                proveedor,
                numeroEgreso,
                null,
                fechaDocumento
        );

        List<EgresoDetalleView> detalles = service.buscarDetalleVistaSegunUsuario(
                auth,
                proveedor,
                numeroEgreso,
                null,
                fechaDocumento
        );

        cargarTotalesDetalle(model, detallesPlano);
        model.addAttribute("detalles", detalles);

        return "egresosDetallado";
    }

    @GetMapping("/detallado")
public String verDetalleEgreso(
        @RequestParam("doctoEgreso") String doctoEgreso,
        Model model
) {
    List<EgresoPlano> detallesPlano = service.buscarDetallePorDoctoEgreso(doctoEgreso);
    List<EgresoDetalleView> detalles = service.buscarDetalleVistaPorDoctoEgreso(doctoEgreso);

    model.addAttribute("doctoEgreso", doctoEgreso);
    model.addAttribute("detalles", detalles);

    cargarTotalesDetalle(model, detallesPlano);

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

    @GetMapping("/nota-detalle")
    public String verDetalleNota(
            @RequestParam("doctoProveedor") String doctoProveedor,
            Model model
    ) {
        model.addAttribute("nota", service.buscarNotaPorDoctoProveedor(doctoProveedor));
        return "detalleNotaPlano";
    }

        @GetMapping(value = "/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
        public ResponseEntity<byte[]> exportarPdf(
            @RequestParam(defaultValue = "detalle") String vista,
            @RequestParam(required = false) String proveedor,
            @RequestParam(required = false) String numeroEgreso,
            @RequestParam(required = false) String doctoSa,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDocumento,
            Authentication auth
        ) {
        ExportPayload payload = construirPayloadExportacion(vista, auth, proveedor, numeroEgreso, doctoSa, fechaDocumento);

        byte[] archivo = payload.resumen()
            ? egresoExportService.generarPdfResumen(payload.resumenData(), payload.totalVlrEgreso())
            : egresoExportService.generarPdfDetalle(
                payload.detalles(),
                payload.totales().totalValorDocto(),
                payload.totales().totalProntoPago(),
                payload.totales().totalDebitos(),
                payload.totales().totalCreditosFinal()
            );

        return crearDescarga(
            archivo,
            MediaType.APPLICATION_PDF,
            payload.resumen() ? "egresos-resumen.pdf" : "egresos-detalle.pdf"
        );
        }

        @GetMapping(value = "/export/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        public ResponseEntity<byte[]> exportarExcel(
            @RequestParam(defaultValue = "detalle") String vista,
            @RequestParam(required = false) String proveedor,
            @RequestParam(required = false) String numeroEgreso,
            @RequestParam(required = false) String doctoSa,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDocumento,
            Authentication auth
        ) {
        ExportPayload payload = construirPayloadExportacion(vista, auth, proveedor, numeroEgreso, doctoSa, fechaDocumento);

        byte[] archivo = payload.resumen()
            ? egresoExportService.generarExcelResumen(payload.resumenData(), payload.totalVlrEgreso())
            : egresoExportService.generarExcelDetalle(
                payload.detalles(),
                payload.totales().totalValorDocto(),
                payload.totales().totalProntoPago(),
                payload.totales().totalDebitos(),
                payload.totales().totalCreditosFinal()
            );

        return crearDescarga(
            archivo,
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            payload.resumen() ? "egresos-resumen.xlsx" : "egresos-detalle.xlsx"
        );
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
        DetalleTotales totales = calcularTotalesDetalle(detalles);

        model.addAttribute("totalDebitos", totales.totalDebitos());
        model.addAttribute("totalCreditos", totales.totalCreditosBase());
        model.addAttribute("totalProntoPago", totales.totalProntoPago());
        model.addAttribute("totalValorDocto", totales.totalValorDocto());
    }

        private ExportPayload construirPayloadExportacion(
            String vista,
            Authentication auth,
            String proveedor,
            String numeroEgreso,
            String doctoSa,
            LocalDate fechaDocumento
        ) {
        boolean esResumen = "resumen".equalsIgnoreCase(vista);
        if (esResumen) {
            List<EgresoPlanoResumen> resumenData = service.buscarPlanoSegunUsuario(
                auth,
                proveedor,
                numeroEgreso,
                doctoSa,
                fechaDocumento
            );

            return new ExportPayload(
                true,
                resumenData,
                List.of(),
                calcularTotalVlrEgreso(resumenData),
                new DetalleTotales(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
            );
        }

        List<EgresoPlano> detallesPlano = service.buscarDetalleSegunUsuario(
            auth,
            proveedor,
            numeroEgreso,
            doctoSa,
            fechaDocumento
        );

        return new ExportPayload(
            false,
            List.of(),
            service.buscarDetalleVistaSegunUsuario(auth, proveedor, numeroEgreso, doctoSa, fechaDocumento),
            BigDecimal.ZERO,
            calcularTotalesDetalle(detallesPlano)
        );
        }

        private ResponseEntity<byte[]> crearDescarga(byte[] contenido, MediaType mediaType, String nombreArchivo) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + normalizarNombreArchivo(nombreArchivo) + "\"")
            .contentType(mediaType)
            .contentLength(contenido.length)
            .body(contenido);
        }

        private String normalizarNombreArchivo(String nombreArchivo) {
        return nombreArchivo.toLowerCase(Locale.ROOT).replace(' ', '-');
        }

        private BigDecimal calcularTotalVlrEgreso(List<EgresoPlanoResumen> detalle) {
        return detalle.stream()
            .map(d -> d.getVlrEgreso() != null ? d.getVlrEgreso() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private DetalleTotales calcularTotalesDetalle(List<EgresoPlano> detalles) {
        BigDecimal totalDebitos = detalles.stream()
            .map(this::valorAjustado)
            .filter(v -> v.compareTo(BigDecimal.ZERO) > 0)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCreditosBase = detalles.stream()
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

        BigDecimal totalCreditosFinal = totalCreditosBase.add(totalProntoPago).add(totalValorDocto);

        return new DetalleTotales(totalDebitos, totalCreditosBase, totalProntoPago, totalValorDocto, totalCreditosFinal);
        }

    private BigDecimal valorAjustado(EgresoPlano d) {
        return nvl(d.getVlrEgreso()).add(nvl(d.getProntoPago()));
    }

    private BigDecimal nvl(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private record DetalleTotales(
            BigDecimal totalDebitos,
            BigDecimal totalCreditosBase,
            BigDecimal totalProntoPago,
            BigDecimal totalValorDocto,
            BigDecimal totalCreditosFinal
    ) {
    }

    private record ExportPayload(
            boolean resumen,
            List<EgresoPlanoResumen> resumenData,
            List<EgresoDetalleView> detalles,
            BigDecimal totalVlrEgreso,
            DetalleTotales totales
    ) {
    }
}