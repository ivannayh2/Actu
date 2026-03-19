package co.dulcesydulces.provedor_backend.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
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

    public PlanosUploadService(UploadsRepository uploadsRepository, JdbcTemplate jdbc) {
        this.uploadsRepository = uploadsRepository;
        this.jdbc = jdbc;
    }

    @Transactional
    public long procesar(MultipartFile egresos, MultipartFile facturas, MultipartFile notas) throws Exception {
        validarTxt(egresos);
        validarTxt(facturas);
        validarTxt(notas);

        long uploadId = uploadsRepository.crearUpload(
                egresos.getOriginalFilename(),
                facturas.getOriginalFilename(),
                notas.getOriginalFilename()
        );

        importarEgresos(uploadId, egresos);
        importarFacturas(uploadId, facturas);
        importarNotas(uploadId, notas);

        return uploadId;
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

    // Tus TXT están separados por TAB
    private static final String SEP = "\t";

    // =========================
    // EGRESOS (12 columnas en el TXT, usamos 0..8)
    // =========================
    private void importarEgresos(long uploadId, MultipartFile file) throws Exception {
        List<Object[]> batch = new ArrayList<>(2000);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            boolean headerSkipped = false;

            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) continue;

                // Saltar encabezado
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }

                String[] c = line.split(SEP, -1);

                // Egresos tiene 12 columnas según tu ejemplo
                if (c.length < 12) {
                    throw new IllegalArgumentException(
                            "Egresos: línea " + lineNo + " tiene " + c.length + " columnas, se esperaban 12. Línea: " + line
                    );
                }
                String doctoEgreso = c[0].trim().replaceFirst("^[^-]+-", "");
                //String doctoEgreso = c[0].trim();
                LocalDate fecha = parseFecha(c[1]);
                String tercero = c[2].trim();
                String suc = c[3].trim();
                String razon = c[4].trim();
                String doctoSa = c[5].trim();
                String doctoCausacion = c[6].trim().replaceFirst("^[^-]+-", "");
                BigDecimal vlrEgreso = parseMoney(c[7]);
                String notas = c[8].trim();

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
                        notas
                });

                if (batch.size() >= 2000) {
                    batchInsertEgresos(batch);
                    batch.clear();
                }
            }
        }

        if (!batch.isEmpty()) batchInsertEgresos(batch);
    }

    private void batchInsertEgresos(List<Object[]> batch) {
        jdbc.batchUpdate("""
            INSERT INTO egresos_plano
            (upload_id, docto_egreso, fecha_egreso, tercero, suc, razon_social, docto_sa, docto_causacion, vlr_egreso, notas)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, batch);
    }

    // =========================
    // FACTURAS (15 columnas)
    // =========================
    private void importarFacturas(long uploadId, MultipartFile file) throws Exception {
        List<Object[]> batch = new ArrayList<>(2000);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            boolean headerSkipped = false;

            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) continue;

                // Saltar encabezado
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }

                String[] c = line.split(SEP, -1);

                if (c.length < 15) {
                    throw new IllegalArgumentException(
                            "Facturas: línea " + lineNo + " tiene " + c.length + " columnas, se esperaban 15. Línea: " + line
                    );
                }

                // Nota: viene en notación científica "8,69302E+12". Guardamos tal cual (VARCHAR)
                String codigoBarraPrincipal = c[8].trim();

                batch.add(new Object[]{
                        uploadId,
                        c[0].trim(),                    // docto_causacion
                        c[1].trim(),                    // documento
                        c[2].trim(),                    // docto_referencia
                        c[3].trim(),                    // proveedor
                        c[4].trim(),                    // razon_social_proveedor
                        Date.valueOf(parseFecha(c[5])), // fecha
                        c[6].trim(),                    // item
                        c[7].trim(),                    // desc_item
                        codigoBarraPrincipal,           // codigo_barra_principal
                        c[9].trim(),                    // um
                        parseDecimalPlain(c[10]),       // cantidad (no es dinero)
                        parseMoney(c[11]),              // valor_subtotal
                        parseMoney(c[12]),              // valor_imptos
                        parseMoney(c[13]),              // valor_dsctos
                        parseMoney(c[14])               // valor_neto
                });

                if (batch.size() >= 2000) {
                    batchInsertFacturas(batch);
                    batch.clear();
                }
            }
        }

        if (!batch.isEmpty()) batchInsertFacturas(batch);
    }

    private void batchInsertFacturas(List<Object[]> batch) {
        jdbc.batchUpdate("""
            INSERT INTO facturas_plano
            (upload_id, docto_causacion, documento, docto_referencia, proveedor, razon_social_proveedor, fecha,
             item, desc_item, codigo_barra_principal, um, cantidad, valor_subtotal, valor_imptos, valor_dsctos, valor_neto)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, batch);
    }

    // =========================
    // NOTAS (10 columnas)
    // =========================
    private void importarNotas(long uploadId, MultipartFile file) throws Exception {
        List<Object[]> batch = new ArrayList<>(2000);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            boolean headerSkipped = false;

            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) continue;

                // Saltar encabezado
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }

                String[] c = line.split(SEP, -1);

                if (c.length < 10) {
                    throw new IllegalArgumentException(
                            "Notas: línea " + lineNo + " tiene " + c.length + " columnas, se esperaban 10. Línea: " + line
                    );
                }

                batch.add(new Object[]{
                        uploadId,
                        c[0].trim(),      // docto_proveedor
                        c[1].trim(),      // nro_documento
                        c[2].trim(),      // referencia_1
                        c[3].trim(),      // razon_social_proveedor
                        parseMoney(c[4]), // valor_bruto (puede ser negativo)
                        parseMoney(c[5]), // valor_dsctos
                        parseMoney(c[6]), // valor_imptos
                        parseMoney(c[7]), // valor_neto (puede ser negativo)
                        parseMoney(c[8]), // valor_retenciones
                        c[9].trim()       // notas
                });

                if (batch.size() >= 2000) {
                    batchInsertNotas(batch);
                    batch.clear();
                }
            }
        }

        if (!batch.isEmpty()) batchInsertNotas(batch);
    }

    private void batchInsertNotas(List<Object[]> batch) {
        jdbc.batchUpdate("""
            INSERT INTO notas_plano
            (upload_id, docto_proveedor, nro_documento, referencia_1, razon_social_proveedor,
             valor_bruto, valor_dsctos, valor_imptos, valor_neto, valor_retenciones, notas)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, batch);
    }

    // =========================
    // PARSERS
    // =========================

    private LocalDate parseFecha(String s) {
        String v = s == null ? "" : s.trim();
        if (v.isEmpty()) throw new IllegalArgumentException("Fecha vacía");

        // dd/MM/yyyy
        String[] p = v.split("/");
        if (p.length == 3) {
            int d = Integer.parseInt(p[0]);
            int m = Integer.parseInt(p[1]);
            int y = Integer.parseInt(p[2]);
            return LocalDate.of(y, m, d);
        }

        // fallback yyyy-MM-dd
        return LocalDate.parse(v);
    }

    /**
     * Para valores de dinero tipo:
     * "$ 779.900,00"  "779.900,00"  "-$ 1.528.524,00"  "$ 0,00"
     */
    private BigDecimal parseMoney(String s) {
        String v = s == null ? "" : s.trim();
        if (v.isEmpty()) return BigDecimal.ZERO;

        boolean negative = v.contains("-");

        v = v.replace("$", "")
             .replace(" ", "")
             .replace("-", "");

        // Miles '.' y decimal ','
        v = v.replace(".", "").replace(",", ".");

        BigDecimal bd = new BigDecimal(v);
        return negative ? bd.negate() : bd;
    }

    /**
     * Para cantidades (no dinero): "2", "2,5", "10"
     */
    private BigDecimal parseDecimalPlain(String s) {
        String v = s == null ? "" : s.trim();
        if (v.isEmpty()) return BigDecimal.ZERO;

        // Si alguna vez viene con coma decimal
        v = v.replace(".", "").replace(",", ".");
        return new BigDecimal(v);
    }
}