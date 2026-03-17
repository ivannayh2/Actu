package co.dulcesydulces.provedor_backend.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.dulcesydulces.provedor_backend.domain.entidades.Egreso;

public interface EgresoRepository extends JpaRepository<Egreso, String> {

    @Query("""
        SELECT e
        FROM Egreso e
        WHERE (:proveedor IS NULL OR :proveedor = '' OR LOWER(e.proveedor) LIKE LOWER(CONCAT('%', :proveedor, '%')))
          AND (:numero IS NULL OR :numero = '' OR LOWER(e.numeroEgreso) LIKE LOWER(CONCAT('%', :numero, '%')))
          AND (:fecha IS NULL OR e.fechaDocumento = :fecha)
        ORDER BY e.fechaDocumento DESC, e.numeroEgreso DESC
    """)
    List<Egreso> buscar(
        @Param("proveedor") String proveedor,
        @Param("numero") String numero,
        @Param("fecha") LocalDate fecha
 
    );

    
}


