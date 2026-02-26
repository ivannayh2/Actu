package co.dulcesydulces.provedor_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.dulcesydulces.provedor_backend.domain.entidades.Usuarios;

// OJO: aquí pon tu entidad que ya exista, por ejemplo Usuario


public interface UsuarioModuloRepo extends JpaRepository<Usuarios, String> {

  @Query(value = """
      SELECT m.codigo
      FROM usuario_modulo um
      JOIN modulos m ON m.id = um.modulo_id
      WHERE um.usuario_codigo = :codigo
        AND um.permitido = 1
      """, nativeQuery = true)
  List<String> codigosPermitidos(@Param("codigo") String codigo);
}
