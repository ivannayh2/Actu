package co.dulcesydulces.provedor_backend.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import co.dulcesydulces.provedor_backend.repository.UploadsRepository;

@Service
public class PlanosUploadService {

    private final UploadsRepository uploadsRepository;
    private final JdbcTemplate jdbc;

    private static final String SEP = "\t";

    public PlanosUploadService(UploadsRepository uploadsRepository, JdbcTemplate jdbc) {
        this.uploadsRepository = uploadsRepository;
        this.jdbc = jdbc;
    }

    @Transactional
    public long procesar(MultipartFile egresos, MultipartFile facturas, MultipartFile notas) throws Exception {

        if ((egresos == null || egresos.isEmpty()) &&
            (facturas == null || facturas.isEmpty()) &&
            (notas == null || notas.isEmpty())) {
            return -1L;
        }

        if (egresos != null && !egresos.isEmpty()) {
            validarTxt(egresos);
        }
        if (facturas != null && !facturas.isEmpty()) {
            validarTxt(facturas);
        }
        if (notas != null && !notas.isEmpty()) {
            validarTxt(notas);
        }

        long uploadId = uploadsRepository.crearUpload(
            egresos != null && !egresos.isEmpty() ? egresos.getOriginalFilename() : null,
            facturas != null && !facturas.isEmpty() ? facturas.getOriginalFilename() : null,
            notas != null && !notas.isEmpty() ? notas.getOriginalFilename() : null
        );

        if (egresos != null && !egresos.isEmpty()) {
            importarEgresos(uploadId, egresos);
        }
        if (facturas != null && !facturas.isEmpty()) {
            importarFacturas(uploadId, facturas);
        }
        if (notas != null && !notas.isEmpty()) {
            importarNotas(uploadId, notas);
        }

        return uploadId;
    }

    @Transactional
    public int eliminarTodoLoImportado() {
        return uploadsRepository.deleteAllImportedData();
    }

    private void validarTxt(MultipartFile f) {
        if (f == null || f.isEmpty()) {
            throw new IllegalArgumentException("Archivo vacío.");
        }

        String name = (f.getOriginalFilename() == null) ? "" : f.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".txt")) {
            throw new IllegalArgumentException("Solo se permiten .txt: " + f.getOriginalFilename());
        }
    }

    // =========================
    // EGRESOS (11 columnas)
    // =========================
    private void importarEgresos(long uploadId, MultipartFile file) throws Exception {
        List<Object[]> batch = new ArrayList<>(2000);

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            int lineNo = 0;
            boolean headerSkipped = false;

            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) continue;

                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }

                if (line.trim().toLowerCase().startsWith("gran total")) {
                    continue;
                }

                String[] c = line.split(SEP, -1);

                if (c.length < 11) {
                    throw new IllegalArgumentException(
                        "Egresos: línea " + lineNo + " tiene " + c.length +
                        " columnas, se esperaban 11. Línea: " + line
                    );
                }

                String doctoEgreso = c[0].trim().replaceFirst("^[^-]+-", "");
                LocalDate fecha = parseFecha(c[1], "Egresos", lineNo, line);
                String tercero = c[2].trim();
                String suc = c[3].trim();
                String razon = c[4].trim();
                String doctoSa = c[5].trim().replaceFirst("^[^-]+-", "");
                String doctoCausacion = c[6].trim().replaceFirst("^[^-]+-", "");
                BigDecimal vlrEgreso = parseMoney(c[7], "Egresos", lineNo, "vlr_egreso", line);
                String notas = c[8].trim();
                BigDecimal valorDocto = parseMoney(c[9], "Egresos", lineNo, "valor_docto", line);
                BigDecimal prontoPago = parseMoney(c[10], "Egresos", lineNo, "pronto_pago", line);

                batch.add(new Object[]{
                    uploadId,
                    doctoEgreso,
                    Date.valueOf(fecha),
                    tercero,
                    suc,
                    razon,
                    doctoSa,
                    doctoCausacion,
                    vlrEgreso,
                    notas,
                    valorDocto,
                    prontoPago
                });

                if (batch.size() >= 2000) {
                    batchInsertEgresos(batch);
                    batch.clear();
                }
            }
        }

        if (!batch.isEmpty()) {
            batchInsertEgresos(batch);
        }
    }

    private void batchInsertEgresos(List<Object[]> batch) {
        jdbc.batchUpdate("""
            INSERT INTO egresos_plano
            (upload_id, docto_egreso, fecha_egreso, tercero, suc, razon_social, docto_sa, docto_causacion, vlr_egreso, notas, valor_docto, pronto_pago)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, batch);
    }

    // =========================
    // FACTURAS (16 columnas)
    // ORDEN REAL DEL TXT:
    // 0 docto_causacion
    // 1 documento
    // 2 docto_referencia
    // 3 proveedor
    // 4 razon_social_proveedor
    // 5 fecha
    // 6 item
    // 7 desc_item
    // 8 codigo_barra_principal
    // 9 um
    // 10 cantidad
    // 11 valor_subtotal
    // 12 valor_imptos
    // 13 valor_dsctos
    // 14 valor_neto
    // 15 tipo_docto
    // =========================
    private void importarFacturas(long uploadId, MultipartFile file) throws Exception {
        List<Object[]> batch = new ArrayList<>(2000);

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            int lineNo = 0;
            boolean headerSkipped = false;

            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) continue;

                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }

                if (line.trim().toLowerCase().startsWith("gran total")) {
                    continue;
                }

                String[] c = line.split(SEP, -1);

                if (c.length < 16) {
                    throw new IllegalArgumentException(
                        "Facturas: línea " + lineNo + " tiene " + c.length +
                        " columnas, se esperaban 16. Línea: " + line
                    );
                }

                batch.add(new Object[]{
                    uploadId,
                    c[15].trim(),                                                    // tipo_docto
                    c[0].trim(),                                                     // docto_causacion
                    c[1].trim(),                                                     // documento
                    c[2].trim(),                                                     // docto_referencia
                    c[3].trim(),                                                     // proveedor
                    c[4].trim(),                                                     // razon_social_proveedor
                    Date.valueOf(parseFecha(c[5], "Facturas", lineNo, line)),        // fecha
                    c[6].trim(),                                                     // item
                    c[7].trim(),                                                     // desc_item
                    c[8].trim(),                                                     // codigo_barra_principal
                    c[9].trim(),                                                     // um
                    parseDecimalPlain(c[10], "Facturas", lineNo, "cantidad", line),  // cantidad
                    parseMoney(c[11], "Facturas", lineNo, "valor_subtotal", line),   // valor_subtotal
                    parseMoney(c[12], "Facturas", lineNo, "valor_imptos", line),     // valor_imptos
                    parseMoney(c[13], "Facturas", lineNo, "valor_dsctos", line),     // valor_dsctos
                    parseMoney(c[14], "Facturas", lineNo, "valor_neto", line)        // valor_neto
                });

                if (batch.size() >= 2000) {
                    batchInsertFacturas(batch);
                    batch.clear();
                }
            }
        }

        if (!batch.isEmpty()) {
            batchInsertFacturas(batch);
        }
    }

    private void batchInsertFacturas(List<Object[]> batch) {
        jdbc.batchUpdate("""
            INSERT INTO facturas_plano
            (upload_id, tipo_docto, docto_causacion, documento, docto_referencia, proveedor, razon_social_proveedor, fecha,
             item, desc_item, codigo_barra_principal, um, cantidad, valor_subtotal, valor_imptos, valor_dsctos, valor_neto)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, batch);
    }

    // =========================
    // NOTAS (10 columnas)
    // =========================
    private void importarNotas(long uploadId, MultipartFile file) throws Exception {
        List<Object[]> batch = new ArrayList<>(2000);

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            int lineNo = 0;
            boolean headerSkipped = false;

            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) continue;

                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }

                if (line.trim().toLowerCase().startsWith("gran total")) {
                    continue;
                }

                String[] c = line.split(SEP, -1);

                if (c.length < 10) {
                    throw new IllegalArgumentException(
                        "Notas: línea " + lineNo + " tiene " + c.length +
                        " columnas, se esperaban 10. Línea: " + line
                    );
                }

                batch.add(new Object[]{
                    uploadId,
                    c[0].trim().replaceFirst("^[^-]+-", ""),
                    c[1].trim(),
                    c[2].trim(),
                    c[3].trim(),
                    parseMoney(c[4], "Notas", lineNo, "valor_bruto", line),
                    parseMoney(c[5], "Notas", lineNo, "valor_dsctos", line),
                    parseMoney(c[6], "Notas", lineNo, "valor_imptos", line),
                    parseMoney(c[7], "Notas", lineNo, "valor_neto", line),
                    parseMoney(c[8], "Notas", lineNo, "valor_retenciones", line),
                    c[9].trim()
                });

                if (batch.size() >= 2000) {
                    batchInsertNotas(batch);
                    batch.clear();
                }
            }
        }

        if (!batch.isEmpty()) {
            batchInsertNotas(batch);
        }
    }

    private void batchInsertNotas(List<Object[]> batch) {
        jdbc.batchUpdate("""
            INSERT INTO notas_plano
            (upload_id, docto_proveedor, nro_documento, referencia_1, razon_social_proveedor,
             valor_bruto, valor_dsctos, valor_imptos, valor_neto, valor_retenciones, notas, creado_en)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
        """, batch);
    }

    // =========================
    // PARSERS
    // =========================
    private LocalDate parseFecha(String s, String archivo, int lineNo, String line) {
        String v = s == null ? "" : s.trim();

        if (v.isEmpty()) {
            throw new IllegalArgumentException(
                archivo + ": fecha vacía en línea " + lineNo + ". Línea: " + line
            );
        }

        try {
            String[] p = v.split("/");
            if (p.length == 3) {
                int d = Integer.parseInt(p[0]);
                int m = Integer.parseInt(p[1]);
                int y = Integer.parseInt(p[2]);
                return LocalDate.of(y, m, d);
            }

            return LocalDate.parse(v);

        } catch (NumberFormatException | DateTimeParseException e) {
            throw new IllegalArgumentException(
                archivo + ": fecha inválida '" + v + "' en línea " + lineNo + ". Línea: " + line,
                e
            );
        }
    }

    private BigDecimal parseMoney(String s, String archivo, int lineNo, String campo, String line) {
        String v = s == null ? "" : s.trim();
        if (v.isEmpty()) return BigDecimal.ZERO;

        try {
            boolean negative = v.contains("-");

            v = v.replace("$", "")
                 .replace(" ", "")
                 .replace("-", "");

            v = v.replace(".", "").replace(",", ".");

            BigDecimal bd = new BigDecimal(v);
            return negative ? bd.negate() : bd;

        } catch (Exception e) {
            throw new IllegalArgumentException(
                archivo + ": valor monetario inválido en campo '" + campo + "' = '" + s +
                "' en línea " + lineNo + ". Línea: " + line,
                e
            );
        }
    }

   private BigDecimal parseDecimalPlain(String s, String archivo, int lineNo, String campo, String line) {
    String v = s == null ? "" : s.trim();
    if (v.isEmpty()) return BigDecimal.ZERO;

    try {
        v = v.replace(" ", "");

        // Caso típico colombiano:
        // 3.600,00 -> 3600.00
        // 1,5      -> 1.5
        // 12       -> 12
        if (v.contains(",") && v.contains(".")) {
            v = v.replace(".", "").replace(",", ".");
        } else if (v.contains(",")) {
            v = v.replace(",", ".");
        }

        return new BigDecimal(v);

    } catch (Exception e) {
        throw new IllegalArgumentException(
            archivo + ": valor decimal inválido en campo '" + campo + "' = '" + s +
            "' en línea " + lineNo + ". Línea: " + line,
            e
        );
    }
}
}