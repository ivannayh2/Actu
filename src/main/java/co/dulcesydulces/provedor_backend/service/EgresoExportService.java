package co.dulcesydulces.provedor_backend.service;

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
        List<String> lineas = new ArrayList<>();
        lineas.add("REPORTE DE EGRESOS");
        lineas.add("");
        lineas.add(formatearLineaResumen("Numero egreso", "Fecha", "Nit proveedor", "Razon social", "Valor"));
        lineas.add(repetir('-', 95));

        for (EgresoPlanoResumen registro : registros) {
            lineas.add(formatearLineaResumen(
                    registro.getDoctoEgreso(),
                    formatearFecha(registro.getFechaEgreso()),
                    registro.getTercero(),
                    registro.getRazonSocial(),
                    formatearMoneda(registro.getVlrEgreso())
            ));
        }

        lineas.add("");
        lineas.add("TOTAL: " + formatearMoneda(totalVlrEgreso));
        return renderizarPdf(lineas);
    }

    public byte[] generarPdfDetalle(
            List<EgresoDetalleView> registros,
            BigDecimal totalValorDocto,
            BigDecimal totalProntoPago,
            BigDecimal totalDebitos,
            BigDecimal totalCreditosFinal
    ) {
        List<String> lineas = new ArrayList<>();
        lineas.add("DETALLE DE EGRESOS");
        lineas.add("");
        lineas.add(formatearLineaDetalle("Numero factura", "Nota", "Detalle", "Debitos", "Creditos"));
        lineas.add(repetir('-', 98));

        for (EgresoDetalleView registro : registros) {
            BigDecimal valorAjustado = valorBigDecimal(registro.getVlrEgreso()).add(valorBigDecimal(registro.getProntoPago()));
            lineas.add(formatearLineaDetalle(
                    registro.getDoctoSa(),
                    registro.getDoctoProveedorRelacionado(),
                    registro.getDoctoCausacion(),
                    valorAjustado.signum() > 0 ? formatearMoneda(valorAjustado) : "-",
                    valorAjustado.signum() < 0 ? formatearMoneda(valorAjustado.abs()) : "-"
            ));
        }

        lineas.add("");
        lineas.add("Valor documento: " + formatearMoneda(totalValorDocto));
        lineas.add("Pronto pago:    " + formatearMoneda(totalProntoPago));
        lineas.add("Total debitos:  " + formatearMoneda(totalDebitos));
        lineas.add("Total creditos: " + formatearMoneda(totalCreditosFinal));
        return renderizarPdf(lineas);
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

    private byte[] renderizarPdf(List<String> lineas) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.COURIER);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD);

            PDPage page = new PDPage(crearPaginaHorizontal());
            document.addPage(page);

            float margin = 36f;
            float y = page.getMediaBox().getHeight() - margin;
            float leading = 12f;

            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            contentStream.setLeading(leading);
            contentStream.beginText();
            contentStream.setFont(bold, 11);
            contentStream.newLineAtOffset(margin, y);

            boolean firstLine = true;
            for (String linea : lineas) {
                if (y <= margin + leading) {
                    contentStream.endText();
                    contentStream.close();

                    page = new PDPage(crearPaginaHorizontal());
                    document.addPage(page);
                    y = page.getMediaBox().getHeight() - margin;

                    contentStream = new PDPageContentStream(document, page);
                    contentStream.setLeading(leading);
                    contentStream.beginText();
                    contentStream.setFont(regular, 9);
                    contentStream.newLineAtOffset(margin, y);
                    firstLine = false;
                }

                if (firstLine) {
                    contentStream.showText(linea);
                    contentStream.newLine();
                    contentStream.setFont(regular, 9);
                    firstLine = false;
                } else {
                    contentStream.showText(linea != null ? linea : "");
                    contentStream.newLine();
                }

                y -= leading;
            }

            contentStream.endText();
            contentStream.close();

            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo generar el PDF de egresos", ex);
        }
    }

    private String formatearLineaResumen(String doctoEgreso, String fecha, String tercero, String razonSocial, String valor) {
        return String.format(
                "%-18s %-12s %-16s %-30s %15s",
                truncar(doctoEgreso, 18),
                truncar(fecha, 12),
                truncar(tercero, 16),
                truncar(razonSocial, 30),
                truncar(valor, 15)
        );
    }

    private String formatearLineaDetalle(String factura, String nota, String detalle, String debitos, String creditos) {
        return String.format(
                "%-20s %-18s %-20s %16s %16s",
                truncar(factura, 20),
                truncar(nota, 18),
                truncar(detalle, 20),
                truncar(debitos, 16),
                truncar(creditos, 16)
        );
    }

    private String truncar(String valor, int limite) {
        String texto = valorTexto(valor);
        if (texto.length() <= limite) {
            return texto;
        }
        if (limite <= 3) {
            return texto.substring(0, limite);
        }
        return texto.substring(0, limite - 3) + "...";
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

    private String repetir(char caracter, int veces) {
        return String.valueOf(caracter).repeat(Math.max(0, veces));
    }
}