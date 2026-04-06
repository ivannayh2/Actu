
package co.dulcesydulces.provedor_backend.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

@Repository
public class UploadsRepository {

    private final JdbcTemplate jdbc;

    public UploadsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long crearUpload(String usuario, String egresosName, String facturasName, String notasName) {
        String usuarioFinal = resolveUsuarioFinal(usuario);

        jdbc.update("""
            INSERT INTO uploads (usuario, nombre_egresos, nombre_facturas, nombre_notas)
            VALUES (?, ?, ?, ?)
        """, usuarioFinal, egresosName, facturasName, notasName);

        // MySQL: forma rápida de obtener el último ID en esta conexión
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (id == null) throw new IllegalStateException("No se pudo obtener upload_id");
        return id;
    }

    private String resolveUsuarioFinal(String usuarioParam) {
        if (usuarioParam != null && !usuarioParam.isBlank() && !"system".equalsIgnoreCase(usuarioParam)) {
            return usuarioParam;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "system";
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            String username = userDetails.getUsername();
            if (username != null && !username.isBlank() && !"anonymousUser".equalsIgnoreCase(username)) {
                return username;
            }
        }

        String authName = auth.getName();
        if (authName != null && !authName.isBlank() && !"anonymousUser".equalsIgnoreCase(authName)) {
            return authName;
        }

        return "system";
    }

    public void deleteAllUploads() {
        jdbc.update("DELETE FROM uploads");
    }

    public int deleteAllImportedData() {
        int total = 0;
        total += jdbc.update("DELETE FROM egresos_plano");
        total += jdbc.update("DELETE FROM facturas_plano");
        total += jdbc.update("DELETE FROM notas_plano");
        total += jdbc.update("DELETE FROM uploads");
        return total;
    }
}