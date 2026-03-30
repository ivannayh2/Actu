
package co.dulcesydulces.provedor_backend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UploadsRepository {

    private final JdbcTemplate jdbc;

    public UploadsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void deleteAllUploads() {
        jdbc.update("DELETE FROM uploads");
    }

    public long crearUpload(String usuario, String egresosName, String facturasName, String notasName) {
        jdbc.update("""
            INSERT INTO uploads (usuario, nombre_egresos, nombre_facturas, nombre_notas)
            VALUES (?, ?, ?, ?)
        """, usuario, egresosName, facturasName, notasName);

        // MySQL: forma rápida de obtener el último ID en esta conexión
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (id == null) throw new IllegalStateException("No se pudo obtener upload_id");
        return id;
    }
}