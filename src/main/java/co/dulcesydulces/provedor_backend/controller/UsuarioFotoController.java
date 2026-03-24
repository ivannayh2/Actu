package co.dulcesydulces.provedor_backend.controller;

import co.dulcesydulces.provedor_backend.domain.entidades.Usuarios;
import co.dulcesydulces.provedor_backend.repository.UsuarioRepository;
import co.dulcesydulces.provedor_backend.service.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios/{codigo}/foto")
public class UsuarioFotoController {
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private StorageService storageService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMINISTRADOR','PUBLICADOR','PROVEEDORES')")
    public ResponseEntity<?> uploadFoto(@PathVariable String codigo, @RequestParam("file") MultipartFile file) throws IOException {
        Optional<Usuarios> opt = usuarioRepository.findByCodigo(codigo);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Usuarios usuario = opt.get();
        // Eliminar foto anterior si existe
        if (usuario.getFotoPerfil() != null) {
            storageService.deleteProfileImage(usuario.getFotoPerfil(), codigo);
        }
        String filename = storageService.storeProfileImage(file, codigo);
        usuario.setFotoPerfil(filename);
        usuarioRepository.save(usuario);
        String url = storageService.getProfileImageUrl(filename, codigo);
        return ResponseEntity.ok().body(java.util.Map.of("url", url));
    }

    @DeleteMapping
    @PreAuthorize("hasAnyAuthority('ADMINISTRADOR','PUBLICADOR','PROVEEDORES')")
    public ResponseEntity<?> deleteFoto(@PathVariable String codigo) {
        Optional<Usuarios> opt = usuarioRepository.findByCodigo(codigo);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Usuarios usuario = opt.get();
        if (usuario.getFotoPerfil() != null) {
            storageService.deleteProfileImage(usuario.getFotoPerfil(), codigo);
            usuario.setFotoPerfil(null);
            usuarioRepository.save(usuario);
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMINISTRADOR','PUBLICADOR','PROVEEDORES')")
    public ResponseEntity<?> getFotoUrl(@PathVariable String codigo) {
        Optional<Usuarios> opt = usuarioRepository.findByCodigo(codigo);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Usuarios usuario = opt.get();
        String url = usuario.getFotoPerfil() != null ? storageService.getProfileImageUrl(usuario.getFotoPerfil(), codigo) : null;
        return ResponseEntity.ok().body(java.util.Map.of("url", url));
    }
}
