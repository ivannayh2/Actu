package co.dulcesydulces.provedor_backend.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.dulcesydulces.provedor_backend.domain.entidades.Proveedores;
import co.dulcesydulces.provedor_backend.domain.entidades.Usuarios;
import co.dulcesydulces.provedor_backend.repository.ProveedoresRepository;
import co.dulcesydulces.provedor_backend.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ProveedoresRepository proveedoresRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            ProveedoresRepository proveedoresRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarioRepository = usuarioRepository;
        this.proveedoresRepository = proveedoresRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Usuarios> listar() {
        return usuarioRepository.findAllByOrderByRolAscNombreUsuarioAsc();
    }

    public Map<String, List<Usuarios>> listarPorRol() {
        return listar().stream()
                .collect(Collectors.groupingBy(Usuarios::getRol));
    }

    @Transactional
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
        // Permisos: ya vienen en el objeto u (si el frontend los envía)
        return repo.save(u);
    }

    public Usuarios actualizar(String codigo, Usuarios changes) {
        Usuarios u = repo.findById(codigo)
                .orElseThrow(() -> new NoSuchElementException("No existe"));
        u.setRol(normalizarRol(changes.getRol()));
        u.setNombreUsuario(changes.getNombreUsuario());
        if (changes.getPermisos() != null) {
            u.setPermisos(changes.getPermisos());
        }
        if (changes.getPassword_hash() != null && !changes.getPassword_hash().isBlank()) {
            u.setPassword_hash(encoder.encode(changes.getPassword_hash()));
        }
        return repo.save(u);
    }

    @Transactional
    public void eliminar(String codigo) {
        usuarioRepository.deleteById(codigo);
    }

    private void sincronizarProveedor(Usuarios usuario) {
        boolean esProveedor = "PROVEEDORES".equalsIgnoreCase(usuario.getRol())
                || "PROVEEDOR".equalsIgnoreCase(usuario.getRol());

        boolean existeEnProveedores = proveedoresRepository.existsById(usuario.getCodigo());

        if (esProveedor && !existeEnProveedores) {
            Proveedores proveedor = new Proveedores();
            proveedor.setUsuarioCodigo(usuario.getCodigo());
            proveedoresRepository.save(proveedor);
        }

        if (!esProveedor && existeEnProveedores) {
            proveedoresRepository.deleteById(usuario.getCodigo());
        }
    }
}