package co.dulcesydulces.provedor_backend.service;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import co.dulcesydulces.provedor_backend.domain.dto.ComprobanteEgresoView;
import co.dulcesydulces.provedor_backend.domain.dto.HistorialCargaView;

@Service
public class ConsultasDocumentosService {

    private static final DateTimeFormatter HISTORIAL_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JdbcTemplate jdbc;

    public ConsultasDocumentosService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<ComprobanteEgresoView> buscarEgresos(String proveedor, String numeroEgreso, LocalDate fecha) {
        String proveedorFiltro = normalizarTexto(proveedor);
        String numeroFiltro = normalizarTexto(numeroEgreso);

        return jdbc.query(
            """
                SELECT upload_id, docto_egreso, fecha_egreso, tercero, razon_social, vlr_egreso, notas
                FROM egresos_plano
                WHERE (? IS NULL OR ? = '' OR LOWER(razon_social) LIKE LOWER(CONCAT('%', ?, '%')))
                  AND (? IS NULL OR ? = '' OR LOWER(docto_egreso) LIKE LOWER(CONCAT('%', ?, '%')))
                  AND (? IS NULL OR fecha_egreso = ?)
                ORDER BY fecha_egreso DESC, docto_egreso DESC
            """,
            (rs, rowNum) -> new ComprobanteEgresoView(
                rs.getLong("upload_id"),
                rs.getString("docto_egreso"),
                rs.getDate("fecha_egreso") != null ? rs.getDate("fecha_egreso").toLocalDate() : null,
                rs.getString("tercero"),
                rs.getString("razon_social"),
                rs.getBigDecimal("vlr_egreso"),
                rs.getString("notas")
            ),
            proveedorFiltro,
            proveedorFiltro,
            proveedorFiltro,
            numeroFiltro,
            numeroFiltro,
            numeroFiltro,
            fecha,
            fecha
        );
    }

    public List<HistorialCargaView> buscarHistorial(String usuario, String fecha, String tipoMovimiento) {
        String usuarioFiltro = normalizarTexto(usuario);
        String fechaFiltro = (fecha != null && !fecha.isBlank()) ? fecha : null;
        String tipoFiltro = (tipoMovimiento != null && !tipoMovimiento.isBlank()) ? tipoMovimiento : null;

        // Filtros separados para uploads y movimientos de usuario
        String movimientoCondHistorial = "";
        String movimientoCondUploads = "";
        if (tipoFiltro != null) {
            switch (tipoFiltro) {
                case "creo":
                    // Solo movimientos de creación de usuario
                    movimientoCondHistorial = " AND h.movimiento LIKE 'Creó el usuario%' ";
                    movimientoCondUploads = " AND 1=0 ";
                    break;
                case "elimino":
                    movimientoCondHistorial = " AND h.movimiento LIKE 'Eliminó el usuario%' ";
                    movimientoCondUploads = " AND 1=0 ";
                    break;
                case "inactivo":
                    movimientoCondHistorial = " AND h.movimiento LIKE 'inactivó el usuario%' ";
                    movimientoCondUploads = " AND 1=0 ";
                    break;
                case "activo":
                    movimientoCondHistorial = " AND h.movimiento LIKE 'activó el usuario%' ";
                    movimientoCondUploads = " AND 1=0 ";
                    break;
                case "archivo":
                    movimientoCondUploads = " AND (nombre_egresos IS NOT NULL OR nombre_facturas IS NOT NULL OR nombre_notas IS NOT NULL) ";
                    movimientoCondHistorial = " AND 1=0 ";
                    break;
            }
        }

        String sql =
            "SELECT id as id, usuario, nombre_egresos, nombre_facturas, nombre_notas, creado_en as fecha, NULL as movimiento " +
            "FROM uploads " +
            "WHERE (? IS NULL OR ? = '' OR LOWER(usuario) LIKE LOWER(CONCAT('%', ?, '%'))) " +
            "  AND (? IS NULL OR DATE(creado_en) = ?) " +
            movimientoCondUploads +
            " UNION ALL " +
            "SELECT h.id as id, u.nombre_usuario as usuario, NULL as nombre_egresos, NULL as nombre_facturas, NULL as nombre_notas, h.fecha_hora as fecha, h.movimiento " +
            "FROM Historial h " +
            "JOIN Usuarios u ON h.usuario_codigo = u.codigo " +
            "WHERE (? IS NULL OR ? = '' OR LOWER(u.nombre_usuario) LIKE LOWER(CONCAT('%', ?, '%'))) " +
            "  AND (? IS NULL OR DATE(h.fecha_hora) = ?) " +
            movimientoCondHistorial;

        List<Map<String, Object>> rows = jdbc.queryForList(
            sql,
            usuarioFiltro,
            usuarioFiltro,
            usuarioFiltro,
            fechaFiltro,
            fechaFiltro,
            usuarioFiltro,
            usuarioFiltro,
            usuarioFiltro,
            fechaFiltro,
            fechaFiltro
        );

        // Ordenar por fecha descendente (ya que UNION ALL no garantiza el orden)
        return rows.stream()
            .map(this::mapHistorial)
            .sorted((a, b) -> b.getFechaCarga().compareTo(a.getFechaCarga()))
            .toList();
    }

    private HistorialCargaView mapHistorial(Map<String, Object> row) {
        return new HistorialCargaView(
            toLong(firstValue(row, "id")),
            toText(firstValue(row, "usuario")),
            toText(firstValue(row, "nombre_egresos")),
            toText(firstValue(row, "nombre_facturas")),
            toText(firstValue(row, "nombre_notas")),
            formatFechaCarga(firstValue(row, "fecha")),
            toText(firstValue(row, "movimiento"))
        );
    }

    private Object firstValue(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object value = row.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String formatFechaCarga(Object value) {
        if (value == null) {
            return "Sin fecha";
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().format(HISTORIAL_FORMATTER);
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.format(HISTORIAL_FORMATTER);
        }
        if (value instanceof Date date) {
            return date.toLocalDate().toString();
        }
        if (value instanceof LocalDate localDate) {
            return localDate.toString();
        }
        return value.toString();
    }

    private Long toLong(Object value) {
    if (value instanceof Number number) {
        return number.longValue();
    }
    if (value == null) {
        return null;
    }
    try {
        return Long.valueOf(value.toString());
    } catch (NumberFormatException ex) {
        return null;
    }
}

    private String toText(Object value) {
        if (value == null) {
            return "-";
        }
        String text = value.toString().trim();
        return text.isEmpty() ? "-" : text;
    }

    private String normalizarTexto(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}