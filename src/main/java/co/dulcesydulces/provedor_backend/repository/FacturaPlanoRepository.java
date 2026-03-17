package co.dulcesydulces.provedor_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.dulcesydulces.provedor_backend.domain.entidades.FacturaPlano;

public interface FacturaPlanoRepository extends JpaRepository<FacturaPlano, Long> {

    @Query("""
        SELECT f
        FROM FacturaPlano f
        WHERE f.doctoCausacion = :doctoCausacion
        ORDER BY f.doctoCausacion ASC
    """)
    List<FacturaPlano> buscarPorDoctoCausacion(@Param("doctoCausacion") String doctoCausacion);
}