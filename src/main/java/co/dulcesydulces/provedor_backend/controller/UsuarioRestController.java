package co.dulcesydulces.provedor_backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.dulcesydulces.provedor_backend.domain.entidades.Usuarios;
import co.dulcesydulces.provedor_backend.service.UsuarioService;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioRestController {

    private final UsuarioService service;

    public UsuarioRestController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public Map<String, Object> list() {
        return Map.of("users", service.listar());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Usuarios u) {
        return ResponseEntity.ok(service.crear(u));
    }

    @PutMapping("/{codigo}")
    public ResponseEntity<?> update(@PathVariable String codigo, @RequestBody Usuarios u) {
        return ResponseEntity.ok(service.actualizar(codigo, u));
    }

    @DeleteMapping("/{codigo}")
    public ResponseEntity<?> delete(@PathVariable String codigo) {
        service.eliminar(codigo);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
