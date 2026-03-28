package co.dulcesydulces.provedor_backend.controller;

import co.dulcesydulces.provedor_backend.repository.HistorialRepository;
import co.dulcesydulces.provedor_backend.repository.UploadsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/historial")
public class HistorialRestController {
    private final HistorialRepository historialRepository;
    private final UploadsRepository uploadsRepository;

    public HistorialRestController(HistorialRepository historialRepository, UploadsRepository uploadsRepository) {
        this.historialRepository = historialRepository;
        this.uploadsRepository = uploadsRepository;
    }

    @DeleteMapping("/limpiar")
    public ResponseEntity<?> limpiarHistorial() {
        historialRepository.deleteAll();
        uploadsRepository.deleteAllUploads();
        return ResponseEntity.ok().build();
    }
}
