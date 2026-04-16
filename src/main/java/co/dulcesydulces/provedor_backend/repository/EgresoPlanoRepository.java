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
            MAX(e.doctoSa) AS doctoSa,
            MAX(e.fechaEgreso) AS fechaEgreso,
            MAX(e.tercero) AS tercero,
            MAX(e.razonSocial) AS razonSocial,
            MAX(e.doctoCausacion) AS doctoCausacion,
            SUM(e.vlrEgreso) AS vlrEgreso,
            SUM(e.prontoPago) AS prontoPago,
            MAX(e.valorDocto) AS valorDocto
        FROM EgresoPlano e
         WHERE (:tercero IS NULL OR :tercero = '' OR
             LOWER(e.tercero) LIKE LOWER(CONCAT('%', :tercero, '%')) OR
             LOWER(e.razonSocial) LIKE LOWER(CONCAT('%', :tercero, '%')))
          AND (:numero IS NULL OR :numero = '' OR LOWER(e.doctoEgreso) LIKE LOWER(CONCAT('%', :numero, '%')))
          AND (:doctoSa IS NULL OR :doctoSa = '' OR LOWER(e.doctoSa) LIKE LOWER(CONCAT('%', :doctoSa, '%')))
          AND (:fecha IS NULL OR e.fechaEgreso = :fecha)
        GROUP BY e.doctoEgreso
        ORDER BY MAX(e.fechaEgreso) DESC, e.doctoEgreso DESC
    """)
    List<EgresoPlanoResumen> buscarConFiltros(
        @Param("tercero") String tercero,
        @Param("numero") String numero,
        @Param("doctoSa") String doctoSa,
        @Param("fecha") LocalDate fecha
    );

    @Query("""
        SELECT 
            e.doctoEgreso AS doctoEgreso,
            MAX(e.doctoSa) AS doctoSa,
            MAX(e.fechaEgreso) AS fechaEgreso,
            MAX(e.tercero) AS tercero,
            MAX(e.razonSocial) AS razonSocial,
            MAX(e.doctoCausacion) AS doctoCausacion,
            SUM(e.vlrEgreso) AS vlrEgreso,
            SUM(e.prontoPago) AS prontoPago,
            MAX(e.valorDocto) AS valorDocto
        FROM EgresoPlano e
        WHERE LOWER(e.tercero) = LOWER(:tercero)
          AND (:numero IS NULL OR :numero = '' OR LOWER(e.doctoEgreso) LIKE LOWER(CONCAT('%', :numero, '%')))
          AND (:doctoSa IS NULL OR :doctoSa = '' OR LOWER(e.doctoSa) LIKE LOWER(CONCAT('%', :doctoSa, '%')))
          AND (:fecha IS NULL OR e.fechaEgreso = :fecha)
        GROUP BY e.doctoEgreso
        ORDER BY MAX(e.fechaEgreso) DESC, e.doctoEgreso DESC
    """)
    List<EgresoPlanoResumen> buscarPorTerceroYFiltros(
        @Param("tercero") String tercero,
        @Param("numero") String numero,
        @Param("doctoSa") String doctoSa,
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

    @Query("""
        SELECT e
        FROM EgresoPlano e
         WHERE (:tercero IS NULL OR :tercero = '' OR
             LOWER(e.tercero) LIKE LOWER(CONCAT('%', :tercero, '%')) OR
             LOWER(e.razonSocial) LIKE LOWER(CONCAT('%', :tercero, '%')))
          AND (:numero IS NULL OR :numero = '' OR LOWER(e.doctoEgreso) LIKE LOWER(CONCAT('%', :numero, '%')))
          AND (:doctoSa IS NULL OR :doctoSa = '' OR LOWER(e.doctoSa) LIKE LOWER(CONCAT('%', :doctoSa, '%')))
          AND (:fecha IS NULL OR e.fechaEgreso = :fecha)
        ORDER BY e.fechaEgreso DESC, e.doctoEgreso DESC, e.doctoSa DESC, e.doctoCausacion ASC
    """)
    List<EgresoPlano> buscarDetallesConFiltros(
        @Param("tercero") String tercero,
        @Param("numero") String numero,
        @Param("doctoSa") String doctoSa,
        @Param("fecha") LocalDate fecha
    );

    @Query("""
        SELECT e
        FROM EgresoPlano e
        WHERE LOWER(e.tercero) = LOWER(:tercero)
          AND (:numero IS NULL OR :numero = '' OR LOWER(e.doctoEgreso) LIKE LOWER(CONCAT('%', :numero, '%')))
          AND (:doctoSa IS NULL OR :doctoSa = '' OR LOWER(e.doctoSa) LIKE LOWER(CONCAT('%', :doctoSa, '%')))
          AND (:fecha IS NULL OR e.fechaEgreso = :fecha)
        ORDER BY e.fechaEgreso DESC, e.doctoEgreso DESC, e.doctoSa DESC, e.doctoCausacion ASC
    """)
    List<EgresoPlano> buscarDetallesPorTerceroYFiltros(
        @Param("tercero") String tercero,
        @Param("numero") String numero,
        @Param("doctoSa") String doctoSa,
        @Param("fecha") LocalDate fecha
    );

    @Query("""
        SELECT e
        FROM EgresoPlano e
        WHERE (:documento IS NULL OR :documento = '' OR
               UPPER(REPLACE(REPLACE(REPLACE(e.doctoEgreso, '-', ''), ' ', ''), '.', '')) LIKE CONCAT('%', UPPER(:documento), '%'))
          AND (:doctoCausacion IS NULL OR :doctoCausacion = '' OR
               UPPER(REPLACE(REPLACE(REPLACE(e.doctoCausacion, '-', ''), ' ', ''), '.', '')) LIKE CONCAT('%', UPPER(:doctoCausacion), '%'))
    """)
    List<EgresoPlano> buscarNotasParaDetalle(
        @Param("documento") String documento,
        @Param("doctoCausacion") String doctoCausacion
    );

    @Query("""
        SELECT e
        FROM EgresoPlano e
        WHERE UPPER(REPLACE(REPLACE(REPLACE(e.doctoSa, '-', ''), ' ', ''), '.', '')) = UPPER(:doctoSaNormalizado)
    """)
    List<EgresoPlano> buscarNotasPorDoctoSaNormalizado(
        @Param("doctoSaNormalizado") String doctoSaNormalizado
    );

    @Query("""
    SELECT e
    FROM EgresoPlano e
    WHERE UPPER(TRIM(e.doctoCausacion)) = UPPER(TRIM(:doctoCausacion))
""")
List<EgresoPlano> buscarDetallePorDoctoCausacion(@Param("doctoCausacion") String doctoCausacion);

    boolean existsByDoctoEgreso(String doctoEgreso);
}