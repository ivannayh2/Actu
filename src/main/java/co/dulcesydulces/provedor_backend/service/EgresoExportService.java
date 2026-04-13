package co.dulcesydulces.provedor_backend.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import co.dulcesydulces.provedor_backend.domain.dto.EgresoDetalleView;
import co.dulcesydulces.provedor_backend.domain.dto.EgresoPlanoResumen;

@Service
public class EgresoExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final float PDF_MARGIN = 36f;
    private static final float PDF_ROW_HEIGHT = 22f;
    private static final float PDF_FONT_SIZE = 9f;
    private static final float PDF_TITLE_SIZE = 14f;
    private static final Color PDF_COLOR_TITLE = new Color(18, 18, 18);
    private static final Color PDF_COLOR_HEADER = Color.WHITE;
    private static final Color PDF_COLOR_HEADER_BORDER = new Color(32, 32, 32);
    private static final Color PDF_COLOR_LABEL = Color.WHITE;
    private static final Color PDF_COLOR_BORDER = new Color(110, 110, 110);
    private static final Color PDF_COLOR_TEXT = new Color(26, 26, 26);
    private static final Color PDF_COLOR_TOTAL = Color.WHITE;
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat(
            "$ #,##0.00",
            DecimalFormatSymbols.getInstance(new Locale("es", "CO"))
    );

    public byte[] generarExcelResumen(List<EgresoPlanoResumen> registros, BigDecimal totalVlrEgreso) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Egresos resumen");
            CellStyle headerStyle = crearHeaderStyle(workbook);
            CellStyle dateStyle = crearDateStyle(workbook);
            CellStyle moneyStyle = crearMoneyStyle(workbook);

            int rowIndex = 0;
            rowIndex = escribirTitulo(sheet, rowIndex, "Reporte de egresos");

            Row header = sheet.createRow(rowIndex++);
            String[] columnas = {"Numero egreso", "Fecha egreso", "Nit proveedor", "Razon social", "Valor"};
            for (int i = 0; i < columnas.length; i++) {
                var cell = header.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
            }

            for (EgresoPlanoResumen registro : registros) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(valorTexto(registro.getDoctoEgreso()));
                var dateCell = row.createCell(1);
                if (registro.getFechaEgreso() != null) {
                    dateCell.setCellValue(java.sql.Date.valueOf(registro.getFechaEgreso()));
                    dateCell.setCellStyle(dateStyle);
                } else {
                    dateCell.setCellValue("");
                }
                row.createCell(2).setCellValue(valorTexto(registro.getTercero()));
                row.createCell(3).setCellValue(valorTexto(registro.getRazonSocial()));
                var amountCell = row.createCell(4);
                amountCell.setCellValue(valorNumerico(registro.getVlrEgreso()));
                amountCell.setCellStyle(moneyStyle);
            }

            Row totalRow = sheet.createRow(rowIndex);
            totalRow.createCell(3).setCellValue("Total");
            var totalCell = totalRow.createCell(4);
            totalCell.setCellValue(valorNumerico(totalVlrEgreso));
            totalCell.setCellStyle(moneyStyle);

            ajustarColumnas(sheet, columnas.length);

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo generar el archivo Excel de egresos", ex);
        }
    }

    public byte[] generarExcelDetalle(
            List<EgresoDetalleView> registros,
            BigDecimal totalValorDocto,
            BigDecimal totalProntoPago,
            BigDecimal totalDebitos,
            BigDecimal totalCreditosFinal
    ) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Egresos detalle");
            CellStyle headerStyle = crearHeaderStyle(workbook);
            CellStyle moneyStyle = crearMoneyStyle(workbook);

            int rowIndex = 0;
            rowIndex = escribirTitulo(sheet, rowIndex, "Detalle de egresos");

            Row header = sheet.createRow(rowIndex++);
            String[] columnas = {"Numero factura", "Nota relacionada", "Detalle", "Debitos", "Creditos"};
            for (int i = 0; i < columnas.length; i++) {
                var cell = header.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
            }

            for (EgresoDetalleView registro : registros) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(valorTexto(registro.getDoctoSa()));
                row.createCell(1).setCellValue(valorTexto(registro.getDoctoProveedorRelacionado()));
                row.createCell(2).setCellValue(valorTexto(registro.getDoctoCausacion()));

                BigDecimal valorAjustado = valorBigDecimal(registro.getVlrEgreso()).add(valorBigDecimal(registro.getProntoPago()));

                var debitosCell = row.createCell(3);
                debitosCell.setCellValue(valorAjustado.signum() > 0 ? valorAjustado.doubleValue() : 0d);
                debitosCell.setCellStyle(moneyStyle);

                var creditosCell = row.createCell(4);
                creditosCell.setCellValue(valorAjustado.signum() < 0 ? valorAjustado.abs().doubleValue() : 0d);
                creditosCell.setCellStyle(moneyStyle);
            }

            rowIndex = escribirTotalDetalle(sheet, rowIndex, "Valor documento", totalValorDocto, moneyStyle);
            rowIndex = escribirTotalDetalle(sheet, rowIndex, "Pronto pago", totalProntoPago, moneyStyle);
            rowIndex = escribirTotalDetalle(sheet, rowIndex, "Total debitos", totalDebitos, moneyStyle);
            escribirTotalDetalle(sheet, rowIndex, "Total creditos", totalCreditosFinal, moneyStyle);

            ajustarColumnas(sheet, columnas.length);

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo generar el archivo Excel de egresos", ex);
        }
    }

    public byte[] generarPdfResumen(List<EgresoPlanoResumen> registros, BigDecimal totalVlrEgreso) {
        List<String[]> filas = new ArrayList<>();
        for (EgresoPlanoResumen registro : registros) {
            filas.add(new String[] {
                valorTexto(registro.getDoctoEgreso()),
                formatearFecha(registro.getFechaEgreso()),
                valorTexto(registro.getTercero()),
                valorTexto(registro.getRazonSocial()),
                formatearMoneda(registro.getVlrEgreso())
            });
        }

        return renderizarPdfTabla(
            "REPORTE DE EGRESOS",
            List.of(),
            new String[] {"Numero egreso", "Fecha egreso", "NIT - tercero", "Razon social", "Valor"},
            new float[] {120f, 90f, 110f, 280f, 110f},
            filas,
            List.of(new PdfResumenFila("Total", formatearMoneda(totalVlrEgreso)))
        );
    }

    public byte[] generarPdfDetalle(
            List<EgresoDetalleView> registros,
            BigDecimal totalValorDocto,
            BigDecimal totalProntoPago,
            BigDecimal totalDebitos,
            BigDecimal totalCreditosFinal
    ) {
        List<String[]> filas = new ArrayList<>();
        List<PdfResumenFila> cabecera = List.of();

        if (!registros.isEmpty()) {
            EgresoDetalleView encabezado = registros.get(0);
            cabecera = List.of(
                new PdfResumenFila("NIT - tercero", valorTexto(encabezado.getTercero())),
                new PdfResumenFila("Razon social", valorTexto(encabezado.getRazonSocial())),
                new PdfResumenFila("Dct egresos", valorTexto(encabezado.getDoctoEgreso()))
            );
        }

        for (EgresoDetalleView registro : registros) {
            BigDecimal valorAjustado = valorBigDecimal(registro.getVlrEgreso()).add(valorBigDecimal(registro.getProntoPago()));
            filas.add(new String[] {
                valorTexto(registro.getDoctoSa()),
                valorTexto(registro.getDoctoProveedorRelacionado()),
                valorTexto(registro.getDoctoCausacion()),
                valorAjustado.signum() > 0 ? formatearMoneda(valorAjustado) : "-",
                valorAjustado.signum() < 0 ? formatearMoneda(valorAjustado.abs()) : "-"
            });
        }

        return renderizarPdfTabla(
            "DETALLE DE EGRESOS",
            cabecera,
            new String[] {"Numero factura", "Nota relacionada", "Detalle", "Debitos", "Creditos"},
            new float[] {135f, 135f, 245f, 95f, 95f},
            filas,
            List.of(
                new PdfResumenFila("Valor documento", formatearMoneda(totalValorDocto)),
                new PdfResumenFila("Pronto pago", formatearMoneda(totalProntoPago)),
                new PdfResumenFila("Total debitos", formatearMoneda(totalDebitos)),
                new PdfResumenFila("Total creditos", formatearMoneda(totalCreditosFinal))
            )
        );
    }

    private int escribirTitulo(XSSFSheet sheet, int rowIndex, String titulo) {
        Row titleRow = sheet.createRow(rowIndex++);
        titleRow.createCell(0).setCellValue(titulo);
        return rowIndex + 1;
    }

    private int escribirTotalDetalle(XSSFSheet sheet, int rowIndex, String label, BigDecimal valor, CellStyle moneyStyle) {
        Row row = sheet.createRow(rowIndex++);
        row.createCell(3).setCellValue(label);
        var cell = row.createCell(4);
        cell.setCellValue(valorNumerico(valor));
        cell.setCellStyle(moneyStyle);
        return rowIndex;
    }

    private CellStyle crearHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle crearDateStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat dataFormat = workbook.createDataFormat();
        style.setDataFormat(dataFormat.getFormat("dd/mm/yyyy"));
        return style;
    }

    private CellStyle crearMoneyStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat dataFormat = workbook.createDataFormat();
        style.setDataFormat(dataFormat.getFormat("$ #,##0.00"));
        return style;
    }

    private void ajustarColumnas(XSSFSheet sheet, int totalColumnas) {
        for (int i = 0; i < totalColumnas; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 1200, 18000));
        }
    }

    private byte[] renderizarPdfTabla(
            String titulo,
            List<PdfResumenFila> cabecera,
            String[] columnas,
            float[] anchos,
            List<String[]> filas,
            List<PdfResumenFila> resumen
    ) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.COURIER);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD);

            PDPage page = new PDPage(crearPaginaHorizontal());
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float y = page.getMediaBox().getHeight() - PDF_MARGIN;
                y = dibujarTitulo(contentStream, bold, titulo, y);

                if (!cabecera.isEmpty()) {
                    y = dibujarTablaResumen(contentStream, regular, bold, cabecera, y, 540f);
                    y -= 16f;
                }

                y = dibujarTablaPrincipal(
                    document,
                    page,
                    contentStream,
                    regular,
                    bold,
                    columnas,
                    anchos,
                    filas,
                    resumen,
                    y,
                    titulo
                );
            }

            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo generar el PDF de egresos", ex);
        }
    }

    private float dibujarTitulo(PDPageContentStream contentStream, PDFont font, String titulo, float y) throws IOException {
        escribirTexto(contentStream, font, PDF_TITLE_SIZE, PDF_MARGIN, y, titulo, PDF_COLOR_TITLE);
        dibujarLinea(contentStream, PDF_MARGIN, y - 8f, crearPaginaHorizontal().getWidth() - PDF_MARGIN, y - 8f, PDF_COLOR_HEADER_BORDER, 1.2f);
        return y - 28f;
    }

    private float dibujarTablaResumen(
            PDPageContentStream contentStream,
            PDFont regular,
            PDFont bold,
            List<PdfResumenFila> filas,
            float y,
            float anchoTotal
    ) throws IOException {
        float etiquetaWidth = 150f;
        float valorWidth = anchoTotal - etiquetaWidth;
        float x = PDF_MARGIN;

        for (PdfResumenFila fila : filas) {
            dibujarCelda(contentStream, x, y, etiquetaWidth, PDF_ROW_HEIGHT, PDF_COLOR_LABEL, PDF_COLOR_BORDER);
            dibujarCelda(contentStream, x + etiquetaWidth, y, valorWidth, PDF_ROW_HEIGHT, Color.WHITE);
            escribirTextoCelda(contentStream, bold, 9f, x, y, etiquetaWidth, PDF_ROW_HEIGHT, fila.etiqueta(), false, PDF_COLOR_TEXT);
            escribirTextoCelda(contentStream, regular, 9f, x + etiquetaWidth, y, valorWidth, PDF_ROW_HEIGHT, fila.valor(), false, PDF_COLOR_TEXT);
            y -= PDF_ROW_HEIGHT;
        }

        return y;
    }

    private float dibujarTablaPrincipal(
            PDDocument document,
            PDPage initialPage,
            PDPageContentStream initialContentStream,
            PDFont regular,
            PDFont bold,
            String[] columnas,
            float[] anchos,
            List<String[]> filas,
            List<PdfResumenFila> resumen,
            float y,
            String titulo
    ) throws IOException {
        PDPage page = initialPage;
        PDPageContentStream contentStream = initialContentStream;
        float tableWidth = 0f;
        for (float ancho : anchos) {
            tableWidth += ancho;
        }

        y = dibujarFilaCabecera(contentStream, bold, columnas, anchos, y);

        for (int rowIndex = 0; rowIndex < filas.size(); rowIndex++) {
            String[] fila = filas.get(rowIndex);
            if (y - PDF_ROW_HEIGHT < PDF_MARGIN + 90f) {
                page = new PDPage(crearPaginaHorizontal());
                document.addPage(page);
                contentStream.close();
                contentStream = new PDPageContentStream(document, page);
                y = page.getMediaBox().getHeight() - PDF_MARGIN;
                y = dibujarTitulo(contentStream, bold, titulo, y);
                y = dibujarFilaCabecera(contentStream, bold, columnas, anchos, y);
            }
            dibujarFilaDatos(contentStream, regular, fila, anchos, y, rowIndex);
            y -= PDF_ROW_HEIGHT;
        }

        y -= 16f;
        if (!resumen.isEmpty()) {
            if (y - (resumen.size() * PDF_ROW_HEIGHT) < PDF_MARGIN) {
                page = new PDPage(crearPaginaHorizontal());
                document.addPage(page);
                contentStream.close();
                contentStream = new PDPageContentStream(document, page);
                y = page.getMediaBox().getHeight() - PDF_MARGIN;
                y = dibujarTitulo(contentStream, bold, titulo, y);
            }

            float resumenX = PDF_MARGIN + Math.max(0f, tableWidth - 280f);
            for (PdfResumenFila fila : resumen) {
                dibujarCelda(contentStream, resumenX, y, 150f, PDF_ROW_HEIGHT, PDF_COLOR_TOTAL, PDF_COLOR_HEADER_BORDER);
                dibujarCelda(contentStream, resumenX + 150f, y, 130f, PDF_ROW_HEIGHT, Color.WHITE, PDF_COLOR_HEADER_BORDER);
                escribirTextoCelda(contentStream, bold, 9f, resumenX, y, 150f, PDF_ROW_HEIGHT, fila.etiqueta(), false, PDF_COLOR_TEXT);
                escribirTextoCelda(contentStream, regular, 9f, resumenX + 150f, y, 130f, PDF_ROW_HEIGHT, fila.valor(), true, PDF_COLOR_TEXT);
                dibujarLinea(contentStream, resumenX, y - PDF_ROW_HEIGHT, resumenX + 280f, y - PDF_ROW_HEIGHT, PDF_COLOR_HEADER_BORDER, 0.9f);
                y -= PDF_ROW_HEIGHT;
            }
        }

        contentStream.close();
        return y;
    }

    private float dibujarFilaCabecera(PDPageContentStream contentStream, PDFont font, String[] columnas, float[] anchos, float y) throws IOException {
        float x = PDF_MARGIN;
        for (int i = 0; i < columnas.length; i++) {
            dibujarCelda(contentStream, x, y, anchos[i], PDF_ROW_HEIGHT, PDF_COLOR_HEADER, PDF_COLOR_HEADER_BORDER);
            escribirTextoCelda(contentStream, font, 9f, x, y, anchos[i], PDF_ROW_HEIGHT, columnas[i], false, PDF_COLOR_TITLE);
            x += anchos[i];
        }
        dibujarLinea(contentStream, PDF_MARGIN, y - PDF_ROW_HEIGHT, x, y - PDF_ROW_HEIGHT, PDF_COLOR_HEADER_BORDER, 1.1f);
        return y - PDF_ROW_HEIGHT;
    }

    private void dibujarFilaDatos(PDPageContentStream contentStream, PDFont font, String[] fila, float[] anchos, float y, int rowIndex) throws IOException {
        float x = PDF_MARGIN;
        Color background = Color.WHITE;
        for (int i = 0; i < anchos.length; i++) {
            dibujarCelda(contentStream, x, y, anchos[i], PDF_ROW_HEIGHT, background, PDF_COLOR_BORDER);
            boolean alineadoDerecha = i >= anchos.length - 2;
            String valor = i < fila.length ? fila[i] : "";
            escribirTextoCelda(contentStream, font, PDF_FONT_SIZE, x, y, anchos[i], PDF_ROW_HEIGHT, valor, alineadoDerecha, PDF_COLOR_TEXT);
            x += anchos[i];
        }
    }

    private void dibujarCelda(PDPageContentStream contentStream, float x, float y, float width, float height, Color fillColor) throws IOException {
        dibujarCelda(contentStream, x, y, width, height, fillColor, PDF_COLOR_BORDER);
    }

    private void dibujarCelda(PDPageContentStream contentStream, float x, float y, float width, float height, Color fillColor, Color borderColor) throws IOException {
        contentStream.setNonStrokingColor(fillColor);
        contentStream.addRect(x, y - height, width, height);
        contentStream.fill();
        contentStream.setStrokingColor(borderColor);
        contentStream.setLineWidth(0.65f);
        contentStream.addRect(x, y - height, width, height);
        contentStream.stroke();
    }

    private void dibujarLinea(
            PDPageContentStream contentStream,
            float x1,
            float y1,
            float x2,
            float y2,
            Color color,
            float lineWidth
    ) throws IOException {
        contentStream.setStrokingColor(color);
        contentStream.setLineWidth(lineWidth);
        contentStream.moveTo(x1, y1);
        contentStream.lineTo(x2, y2);
        contentStream.stroke();
    }

    private void escribirTextoCelda(
            PDPageContentStream contentStream,
            PDFont font,
            float fontSize,
            float x,
            float y,
            float width,
            float height,
            String texto,
            boolean alinearDerecha,
            Color textColor
    ) throws IOException {
        float padding = 6f;
        float usableWidth = width - (padding * 2);
        String contenido = ajustarTextoPdf(valorTexto(texto), font, fontSize, usableWidth);
        float textWidth = font.getStringWidth(contenido) / 1000f * fontSize;
        float textX = alinearDerecha ? x + width - padding - textWidth : x + padding;
        float textY = y - ((height - fontSize) / 2f) - 2f;
        escribirTexto(contentStream, font, fontSize, textX, textY, contenido, textColor);
    }

    private void escribirTexto(
            PDPageContentStream contentStream,
            PDFont font,
            float fontSize,
            float x,
            float y,
            String texto,
            Color color
    ) throws IOException {
        contentStream.beginText();
        contentStream.setNonStrokingColor(color);
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(texto != null ? texto : "");
        contentStream.endText();
    }

    private String ajustarTextoPdf(String valor, PDFont font, float fontSize, float maxWidth) throws IOException {
        String texto = valorTexto(valor);
        if (font.getStringWidth(texto) / 1000f * fontSize <= maxWidth) {
            return texto;
        }

        String sufijo = "...";
        String base = texto;
        while (!base.isEmpty()) {
            base = base.substring(0, base.length() - 1);
            String candidato = base + sufijo;
            if (font.getStringWidth(candidato) / 1000f * fontSize <= maxWidth) {
                return candidato;
            }
        }
        return sufijo;
    }

    private String valorTexto(String valor) {
        return valor != null && !valor.isBlank() ? valor : "-";
    }

    private double valorNumerico(BigDecimal valor) {
        return valorBigDecimal(valor).doubleValue();
    }

    private BigDecimal valorBigDecimal(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private String formatearMoneda(BigDecimal valor) {
        return MONEY_FORMAT.format(valorBigDecimal(valor));
    }

    private String formatearFecha(LocalDate fecha) {
        return fecha != null ? DATE_FORMATTER.format(fecha) : "-";
    }

    private PDRectangle crearPaginaHorizontal() {
        return new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
    }

    private record PdfResumenFila(String etiqueta, String valor) {
    }
}