package co.dulcesydulces.provedor_backend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import co.dulcesydulces.provedor_backend.domain.dto.EgresoCreateRequest;
import co.dulcesydulces.provedor_backend.domain.dto.EgresoPlanoResumen;
import co.dulcesydulces.provedor_backend.domain.entidades.Egreso;
import co.dulcesydulces.provedor_backend.domain.entidades.EgresoPlano;
import co.dulcesydulces.provedor_backend.domain.entidades.FacturaPlano;
import co.dulcesydulces.provedor_backend.repository.EgresoPlanoRepository;
import co.dulcesydulces.provedor_backend.repository.EgresoRepository;
import co.dulcesydulces.provedor_backend.repository.FacturaPlanoRepository;

@Service
public class EgresoService {

    private final EgresoRepository egresoRepository;
    private final EgresoPlanoRepository egresoPlanoRepository;
    private final FacturaPlanoRepository facturaPlanoRepository;
    private final Path baseDir = Paths.get("uploads", "egresos");

    public EgresoService(
            EgresoRepository egresoRepository,
            EgresoPlanoRepository egresoPlanoRepository,
            FacturaPlanoRepository facturaPlanoRepository
    ) {
        this.egresoRepository = egresoRepository;
        this.egresoPlanoRepository = egresoPlanoRepository;
        this.facturaPlanoRepository = facturaPlanoRepository;
    }

    public List<EgresoPlanoResumen> buscarPlanoSegunUsuario(
            Authentication auth,
            String proveedor,
            String numeroEgreso,
            LocalDate fechaDocumento
    ) {
        boolean esAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMINISTRADOR"));

        boolean esPublicador = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("PUBLICADOR"));

        boolean esProveedor = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("PROVEEDORES"));

        if (esAdmin || esPublicador) {
            return egresoPlanoRepository.buscarConFiltros(proveedor, numeroEgreso, fechaDocumento);
        }

        if (esProveedor) {
            String usuarioLogueado = auth.getName();
            return egresoPlanoRepository.buscarPorTerceroYFiltros(
                    usuarioLogueado,
                    numeroEgreso,
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

    public Egreso crear(EgresoCreateRequest req, MultipartFile soporte) {
        if (egresoRepository.existsById(req.getNumeroEgreso())) {
            throw new RuntimeException("Ya existe el egreso: " + req.getNumeroEgreso());
        }

        Egreso e = new Egreso();
        e.setNumeroEgreso(req.getNumeroEgreso().trim());
        e.setProveedor(req.getProveedor().trim());
        e.setValorEgreso(req.getValorEgreso());
        e.setFechaDocumento(req.getFechaDocumento());
        e.setCreadoEn(LocalDateTime.now());

        if (soporte != null && !soporte.isEmpty()) {
            e.setRutaDocumento(guardarSoporte(req.getNumeroEgreso(), soporte));
        }

        return egresoRepository.save(e);
    }

    public Egreso obtenerPorNumero(String numeroEgreso) {
        return egresoRepository.findById(numeroEgreso)
                .orElseThrow(() -> new RuntimeException("No existe el egreso: " + numeroEgreso));
    }

    private String guardarSoporte(String numeroEgreso, MultipartFile file) {
        try {
            Files.createDirectories(baseDir);

            String original = file.getOriginalFilename() == null ? "soporte" : file.getOriginalFilename();
            String safeName = original.replaceAll("[^a-zA-Z0-9._-]", "_");
            String finalName = numeroEgreso + "_" + safeName;

            Path destino = baseDir.resolve(finalName);
            Files.copy(file.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

            return destino.toString().replace("\\", "/");
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo guardar el soporte", ex);
        }
    }
}