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
        WHERE (:tercero IS NULL OR :tercero = '' OR LOWER(e.tercero) LIKE LOWER(CONCAT('%', :tercero, '%')))
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
        WHERE (:tercero IS NULL OR :tercero = '' OR LOWER(e.tercero) LIKE LOWER(CONCAT('%', :tercero, '%')))
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

    @Query(value = """
        SELECT e.*
        FROM egresos_plano e
        WHERE e.notas IS NOT NULL
          AND TRIM(e.notas) <> ''
          AND (
            (:docNormalizado IS NOT NULL AND :docNormalizado <> '' AND (
                UPPER(REPLACE(REPLACE(REPLACE(TRIM(COALESCE(e.docto_sa, '')), '-', ''), ' ', ''), '.', '')) = :docNormalizado
                OR UPPER(REPLACE(REPLACE(REPLACE(TRIM(COALESCE(e.docto_causacion, '')), '-', ''), ' ', ''), '.', '')) = :docNormalizado
            ))
            OR
            (:causacionNormalizada IS NOT NULL AND :causacionNormalizada <> '' AND (
                UPPER(REPLACE(REPLACE(REPLACE(TRIM(COALESCE(e.docto_sa, '')), '-', ''), ' ', ''), '.', '')) = :causacionNormalizada
                OR UPPER(REPLACE(REPLACE(REPLACE(TRIM(COALESCE(e.docto_causacion, '')), '-', ''), ' ', ''), '.', '')) = :causacionNormalizada
            ))
          )
        ORDER BY e.fecha_egreso DESC, e.id DESC
    """, nativeQuery = true)
    List<EgresoPlano> buscarNotasParaDetalle(
        @Param("docNormalizado") String docNormalizado,
        @Param("causacionNormalizada") String causacionNormalizada
    );

    @Query(value = """
        SELECT e.*
        FROM egresos_plano e
        WHERE :doctoSaNormalizado IS NOT NULL
          AND :doctoSaNormalizado <> ''
          AND e.notas IS NOT NULL
          AND TRIM(e.notas) <> ''
          AND UPPER(REPLACE(REPLACE(REPLACE(TRIM(COALESCE(e.docto_sa, '')), '-', ''), ' ', ''), '.', '')) = :doctoSaNormalizado
        ORDER BY e.fecha_egreso DESC, e.id DESC
    """, nativeQuery = true)
    List<EgresoPlano> buscarNotasPorDoctoSaNormalizado(
        @Param("doctoSaNormalizado") String doctoSaNormalizado
    );

    boolean existsByDoctoEgreso(String doctoEgreso);
}