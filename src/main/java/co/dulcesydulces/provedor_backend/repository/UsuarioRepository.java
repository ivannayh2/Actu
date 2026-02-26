package co.dulcesydulces.provedor_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.dulcesydulces.provedor_backend.domain.entidades.Usuarios;

public interface UsuarioRepository extends JpaRepository<Usuarios, String> {

    Optional<Usuarios> findByCodigo(String codigo);

    Optional<Usuarios> findByEmail(String email);   // ✅ NUEVO

    List<Usuarios> findAllByOrderByRolAscNombreUsuarioAsc();
}
