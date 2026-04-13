package co.dulcesydulces.provedor_backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.dulcesydulces.provedor_backend.domain.entidades.NotaPlano;

public interface NotaPlanoRepository extends JpaRepository<NotaPlano, Long> {

    Optional<NotaPlano> findFirstByDoctoProveedor(String doctoProveedor);

    List<NotaPlano> findByDoctoProveedorIn(Collection<String> doctosProveedor);

    Optional<NotaPlano> findFirstByReferencia1(String referencia1);

    @Query("""
        SELECT n
        FROM NotaPlano n
        WHERE UPPER(REPLACE(REPLACE(REPLACE(n.nroDocumento, '-', ''), ' ', ''), '.', '')) IN :valoresNormalizados
        ORDER BY n.id ASC
    """)
    List<NotaPlano> buscarPorNroDocumentoNormalizadoIn(
            @Param("valoresNormalizados") Collection<String> valoresNormalizados
    );

    @Query("""
        SELECT n
        FROM NotaPlano n
        WHERE UPPER(REPLACE(REPLACE(REPLACE(n.referencia1, '-', ''), ' ', ''), '.', '')) IN :valoresNormalizados
        ORDER BY n.id ASC
    """)
    List<NotaPlano> buscarPorReferencia1NormalizadaIn(
            @Param("valoresNormalizados") Collection<String> valoresNormalizados
    );

    @Query("""
        SELECT n
        FROM NotaPlano n
        WHERE UPPER(REPLACE(REPLACE(REPLACE(n.doctoProveedor, '-', ''), ' ', ''), '.', '')) = UPPER(:ndNormalizado)
    """)
    List<NotaPlano> buscarPorNdNormalizado(
            @Param("ndNormalizado") String ndNormalizado
    );
}