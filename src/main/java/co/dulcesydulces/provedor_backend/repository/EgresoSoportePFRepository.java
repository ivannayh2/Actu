package co.dulcesydulces.provedor_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import co.dulcesydulces.provedor_backend.domain.entidades.EgresoSoportePF;

@Repository
public interface EgresoSoportePFRepository extends JpaRepository<EgresoSoportePF, Long> {

    List<EgresoSoportePF> findByEgresoId(Long egresoId);
}