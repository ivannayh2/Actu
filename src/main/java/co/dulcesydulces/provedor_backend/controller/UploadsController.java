package co.dulcesydulces.provedor_backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import co.dulcesydulces.provedor_backend.service.PlanosUploadService;

@Controller
@RequestMapping("/uploads")
public class UploadsController {

    private final PlanosUploadService planosUploadService;

    public UploadsController(PlanosUploadService planosUploadService) {
        this.planosUploadService = planosUploadService;
    }

    @PostMapping("/planos")
    public String uploadPlanos(
            @RequestParam("egresosFile") MultipartFile egresosFile,
            @RequestParam("facturasFile") MultipartFile facturasFile,
            @RequestParam("notasFile") MultipartFile notasFile,
            Model model
    ) {
        try {
            long uploadId = planosUploadService.procesar(egresosFile, facturasFile, notasFile);
            model.addAttribute("msg", "Archivos procesados OK. upload_id=" + uploadId);
        } catch (Exception e) {
            model.addAttribute("msg", "Error procesando archivos: " + e.getMessage());
        }
        return "home"; // o el nombre real de tu template
    }

    @PostMapping("/planos/eliminar-todo")
    public String eliminarTodoLoImportado(Model model) {
        try {
            int total = planosUploadService.eliminarTodoLoImportado();
            model.addAttribute("msg", "Se eliminaron " + total + " registros importados.");
        } catch (Exception e) {
            model.addAttribute("msg", "Error eliminando registros importados: " + e.getMessage());
        }
        return "home";
    }
}