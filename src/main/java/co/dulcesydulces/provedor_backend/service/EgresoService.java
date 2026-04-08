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

        Map<String, NotaPlano> notasPorDoctoProveedor = construirMapaNotasRelacionadas(detalles);

        return detalles.stream()
                .map(detalle -> mapearDetalleVista(detalle, notasPorDoctoProveedor))
                .collect(Collectors.toList());
    }

    public List<EgresoPlano> buscarDetallePorDoctoEgreso(String doctoEgreso) {
        return egresoPlanoRepository.buscarDetallePorDoctoEgreso(doctoEgreso);
    }

    public List<FacturaPlano> buscarFacturasPorDoctoCausacion(String doctoCausacion) {
        return facturaPlanoRepository.buscarPorDoctoCausacion(doctoCausacion);
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

    Map<String, NotaPlano> notasPorDoctoProveedor = construirMapaNotasRelacionadas(detalles);

    return detalles.stream()
            .map(detalle -> mapearDetalleVista(detalle, notasPorDoctoProveedor))
            .collect(Collectors.toList());
}

    private Map<String, NotaPlano> construirMapaNotasRelacionadas(List<EgresoPlano> detalles) {
        List<String> doctosProveedor = detalles.stream()
                .filter(detalle -> debeBuscarNotaRelacionada(detalle.getNotas()))
                .map(EgresoPlano::getDoctoSa)
                .map(this::extraerDoctoSaBase)
                .filter(this::tieneTexto)
                .distinct()
                .collect(Collectors.toList());

        if (doctosProveedor.isEmpty()) {
            return Map.of();
        }

        return notaPlanoRepository.findByDoctoProveedorIn(doctosProveedor).stream()
                .filter(nota -> tieneTexto(nota.getDoctoProveedor()))
                .collect(Collectors.toMap(
                        NotaPlano::getDoctoProveedor,
                        Function.identity(),
                        (primero, segundo) -> primero
                ));
    }

    private EgresoDetalleView mapearDetalleVista(EgresoPlano egreso, Map<String, NotaPlano> notasPorDoctoProveedor) {
        EgresoDetalleView view = new EgresoDetalleView(egreso);

        if (!debeBuscarNotaRelacionada(egreso.getNotas())) {
            return view;
        }

        String doctoSaBase = extraerDoctoSaBase(egreso.getDoctoSa());
        view.setDoctoSaBase(doctoSaBase);

        if (!tieneTexto(doctoSaBase)) {
            return view;
        }

        NotaPlano nota = notasPorDoctoProveedor.get(doctoSaBase);
        if (nota == null) {
            return view;
        }

        view.setMostrarNotaRelacionada(true);
        view.setDoctoProveedorRelacionado(nota.getDoctoProveedor());
        view.setNotaPlanoRelacionada(nota.getNotas());

        return view;
    }

    private boolean debeBuscarNotaRelacionada(String notas) {
        if (notas == null) {
            return false;
        }

        String valor = notas.trim();
        return !valor.isEmpty() && !valor.equalsIgnoreCase("SN");
    }

    private String extraerDoctoSaBase(String doctoSa) {
        if (!tieneTexto(doctoSa)) {
            return null;
        }

        int ultimoGuion = doctoSa.lastIndexOf('-');
        if (ultimoGuion <= 0) {
            return doctoSa.trim();
        }

        return doctoSa.substring(0, ultimoGuion).trim();
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