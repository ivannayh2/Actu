package co.dulcesydulces.provedor_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.dulcesydulces.provedor_backend.domain.entidades.NotaPlano;

public interface NotaPlanoRepository extends JpaRepository<NotaPlano, Long> {

    @Query(value = """
        SELECT n.*
        FROM notas_plano n
        WHERE :ndNormalizado IS NOT NULL
          AND :ndNormalizado <> ''
          AND UPPER(REPLACE(REPLACE(REPLACE(TRIM(COALESCE(n.nro_documento, '')), '-', ''), ' ', ''), '.', '')) = :ndNormalizado
        ORDER BY n.creado_en ASC, n.id ASC
        """, nativeQuery = true)
    List<NotaPlano> buscarPorNdNormalizado(@Param("ndNormalizado") String ndNormalizado);
}
