package co.dulcesydulces.provedor_backend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import co.dulcesydulces.provedor_backend.domain.dto.EgresoCreateRequest;
import co.dulcesydulces.provedor_backend.domain.entidades.Egreso;
import co.dulcesydulces.provedor_backend.repository.EgresoRepository;

@Service
public class EgresoService {

    private final EgresoRepository repo;
    private final Path baseDir = Paths.get("uploads", "egresos");

    public EgresoService(EgresoRepository repo) {
        this.repo = repo;
    }

    public List<Egreso> buscar(String proveedor, String numeroEgreso, LocalDate fechaDocumento) {
        return repo.buscar(proveedor, numeroEgreso, fechaDocumento);
    }

    public Egreso crear(EgresoCreateRequest req, MultipartFile soporte) {
        if (repo.existsById(req.getNumeroEgreso())) {
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

        return repo.save(e);
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
