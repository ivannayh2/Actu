package co.dulcesydulces.provedor_backend.service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import co.dulcesydulces.provedor_backend.domain.dto.EgresoCreateRequest;
import co.dulcesydulces.provedor_backend.domain.dto.EgresoPlanoResumen;
import co.dulcesydulces.provedor_backend.domain.entidades.Egreso;
import co.dulcesydulces.provedor_backend.domain.entidades.EgresoPlano;
import co.dulcesydulces.provedor_backend.domain.entidades.EgresoSoportePF;
import co.dulcesydulces.provedor_backend.domain.entidades.FacturaPlano;
import co.dulcesydulces.provedor_backend.repository.EgresoPlanoRepository;
import co.dulcesydulces.provedor_backend.repository.EgresoRepository;
import co.dulcesydulces.provedor_backend.repository.FacturaPlanoRepository;

@Service
public class EgresoService {

    private final EgresoRepository egresoRepository;
    private final EgresoPlanoRepository egresoPlanoRepository;
    private final FacturaPlanoRepository facturaPlanoRepository;
    private final S3StorageService s3StorageService;

    public EgresoService(
            EgresoRepository egresoRepository,
            EgresoPlanoRepository egresoPlanoRepository,
            FacturaPlanoRepository facturaPlanoRepository,
            S3StorageService s3StorageService
    ) {
        this.egresoRepository = egresoRepository;
        this.egresoPlanoRepository = egresoPlanoRepository;
        this.facturaPlanoRepository = facturaPlanoRepository;
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

    public List<EgresoPlano> buscarDetallePorDoctoEgreso(String doctoEgreso) {
        return egresoPlanoRepository.buscarDetallePorDoctoEgreso(doctoEgreso);
    }

    public List<FacturaPlano> buscarFacturasPorDoctoCausacion(String doctoCausacion) {
        return facturaPlanoRepository.buscarPorDoctoCausacion(doctoCausacion);
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

                String s3Key = subirAS3(archivo);

                EgresoSoportePF soporte = new EgresoSoportePF();
                soporte.setNombreOriginal(archivo.getOriginalFilename());
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

    private void validarArchivo(MultipartFile file) {
        String contentType = file.getContentType();

        boolean permitido = "application/pdf".equals(contentType)
                || (contentType != null && contentType.startsWith("image/"));

        if (!permitido) {
            throw new RuntimeException("Tipo de archivo no permitido: " + contentType);
        }
    }

    private String subirAS3(MultipartFile file) {
        try {
            String original = file.getOriginalFilename() == null ? "soporte" : file.getOriginalFilename();
            String safeName = original.replaceAll("[^a-zA-Z0-9._-]", "_");
            String key = "egresos/" + UUID.randomUUID() + "_" + safeName;

            return s3StorageService.subirArchivo(key, file.getInputStream(), file.getSize(), file.getContentType());
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo subir el soporte a S3", ex);
        }
    }
}