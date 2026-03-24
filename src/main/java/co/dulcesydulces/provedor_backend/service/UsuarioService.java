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
        if (u.getPassword_hash() != null && !u.getPassword_hash().startsWith("$2a$")) {
            u.setPassword_hash(passwordEncoder.encode(u.getPassword_hash()));
        }

        Usuarios guardado = usuarioRepository.save(u);
        sincronizarProveedor(guardado);

        return guardado;
    }

    @Transactional
    public Usuarios actualizar(String codigo, Usuarios u) {
        Usuarios actual = usuarioRepository.findById(codigo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        actual.setNombreUsuario(u.getNombreUsuario());
        actual.setEmail(u.getEmail());
        actual.setRol(u.getRol());
        actual.setEstado_u(u.getEstado_u());
        actual.setRoleId(u.getRoleId());

        if (u.getPassword_hash() != null && !u.getPassword_hash().isBlank()) {
            if (u.getPassword_hash().startsWith("$2a$")) {
                actual.setPassword_hash(u.getPassword_hash());
            } else {
                actual.setPassword_hash(passwordEncoder.encode(u.getPassword_hash()));
            }
        }

        Usuarios actualizado = usuarioRepository.save(actual);
        sincronizarProveedor(actualizado);

        return actualizado;
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