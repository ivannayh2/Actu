package co.dulcesydulces.provedor_backend.controller;

import java.time.LocalDateTime;

import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import co.dulcesydulces.provedor_backend.domain.entidades.Historial;
import co.dulcesydulces.provedor_backend.domain.entidades.Usuarios;
import co.dulcesydulces.provedor_backend.repository.HistorialRepository;
import co.dulcesydulces.provedor_backend.repository.UsuarioRepository;
import co.dulcesydulces.provedor_backend.service.PlanosUploadService;

@Controller
@RequestMapping("/uploads")
public class UploadsController {

    private final PlanosUploadService planosUploadService;
    private final UsuarioRepository usuarioRepository;
    private final HistorialRepository historialRepository;

    public UploadsController(
            PlanosUploadService planosUploadService,
            UsuarioRepository usuarioRepository,
            HistorialRepository historialRepository
    ) {
        this.planosUploadService = planosUploadService;
        this.usuarioRepository = usuarioRepository;
        this.historialRepository = historialRepository;
    }

    @PostMapping("/planos")
    public String uploadPlanos(
            @RequestParam("egresosFile") MultipartFile egresosFile,
            @RequestParam("facturasFile") MultipartFile facturasFile,
            @RequestParam("notasFile") MultipartFile notasFile,
            Authentication auth,
            Model model
    ) {
        try {
            Authentication currentAuth = auth != null ? auth : SecurityContextHolder.getContext().getAuthentication();
            String codigo = (currentAuth != null) ? currentAuth.getName() : null;

            String usuario = usuarioRepository.findByCodigo(codigo)
                .map(u -> u.getNombreUsuario() != null && !u.getNombreUsuario().isBlank() ? u.getNombreUsuario() : u.getCodigo())
                .orElseGet(() -> (codigo != null && !codigo.isBlank() && !"anonymousUser".equalsIgnoreCase(codigo))
                    ? codigo
                    : "system");

            long uploadId = planosUploadService.procesar(egresosFile, facturasFile, notasFile, usuario);
            model.addAttribute("msg", "Archivos procesados OK. upload_id=" + uploadId);
        } catch (Exception e) {
            model.addAttribute("msg", "Error procesando archivos: " + e.getMessage());
        }
        return "home"; // o el nombre real de tu template
    }

    @PostMapping("/planos/eliminar-todo")
    public String eliminarTodoLoImportado(Authentication auth, Model model) {
        try {
            int total = planosUploadService.eliminarTodoLoImportado();

            try {
                Authentication currentAuth = auth != null ? auth : SecurityContextHolder.getContext().getAuthentication();
                String codigo = (currentAuth != null) ? currentAuth.getName() : null;
                Usuarios usuarioActor = usuarioRepository.findByCodigo(codigo).orElse(null);

                if (usuarioActor != null) {
                    Historial h = new Historial();
                    h.setUsuario(usuarioActor);
                    h.setFechaHora(LocalDateTime.now());
                    h.setMovimiento("Eliminó archivos importados (" + total + " registros).");
                    historialRepository.save(h);
                }
            } catch (Exception ignored) {
                // No bloquear eliminación si falla el registro en historial.
            }

            model.addAttribute("msg", "Se eliminaron " + total + " registros importados.");
        } catch (Exception e) {
            model.addAttribute("msg", "Error eliminando registros importados: " + e.getMessage());
        }
        return "home";
    }
}