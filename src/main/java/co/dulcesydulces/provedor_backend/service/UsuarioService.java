package co.dulcesydulces.provedor_backend.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import co.dulcesydulces.provedor_backend.domain.entidades.Historial;
import co.dulcesydulces.provedor_backend.repository.HistorialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.dulcesydulces.provedor_backend.domain.entidades.Proveedores;
import co.dulcesydulces.provedor_backend.domain.entidades.Usuarios;
import co.dulcesydulces.provedor_backend.repository.ProveedoresRepository;
import co.dulcesydulces.provedor_backend.repository.UsuarioRepository;

@Service
public class UsuarioService {
    @Autowired
    private HistorialRepository historialRepository;

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

        // Registrar en historial
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String creador = (authentication != null) ? authentication.getName() : "anon";
            Usuarios usuarioCreador = usuarioRepository.findByCodigo(creador).orElse(null);
            if (usuarioCreador != null) {
                Historial h = new Historial();
                h.setUsuario(usuarioCreador);
                h.setFechaHora(java.time.LocalDateTime.now());
                h.setMovimiento("Creó el usuario '" + guardado.getCodigo() + "' (" + guardado.getNombreUsuario() + ")");
                historialRepository.save(h);
            }
        } catch (Exception ex) {
            // No bloquear creación si falla historial
        }

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
        // Obtener info del usuario a eliminar antes de borrarlo
        Usuarios eliminado = usuarioRepository.findById(codigo).orElse(null);
        usuarioRepository.deleteById(codigo);

        // Registrar en historial
        if (eliminado != null) {
            try {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                String eliminador = (authentication != null) ? authentication.getName() : "anon";
                Usuarios usuarioEliminador = usuarioRepository.findByCodigo(eliminador).orElse(null);
                if (usuarioEliminador != null) {
                    Historial h = new Historial();
                    h.setUsuario(usuarioEliminador);
                    h.setFechaHora(java.time.LocalDateTime.now());
                    h.setMovimiento("Eliminó el usuario '" + eliminado.getCodigo() + "' (" + eliminado.getNombreUsuario() + ")");
                    historialRepository.save(h);
                }
            } catch (Exception ex) {
                // No bloquear eliminación si falla historial
            }
        }
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

    @Transactional
    public Usuarios toggleEstado(String codigo) {
        Usuarios usuario = usuarioRepository.findById(codigo)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        String estadoAnterior = usuario.getEstado_u();
        String nuevoEstado;
        if ("inactivo".equalsIgnoreCase(estadoAnterior)) {
            usuario.setEstado_u("activo");
            nuevoEstado = "activó";
        } else {
            usuario.setEstado_u("inactivo");
            nuevoEstado = "inactivó";
        }
        Usuarios actualizado = usuarioRepository.save(usuario);
        sincronizarProveedor(actualizado);

        // Registrar en historial
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String actor = (authentication != null) ? authentication.getName() : "anon";
            Usuarios usuarioActor = usuarioRepository.findByCodigo(actor).orElse(null);
            if (usuarioActor != null) {
                Historial h = new Historial();
                h.setUsuario(usuarioActor);
                h.setFechaHora(java.time.LocalDateTime.now());
                h.setMovimiento(nuevoEstado + " el usuario '" + actualizado.getCodigo() + "' (" + actualizado.getNombreUsuario() + ")");
                historialRepository.save(h);
            }
        } catch (Exception ex) {
            // No bloquear si falla historial
        }

        return actualizado;
    }
}