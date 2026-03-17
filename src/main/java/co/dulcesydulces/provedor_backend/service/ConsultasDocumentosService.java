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

    public List<HistorialCargaView> buscarHistorial(String usuario, String archivo) {
        String usuarioFiltro = normalizarTexto(usuario);
        String archivoFiltro = normalizarTexto(archivo);

        List<Map<String, Object>> rows = jdbc.queryForList(
            """
                SELECT *
                FROM uploads
                WHERE (? IS NULL OR ? = '' OR LOWER(usuario) LIKE LOWER(CONCAT('%', ?, '%')))
                  AND (
                        ? IS NULL OR ? = ''
                        OR LOWER(COALESCE(nombre_egresos, '')) LIKE LOWER(CONCAT('%', ?, '%'))
                        OR LOWER(COALESCE(nombre_facturas, '')) LIKE LOWER(CONCAT('%', ?, '%'))
                        OR LOWER(COALESCE(nombre_notas, '')) LIKE LOWER(CONCAT('%', ?, '%'))
                  )
                ORDER BY 1 DESC
                LIMIT 200
            """,
            usuarioFiltro,
            usuarioFiltro,
            usuarioFiltro,
            archivoFiltro,
            archivoFiltro,
            archivoFiltro,
            archivoFiltro,
            archivoFiltro
        );

        return rows.stream()
            .map(this::mapHistorial)
            .toList();
    }

    private HistorialCargaView mapHistorial(Map<String, Object> row) {
        return new HistorialCargaView(
            toLong(firstValue(row, "upload_id", "id")),
            toText(firstValue(row, "usuario", "user")),
            toText(firstValue(row, "nombre_egresos", "archivo_egresos")),
            toText(firstValue(row, "nombre_facturas", "archivo_facturas")),
            toText(firstValue(row, "nombre_notas", "archivo_notas")),
            formatFechaCarga(firstValue(row, "creado_en", "created_at", "fecha_carga", "fecha"))
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
            return Long.parseLong(value.toString());
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