package co.dulcesydulces.provedor_backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.dulcesydulces.provedor_backend.domain.entidades.NotaPlano;

public interface NotaPlanoRepository extends JpaRepository<NotaPlano, Long> {

    Optional<NotaPlano> findFirstByDoctoProveedor(String doctoProveedor);

    List<NotaPlano> findByDoctoProveedorIn(Collection<String> doctosProveedor);
}
