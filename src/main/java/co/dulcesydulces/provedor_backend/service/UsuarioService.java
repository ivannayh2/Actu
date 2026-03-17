package co.dulcesydulces.provedor_backend.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import co.dulcesydulces.provedor_backend.domain.entidades.Usuarios;
import co.dulcesydulces.provedor_backend.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository repo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UsuarioService(UsuarioRepository repo) {
        this.repo = repo;
    }

    public List<Usuarios> listar() {
        return repo.findAllByOrderByRolAscNombreUsuarioAsc();
    }

    public Map<String, List<Usuarios>> listarPorRol() {
        Map<String, List<Usuarios>> map = new LinkedHashMap<>();
        map.put("ADMINISTRADOR", new ArrayList<>());
        map.put("PUBLICADOR", new ArrayList<>());
        map.put("PROVEEDORES", new ArrayList<>());

        for (Usuarios u : listar()) {
            String r = normalizarRol(u.getRol());
            map.computeIfAbsent(r, k -> new ArrayList<>()).add(u);
        }
        return map;
    }

    public Usuarios crear(Usuarios u) {
        if (u.getCodigo() == null || u.getCodigo().isBlank()) {
            throw new IllegalArgumentException("El código es obligatorio");
        }

        if (repo.existsById(u.getCodigo())) {
            throw new IllegalArgumentException("Ya existe un usuario con ese código");
        }

        if (u.getNombreUsuario() == null || u.getNombreUsuario().isBlank()) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio");
        }

        u.setRol(normalizarRol(u.getRol()));

        if (u.getPassword_hash() == null || u.getPassword_hash().isBlank()) {
            throw new IllegalArgumentException("La clave es obligatoria");
        }

        u.setPassword_hash(encoder.encode(u.getPassword_hash()));
        return repo.save(u);
    }

    public Usuarios actualizar(String codigo, Usuarios changes) {
        Usuarios u = repo.findById(codigo)
                .orElseThrow(() -> new NoSuchElementException("No existe"));

        u.setRol(normalizarRol(changes.getRol()));
        u.setNombreUsuario(changes.getNombreUsuario());

        if (changes.getPassword_hash() != null && !changes.getPassword_hash().isBlank()) {
            u.setPassword_hash(encoder.encode(changes.getPassword_hash()));
        }

        return repo.save(u);
    }

    public void eliminar(String codigo) {
        repo.deleteById(codigo);
    }

    private String normalizarRol(String rol) {
        String r = (rol == null ? "" : rol.trim().toUpperCase());

        if (r.equals("ADMINISTRADOR")) return "ADMINISTRADOR";
        if (r.equals("PUBLICADOR")) return "PUBLICADOR";
        if (r.equals("PROVEEDOR") || r.equals("PROVEEDORES")) return "PROVEEDORES";

        return "PUBLICADOR";
    }
}