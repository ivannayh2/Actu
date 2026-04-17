package co.dulcesydulces.provedor_backend.service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import co.dulcesydulces.provedor_backend.domain.dto.EgresoCreateRequest;
import co.dulcesydulces.provedor_backend.domain.dto.EgresoDetalleView;
import co.dulcesydulces.provedor_backend.domain.dto.EgresoPlanoResumen;
import co.dulcesydulces.provedor_backend.domain.entidades.Egreso;
import co.dulcesydulces.provedor_backend.domain.entidades.EgresoPlano;
import co.dulcesydulces.provedor_backend.domain.entidades.EgresoSoportePF;
import co.dulcesydulces.provedor_backend.domain.entidades.FacturaPlano;
import co.dulcesydulces.provedor_backend.domain.entidades.MapasNotasRelacionadas;
import co.dulcesydulces.provedor_backend.domain.entidades.NotaPlano;
import co.dulcesydulces.provedor_backend.repository.EgresoPlanoRepository;
import co.dulcesydulces.provedor_backend.repository.EgresoRepository;
import co.dulcesydulces.provedor_backend.repository.FacturaPlanoRepository;
import co.dulcesydulces.provedor_backend.repository.NotaPlanoRepository;

@Service
public class EgresoService {

    private final EgresoRepository egresoRepository;
    private final EgresoPlanoRepository egresoPlanoRepository;
    private final FacturaPlanoRepository facturaPlanoRepository;
    private final NotaPlanoRepository notaPlanoRepository;
    private final S3StorageService s3StorageService;

    public EgresoService(
            EgresoRepository egresoRepository,
            EgresoPlanoRepository egresoPlanoRepository,
            FacturaPlanoRepository facturaPlanoRepository,
            NotaPlanoRepository notaPlanoRepository,
            S3StorageService s3StorageService
    ) {
        this.egresoRepository = egresoRepository;
        this.egresoPlanoRepository = egresoPlanoRepository;
        this.facturaPlanoRepository = facturaPlanoRepository;
        this.notaPlanoRepository = notaPlanoRepository;
        this.s3StorageService = s3StorageService;
    }

    public List<EgresoPlanoResumen> buscarPlanoSegunUsuario(
            Authentication auth,
            String proveedor,
            String numeroEgreso,
            String doctoSa,
            LocalDate fechaDocumento
    ) {
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMINISTRADOR"));

        boolean esPublicador = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("PUBLICADOR"));

        boolean esProveedor = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("PROVEEDORES"));

        if (esAdmin || esPublicador) {
            return egresoPlanoRepository.buscarConFiltros(
                    proveedor,
                    numeroEgreso,
                    doctoSa,
                    fechaDocumento
            );
        }

        if (esProveedor) {
            String usuarioLogueado = auth.getName();
            return egresoPlanoRepository.buscarPorTerceroYFiltros(
                    usuarioLogueado,
                    numeroEgreso,
                    doctoSa,
                    fechaDocumento
            );
        }

        return List.of();
    }

    public List<EgresoPlano> buscarDetalleSegunUsuario(
            Authentication auth,
            String proveedor,
            String numeroEgreso,
            String doctoSa,
            LocalDate fechaDocumento
    ) {
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMINISTRADOR"));

        boolean esPublicador = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("PUBLICADOR"));

        boolean esProveedor = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("PROVEEDORES"));

        if (esAdmin || esPublicador) {
            return egresoPlanoRepository.buscarDetallesConFiltros(
                    proveedor,
                    numeroEgreso,
                    doctoSa,
                    fechaDocumento
            );
        }

        if (esProveedor) {
            String usuarioLogueado = auth.getName();
            return egresoPlanoRepository.buscarDetallesPorTerceroYFiltros(
                    usuarioLogueado,
                    numeroEgreso,
                    doctoSa,
                    fechaDocumento
            );
        }

        return List.of();
    }

    public List<EgresoDetalleView> buscarDetalleVistaSegunUsuario(
            Authentication auth,
            String proveedor,
            String numeroEgreso,
            String doctoSa,
            LocalDate fechaDocumento
    ) {
        List<EgresoPlano> detalles = buscarDetalleSegunUsuario(
                auth,
                proveedor,
                numeroEgreso,
                doctoSa,
                fechaDocumento
        );

        MapasNotasRelacionadas mapas = construirMapasNotasRelacionadas(detalles);

        return detalles.stream()
                .map(detalle -> mapearDetalleVista(detalle, mapas))
                .collect(Collectors.toList());
    }

    public List<EgresoPlano> buscarDetallePorDoctoEgreso(String doctoEgreso) {
        return egresoPlanoRepository.buscarDetallePorDoctoEgreso(doctoEgreso);
    }

    public List<FacturaPlano> buscarFacturasPorDoctoCausacion(String doctoCausacion) {
    if (doctoCausacion == null || doctoCausacion.trim().isEmpty()) {
        return List.of();
    }

    return facturaPlanoRepository.buscarPorDoctoCausacion(doctoCausacion.trim());
}

    public NotaPlano buscarNotaPorDoctoProveedor(String doctoProveedor) {
        return notaPlanoRepository.findFirstByDoctoProveedor(doctoProveedor)
                .orElseThrow(() -> new RuntimeException(
                        "No se encontró nota en notas_plano para el docto_proveedor: " + doctoProveedor
                ));
    }

    @Transactional
    public Egreso crear(EgresoCreateRequest req, MultipartFile[] soportes) {
        Egreso egreso = new Egreso();
        egreso.setFechaDocumento(req.getFechaDocumento());

        if (soportes != null) {
            for (MultipartFile archivo : soportes) {
                if (archivo == null || archivo.isEmpty()) {
                    continue;
                }

                validarArchivo(archivo);

                String nombreOriginal = archivo.getOriginalFilename();
                if (nombreOriginal == null || nombreOriginal.isBlank()) {
                    nombreOriginal = "archivo_sin_nombre";
                }

                String doctoEgreso = extraerDoctoEgreso(nombreOriginal);
                if (doctoEgreso == null) {
                    throw new RuntimeException(
                            "No se pudo identificar el docto_egreso en el archivo: " + nombreOriginal
                    );
                }

                boolean existeDocto = egresoPlanoRepository.existsByDoctoEgreso(doctoEgreso);
                if (!existeDocto) {
                    throw new RuntimeException(
                            "El docto_egreso " + doctoEgreso + " no existe en egresos_plano"
                    );
                }

                String s3Key = subirAS3(archivo);

                EgresoSoportePF soporte = new EgresoSoportePF();
                soporte.setNombreOriginal(nombreOriginal);
                soporte.setDoctoEgreso(doctoEgreso);
                soporte.setS3Key(s3Key);
                soporte.setContentType(archivo.getContentType());
                soporte.setTamanoBytes(archivo.getSize());

                egreso.agregarSoporte(soporte);
            }
        }

        return egresoRepository.save(egreso);
    }

    public Egreso obtenerPorId(Long id) {
        return egresoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No existe el egreso con id: " + id));
    }

    public List<EgresoDetalleView> buscarDetalleVistaPorDoctoEgreso(String doctoEgreso) {
        List<EgresoPlano> detalles = egresoPlanoRepository.buscarDetallePorDoctoEgreso(doctoEgreso);

        MapasNotasRelacionadas mapas = construirMapasNotasRelacionadas(detalles);

        return detalles.stream()
                .map(detalle -> mapearDetalleVista(detalle, mapas))
                .collect(Collectors.toList());
    }

    public List<EgresoDetalleView> buscarDetalleVistaPorDoctoCausacion(String doctoCausacion) {
    List<EgresoPlano> detalles = egresoPlanoRepository.buscarDetallePorDoctoCausacion(doctoCausacion);

    MapasNotasRelacionadas mapas = construirMapasNotasRelacionadas(detalles);

    return detalles.stream()
            .map(detalle -> mapearDetalleVista(detalle, mapas))
            .collect(Collectors.toList());
}

    public List<EgresoDetalleView> buscarDetalleVistaPorDoctoSa(String doctoSa) {
    List<EgresoPlano> detalles = egresoPlanoRepository.buscarDetallePorDoctoSa(doctoSa);

    MapasNotasRelacionadas mapas = construirMapasNotasRelacionadas(detalles);

    return detalles.stream()
            .map(detalle -> mapearDetalleVista(detalle, mapas))
            .collect(Collectors.toList());
}

    private MapasNotasRelacionadas construirMapasNotasRelacionadas(List<EgresoPlano> detalles) {
        List<String> doctosSaBase = detalles.stream()
                .filter(detalle -> debeBuscarNotaRelacionada(detalle.getNotas()))
                .map(EgresoPlano::getDoctoSa)
                .map(this::extraerDoctoSaBase)
                .filter(this::tieneTexto)
                .distinct()
                .collect(Collectors.toList());

        if (doctosSaBase.isEmpty()) {
            return new MapasNotasRelacionadas(Map.of(), Map.of(), Map.of());
        }

        List<String> valoresNormalizados = doctosSaBase.stream()
                .map(this::normalizarTextoComparacion)
                .filter(this::tieneTexto)
                .distinct()
                .collect(Collectors.toList());

        Map<String, NotaPlano> porDoctoProveedor = notaPlanoRepository.findByDoctoProveedorIn(doctosSaBase).stream()
                .filter(nota -> tieneTexto(nota.getDoctoProveedor()))
                .filter(nota -> esDocumentoFP(nota.getNroDocumento()))
                .collect(Collectors.toMap(
                        nota -> normalizarTextoComparacion(nota.getDoctoProveedor()),
                        Function.identity(),
                        (primero, segundo) -> primero
                ));

        Map<String, NotaPlano> porNroDocumento = notaPlanoRepository.buscarPorNroDocumentoNormalizadoIn(valoresNormalizados).stream()
                .filter(nota -> tieneTexto(nota.getNroDocumento()))
                .filter(nota -> esDocumentoFP(nota.getNroDocumento()))
                .collect(Collectors.toMap(
                        nota -> normalizarTextoComparacion(nota.getNroDocumento()),
                        Function.identity(),
                        (primero, segundo) -> primero
                ));

        Map<String, NotaPlano> porReferencia1 = notaPlanoRepository.buscarPorReferencia1NormalizadaIn(valoresNormalizados).stream()
                .filter(nota -> tieneTexto(nota.getReferencia1()))
                .filter(nota -> esDocumentoFP(nota.getNroDocumento()))
                .collect(Collectors.toMap(
                        nota -> normalizarTextoComparacion(nota.getReferencia1()),
                        Function.identity(),
                        (primero, segundo) -> primero
                ));

        return new MapasNotasRelacionadas(porDoctoProveedor, porNroDocumento, porReferencia1);
    }

    private EgresoDetalleView mapearDetalleVista(EgresoPlano egreso, MapasNotasRelacionadas mapas) {
    EgresoDetalleView view = new EgresoDetalleView(egreso);

    // fallback por defecto
    view.setNotaMostrada(egreso.getNotas());

    if (!debeBuscarNotaRelacionada(egreso.getNotas())) {
        return view;
    }

    String doctoSaBase = extraerDoctoSaBase(egreso.getDoctoSa());
    view.setDoctoSaBase(doctoSaBase);

    if (!tieneTexto(doctoSaBase)) {
        return view;
    }

    String clave = normalizarTextoComparacion(doctoSaBase);

    NotaPlano notaPorDoctoProveedor = mapas.getPorDoctoProveedor().get(clave);
    if (notaPorDoctoProveedor != null) {
        llenarNotaRelacionada(view, notaPorDoctoProveedor, "docto_proveedor", notaPorDoctoProveedor.getDoctoProveedor());
        return view;
    }

    NotaPlano notaPorNroDocumento = mapas.getPorNroDocumento().get(clave);
    if (notaPorNroDocumento != null) {
        llenarNotaRelacionada(view, notaPorNroDocumento, "nro_documento", notaPorNroDocumento.getNroDocumento());
        return view;
    }

    NotaPlano notaPorReferencia1 = mapas.getPorReferencia1().get(clave);
    if (notaPorReferencia1 != null) {
        llenarNotaRelacionada(view, notaPorReferencia1, "referencia_1", notaPorReferencia1.getReferencia1());
        return view;
    }

    // si no encontró nada, se queda con egresos_plano.notas
    return view;
}

    private void llenarNotaRelacionada(
        EgresoDetalleView view,
        NotaPlano nota,
        String campoCoincidencia,
        String valorCoincidencia
) {
    view.setMostrarNotaRelacionada(true);
    view.setDoctoProveedorRelacionado(nota.getDoctoProveedor());
    view.setNotaPlanoRelacionada(nota.getNotas());
    view.setCampoCoincidenciaNota(campoCoincidencia);
    view.setValorCoincidenciaNota(valorCoincidencia);
    view.setNotaMostrada(nota.getNotas());

        // NUEVO
        view.setNroDocumentoNota(nota.getNroDocumento());
        view.setValorNetoNota(nota.getValorNeto());
}

    private boolean debeBuscarNotaRelacionada(String notas) {
        if (notas == null) {
            return false;
        }

        String valor = notas.trim();
        return !valor.isEmpty() && !valor.equalsIgnoreCase("SN");
    }

    private boolean esDocumentoFP(String nroDocumento) {
        return nroDocumento != null && nroDocumento.trim().toUpperCase().startsWith("FP");
    }

    private String extraerDoctoSaBase(String doctoSa) {
        if (!tieneTexto(doctoSa)) {
            return null;
        }

        String valor = doctoSa.trim();

        String[] partes = valor.split("-");
        if (partes.length >= 3) {
            return partes[1].trim();
        }

        return valor;
    }

    private String normalizarTextoComparacion(String valor) {
        if (!tieneTexto(valor)) {
            return null;
        }

        return valor.replace("-", "")
                .replace(" ", "")
                .replace(".", "")
                .trim()
                .toUpperCase();
    }

    private boolean tieneTexto(String valor) {
        return valor != null && !valor.trim().isEmpty();
    }

    private void validarArchivo(MultipartFile file) {
        String contentType = file.getContentType();

        boolean permitido = "application/pdf".equals(contentType)
                || (contentType != null && contentType.startsWith("image/"));

        if (!permitido) {
            throw new RuntimeException("Tipo de archivo no permitido: " + contentType);
        }
    }

    private String extraerDoctoEgreso(String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.isBlank()) {
            return null;
        }

        Pattern pattern = Pattern.compile("(CE\\d+-\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(nombreArchivo);

        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        }

        return null;
    }

    private String subirAS3(MultipartFile file) {
        try {
            String original = file.getOriginalFilename() == null ? "soporte" : file.getOriginalFilename();
            String safeName = original.replaceAll("[^a-zA-Z0-9._-]", "_");
            String key = "egresos/" + UUID.randomUUID() + "_" + safeName;

            return s3StorageService.subirArchivo(
                    key,
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType()
            );
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo subir el soporte a S3", ex);
        }
    }
}