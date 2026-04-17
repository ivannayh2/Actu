package co.dulcesydulces.provedor_backend.controller;

import co.dulcesydulces.provedor_backend.repository.HistorialRepository;
import co.dulcesydulces.provedor_backend.repository.UploadsRepository;
import co.dulcesydulces.provedor_backend.repository.UsuarioRepository;
import co.dulcesydulces.provedor_backend.domain.entidades.Usuarios;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/historial")
public class HistorialRestController {
    private final HistorialRepository historialRepository;
    private final UploadsRepository uploadsRepository;
    private final UsuarioRepository usuarioRepository;

    public HistorialRestController(HistorialRepository historialRepository, UploadsRepository uploadsRepository, UsuarioRepository usuarioRepository) {
        this.historialRepository = historialRepository;
        this.uploadsRepository = uploadsRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @DeleteMapping("/limpiar")
    public ResponseEntity<?> limpiarHistorial(Authentication authentication) {
        // Solo ADMINISTRADOR puede borrar el historial
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No autorizado");
        }
        
        String codigo = authentication.getName();
        Usuarios usuarioActual = usuarioRepository.findByCodigo(codigo).orElse(null);
        
        if (usuarioActual == null || !usuarioActual.getRol().equalsIgnoreCase("ADMINISTRADOR")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Solo administrador puede limpiar el historial");
        }
        
        historialRepository.deleteAll();
        uploadsRepository.deleteAllUploads();
        return ResponseEntity.ok().build();
    }
}
