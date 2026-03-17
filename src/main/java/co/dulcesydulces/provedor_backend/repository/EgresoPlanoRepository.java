package co.dulcesydulces.provedor_backend.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.dulcesydulces.provedor_backend.domain.dto.EgresoPlanoResumen;
import co.dulcesydulces.provedor_backend.domain.entidades.EgresoPlano;

public interface EgresoPlanoRepository extends JpaRepository<EgresoPlano, Long> {

    @Query("""
        SELECT 
            e.doctoEgreso AS doctoEgreso,
            MAX(e.fechaEgreso) AS fechaEgreso,
            MAX(e.tercero) AS tercero,
            SUM(e.vlrEgreso) AS vlrEgreso
        FROM EgresoPlano e
        WHERE (:tercero IS NULL OR :tercero = '' OR LOWER(e.tercero) LIKE LOWER(CONCAT('%', :tercero, '%')))
          AND (:numero IS NULL OR :numero = '' OR LOWER(e.doctoEgreso) LIKE LOWER(CONCAT('%', :numero, '%')))
          AND (:fecha IS NULL OR e.fechaEgreso = :fecha)
        GROUP BY e.doctoEgreso
        ORDER BY MAX(e.fechaEgreso) DESC, e.doctoEgreso DESC
    """)
    List<EgresoPlanoResumen> buscarConFiltros(
        @Param("tercero") String tercero,
        @Param("numero") String numero,
        @Param("fecha") LocalDate fecha
    );

    @Query("""
        SELECT 
            e.doctoEgreso AS doctoEgreso,
            MAX(e.fechaEgreso) AS fechaEgreso,
            MAX(e.tercero) AS tercero,
            SUM(e.vlrEgreso) AS vlrEgreso
        FROM EgresoPlano e
        WHERE LOWER(e.tercero) = LOWER(:tercero)
          AND (:numero IS NULL OR :numero = '' OR LOWER(e.doctoEgreso) LIKE LOWER(CONCAT('%', :numero, '%')))
          AND (:fecha IS NULL OR e.fechaEgreso = :fecha)
        GROUP BY e.doctoEgreso
        ORDER BY MAX(e.fechaEgreso) DESC, e.doctoEgreso DESC
    """)
    List<EgresoPlanoResumen> buscarPorTerceroYFiltros(
        @Param("tercero") String tercero,
        @Param("numero") String numero,
        @Param("fecha") LocalDate fecha
    );

    @Query("""
        SELECT e
        FROM EgresoPlano e
        WHERE e.doctoEgreso = :doctoEgreso
        ORDER BY e.doctoCausacion ASC
    """)
    List<EgresoPlano> buscarDetallePorDoctoEgreso(
        @Param("doctoEgreso") String doctoEgreso
    );
}