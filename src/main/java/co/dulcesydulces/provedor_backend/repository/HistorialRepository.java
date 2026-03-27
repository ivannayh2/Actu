package co.dulcesydulces.provedor_backend.repository;

import co.dulcesydulces.provedor_backend.domain.entidades.Historial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistorialRepository extends JpaRepository<Historial, Long> {
}
