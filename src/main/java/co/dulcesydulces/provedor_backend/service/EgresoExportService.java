package co.dulcesydulces.provedor_backend.service;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;

import co.dulcesydulces.provedor_backend.domain.dto.EgresoDetalleView;
import co.dulcesydulces.provedor_backend.domain.dto.EgresoPlanoResumen;

@Service
public class EgresoExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final float PDF_MARGIN = 36f;
    private static final float PDF_ROW_HEIGHT = 26f;
    private static final float PDF_FONT_SIZE = 9f;
    private static final float PDF_TITLE_SIZE = 14f;
    private static final float PDF_SUBTITLE_SIZE = 8f;
    private static final Color PDF_COLOR_TITLE = Color.BLACK;
    private static final Color PDF_COLOR_HEADER = new Color(80, 80, 80);
    private static final Color PDF_COLOR_HEADER_BORDER = new Color(30, 30, 30);
    private static final Color PDF_COLOR_BORDER = new Color(210, 210, 210);
    private static final Color PDF_COLOR_TEXT = Color.BLACK;
    private static final String PDF_LOGO_RESOURCE = "/static/img/logo.png";
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat(
            "$ #,##0.00",
            DecimalFormatSymbols.getInstance(new Locale("es", "CO"))
    );

    public byte[] generarExcelResumen(List<EgresoPlanoResumen> registros, BigDecimal totalVlrEgreso) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Egresos resumen");
            int totalColumnas = 5;
            CellStyle headerStyle = crearHeaderStyle(workbook);
            CellStyle dateStyle = crearDateStyle(workbook);
            CellStyle dateAltStyle = crearDateBodyStyle(workbook, true);
            CellStyle moneyStyle = crearMoneyStyle(workbook);
            CellStyle moneyAltStyle = crearMoneyBodyStyle(workbook, true);
            CellStyle titleStyle = crearTitleStyle(workbook);
            CellStyle subtitleStyle = crearSubtitleStyle(workbook);
            CellStyle bodyStyle = crearBodyStyle(workbook, false);
            CellStyle bodyAltStyle = crearBodyStyle(workbook, true);
            CellStyle totalLabelStyle = crearTotalLabelStyle(workbook);
            CellStyle totalMoneyStyle = crearTotalMoneyStyle(workbook);

            int rowIndex = 0;
            rowIndex = escribirTitulo(sheet, rowIndex, "Reporte de egresos", totalColumnas, titleStyle, subtitleStyle);

            int headerRowIndex = rowIndex;
            Row header = sheet.createRow(rowIndex++);
            String[] columnas = {"Numero egreso", "Fecha egreso", "Nit proveedor", "Razon social", "Valor"};
            for (int i = 0; i < columnas.length; i++) {
                var cell = header.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
            }

            for (EgresoPlanoResumen registro : registros) {
                Row row = sheet.createRow(rowIndex++);
                Cell rowCell = row.createCell(0);
                rowCell.setCellValue(valorTexto(registro.getDoctoEgreso()));
                rowCell.setCellStyle(esFilaPar(rowIndex) ? bodyAltStyle : bodyStyle);

                var dateCell = row.createCell(1);
                if (registro.getFechaEgreso() != null) {
                    dateCell.setCellValue(java.sql.Date.valueOf(registro.getFechaEgreso()));
                    dateCell.setCellStyle(esFilaPar(rowIndex) ? dateAltStyle : dateStyle);
                } else {
                    dateCell.setCellValue("");
                    dateCell.setCellStyle(esFilaPar(rowIndex) ? bodyAltStyle : bodyStyle);
                }

                Cell nitCell = row.createCell(2);
                nitCell.setCellValue(valorTexto(registro.getTercero()));
                nitCell.setCellStyle(esFilaPar(rowIndex) ? bodyAltStyle : bodyStyle);

                Cell razonCell = row.createCell(3);
                razonCell.setCellValue(valorTexto(registro.getRazonSocial()));
                razonCell.setCellStyle(esFilaPar(rowIndex) ? bodyAltStyle : bodyStyle);

                var amountCell = row.createCell(4);
                amountCell.setCellValue(valorNumerico(registro.getVlrEgreso()));
                amountCell.setCellStyle(esFilaPar(rowIndex) ? moneyAltStyle : moneyStyle);
            }

            Row totalRow = sheet.createRow(rowIndex);
            Cell totalLabelCell = totalRow.createCell(3);
            totalLabelCell.setCellValue("Total");
            totalLabelCell.setCellStyle(totalLabelStyle);

            var totalCell = totalRow.createCell(4);
            totalCell.setCellValue(valorNumerico(totalVlrEgreso));
            totalCell.setCellStyle(totalMoneyStyle);

            aplicarAjustesTabla(sheet, headerRowIndex, totalColumnas);
            ajustarColumnas(sheet, totalColumnas);

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
            int totalColumnas = 5;
            CellStyle headerStyle = crearHeaderStyle(workbook);
            CellStyle moneyStyle = crearMoneyStyle(workbook);
            CellStyle moneyAltStyle = crearMoneyBodyStyle(workbook, true);
            CellStyle titleStyle = crearTitleStyle(workbook);
            CellStyle subtitleStyle = crearSubtitleStyle(workbook);
            CellStyle bodyStyle = crearBodyStyle(workbook, false);
            CellStyle bodyAltStyle = crearBodyStyle(workbook, true);
            CellStyle totalLabelStyle = crearTotalLabelStyle(workbook);
            CellStyle totalMoneyStyle = crearTotalMoneyStyle(workbook);

            int rowIndex = 0;
            rowIndex = escribirTitulo(sheet, rowIndex, "Detalle de egresos", totalColumnas, titleStyle, subtitleStyle);

            int headerRowIndex = rowIndex;
            Row header = sheet.createRow(rowIndex++);
            String[] columnas = {"Numero factura", "Nota relacionada", "Detalle", "Debitos", "Creditos"};
            for (int i = 0; i < columnas.length; i++) {
                var cell = header.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
            }

            for (EgresoDetalleView registro : registros) {
                Row row = sheet.createRow(rowIndex++);
                Cell facturaCell = row.createCell(0);
                facturaCell.setCellValue(valorTexto(registro.getDoctoSa()));
                facturaCell.setCellStyle(esFilaPar(rowIndex) ? bodyAltStyle : bodyStyle);

                Cell notaCell = row.createCell(1);
                notaCell.setCellValue(valorTexto(registro.getDoctoProveedorRelacionado()));
                notaCell.setCellStyle(esFilaPar(rowIndex) ? bodyAltStyle : bodyStyle);

                Cell detalleCell = row.createCell(2);
                detalleCell.setCellValue(valorTexto(registro.getDoctoCausacion()));
                detalleCell.setCellStyle(esFilaPar(rowIndex) ? bodyAltStyle : bodyStyle);

                BigDecimal valorAjustado = valorBigDecimal(registro.getVlrEgreso()).add(valorBigDecimal(registro.getProntoPago()));

                var debitosCell = row.createCell(3);
                debitosCell.setCellValue(valorAjustado.signum() > 0 ? valorAjustado.doubleValue() : 0d);
                debitosCell.setCellStyle(esFilaPar(rowIndex) ? moneyAltStyle : moneyStyle);

                var creditosCell = row.createCell(4);
                creditosCell.setCellValue(valorAjustado.signum() < 0 ? valorAjustado.abs().doubleValue() : 0d);
                creditosCell.setCellStyle(esFilaPar(rowIndex) ? moneyAltStyle : moneyStyle);
            }

            rowIndex = escribirTotalDetalle(sheet, rowIndex, "Valor documento", totalValorDocto, totalLabelStyle, totalMoneyStyle);
            rowIndex = escribirTotalDetalle(sheet, rowIndex, "Pronto pago", totalProntoPago, totalLabelStyle, totalMoneyStyle);
            rowIndex = escribirTotalDetalle(sheet, rowIndex, "Total debitos", totalDebitos, totalLabelStyle, totalMoneyStyle);
            escribirTotalDetalle(sheet, rowIndex, "Total creditos", totalCreditosFinal, totalLabelStyle, totalMoneyStyle);

            aplicarAjustesTabla(sheet, headerRowIndex, totalColumnas);
            ajustarColumnas(sheet, totalColumnas);

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

    private int escribirTitulo(
            XSSFSheet sheet,
            int rowIndex,
            String titulo,
            int totalColumnas,
            CellStyle titleStyle,
            CellStyle subtitleStyle
    ) {
        Row titleRow = sheet.createRow(rowIndex);
        titleRow.setHeightInPoints(28f);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(titulo);
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, totalColumnas - 1));
        for (int i = 1; i < totalColumnas; i++) {
            Cell mergedCell = titleRow.createCell(i);
            mergedCell.setCellStyle(titleStyle);
        }

        rowIndex++;

        Row subtitleRow = sheet.createRow(rowIndex);
        Cell subtitleCell = subtitleRow.createCell(0);
        subtitleCell.setCellValue("Dulces & Dulces  |  Generado: " + DATE_FORMATTER.format(LocalDate.now()));
        subtitleCell.setCellStyle(subtitleStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, totalColumnas - 1));
        for (int i = 1; i < totalColumnas; i++) {
            Cell mergedCell = subtitleRow.createCell(i);
            mergedCell.setCellStyle(subtitleStyle);
        }

        return rowIndex + 2;
    }

    private int escribirTotalDetalle(
            XSSFSheet sheet,
            int rowIndex,
            String label,
            BigDecimal valor,
            CellStyle totalLabelStyle,
            CellStyle totalMoneyStyle
    ) {
        Row row = sheet.createRow(rowIndex++);
        Cell labelCell = row.createCell(3);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(totalLabelStyle);

        var cell = row.createCell(4);
        cell.setCellValue(valorNumerico(valor));
        cell.setCellStyle(totalMoneyStyle);
        return rowIndex;
    }

    private CellStyle crearHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.GREY_80_PERCENT.getIndex());
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle crearDateStyle(XSSFWorkbook workbook) {
        return crearDateBodyStyle(workbook, false);
    }

    private CellStyle crearDateBodyStyle(XSSFWorkbook workbook, boolean alternada) {
        CellStyle style = workbook.createCellStyle();
        DataFormat dataFormat = workbook.createDataFormat();
        style.setDataFormat(dataFormat.getFormat("dd/mm/yyyy"));
        style.setBorderBottom(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private CellStyle crearMoneyStyle(XSSFWorkbook workbook) {
        return crearMoneyBodyStyle(workbook, false);
    }

    private CellStyle crearMoneyBodyStyle(XSSFWorkbook workbook, boolean alternada) {
        CellStyle style = workbook.createCellStyle();
        DataFormat dataFormat = workbook.createDataFormat();
        style.setDataFormat(dataFormat.getFormat("$ #,##0.00;[Red]-$ #,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle crearBodyStyle(XSSFWorkbook workbook, boolean alternada) {
        CellStyle style = workbook.createCellStyle();
        DataFormat dataFormat = workbook.createDataFormat();
        style.setDataFormat(dataFormat.getFormat("@"));
        style.setBorderBottom(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setAlignment(HorizontalAlignment.LEFT);
        return style;
    }

    private CellStyle crearTitleStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(IndexedColors.BLACK.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle crearSubtitleStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(false);
        font.setFontHeightInPoints((short) 10);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle crearTotalLabelStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.GREY_80_PERCENT.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle crearTotalMoneyStyle(XSSFWorkbook workbook) {
        CellStyle style = crearTotalLabelStyle(workbook);
        DataFormat dataFormat = workbook.createDataFormat();
        style.setDataFormat(dataFormat.getFormat("$ #,##0.00;[Red]-$ #,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private boolean esFilaPar(int rowIndex) {
        return rowIndex % 2 == 0;
    }

    private void aplicarAjustesTabla(XSSFSheet sheet, int headerRow, int totalColumnas) {
        sheet.createFreezePane(0, headerRow + 1);
        sheet.setAutoFilter(new CellRangeAddress(headerRow, headerRow, 0, totalColumnas - 1));
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
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            PDPage page = new PDPage(crearPaginaHorizontal());
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float y = page.getMediaBox().getHeight() - PDF_MARGIN;
                y = dibujarTitulo(document, contentStream, bold, titulo, y);

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

    private float dibujarTitulo(PDDocument document, PDPageContentStream contentStream, PDFont font, String titulo, float y) throws IOException {
        float logoOffset = dibujarLogoPdf(document, contentStream, y);
        float titleX = PDF_MARGIN + logoOffset;

        escribirTexto(contentStream, font, PDF_TITLE_SIZE, titleX, y, titulo, PDF_COLOR_TITLE);
        escribirTexto(
            contentStream,
            new PDType1Font(Standard14Fonts.FontName.HELVETICA),
            PDF_SUBTITLE_SIZE,
            titleX,
            y - 14f,
            "Dulces & Dulces  |  Generado: " + formatearFecha(LocalDate.now()),
            new Color(130, 130, 130)
        );
        dibujarLinea(contentStream, PDF_MARGIN, y - 22f, crearPaginaHorizontal().getWidth() - PDF_MARGIN, y - 22f, new Color(200, 200, 200), 0.5f);
        return y - 36f;
    }

    private float dibujarLogoPdf(PDDocument document, PDPageContentStream contentStream, float y) {
        try (InputStream logoStream = getClass().getResourceAsStream(PDF_LOGO_RESOURCE)) {
            if (logoStream == null) {
                return 0f;
            }

            BufferedImage logo = ImageIO.read(logoStream);
            if (logo == null) {
                return 0f;
            }

            PDImageXObject image = LosslessFactory.createFromImage(document, logo);
            float logoHeight = 22f;
            float logoWidth = logoHeight * ((float) logo.getWidth() / (float) logo.getHeight());
            contentStream.drawImage(image, PDF_MARGIN, y - logoHeight + 4f, logoWidth, logoHeight);
            return logoWidth + 8f;
        } catch (IOException ex) {
            return 0f;
        }
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
            escribirTextoCelda(contentStream, bold, 9f, x, y, etiquetaWidth, PDF_ROW_HEIGHT, fila.etiqueta(), false, PDF_COLOR_HEADER);
            escribirTextoCelda(contentStream, regular, 9f, x + etiquetaWidth, y, valorWidth, PDF_ROW_HEIGHT, fila.valor(), false, PDF_COLOR_TEXT);
            dibujarLinea(contentStream, x, y - PDF_ROW_HEIGHT, x + etiquetaWidth + valorWidth, y - PDF_ROW_HEIGHT, PDF_COLOR_BORDER, 0.4f);
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
                y = dibujarTitulo(document, contentStream, bold, titulo, y);
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
                y = dibujarTitulo(document, contentStream, bold, titulo, y);
            }

            float resumenX = PDF_MARGIN + Math.max(0f, tableWidth - 280f);
            dibujarLinea(contentStream, resumenX, y + 2f, resumenX + 280f, y + 2f, PDF_COLOR_HEADER_BORDER, 0.8f);
            for (PdfResumenFila fila : resumen) {
                escribirTextoCelda(contentStream, bold, 9f, resumenX, y, 150f, PDF_ROW_HEIGHT, fila.etiqueta(), false, PDF_COLOR_HEADER);
                escribirTextoCelda(contentStream, regular, 9f, resumenX + 150f, y, 130f, PDF_ROW_HEIGHT, fila.valor(), true, PDF_COLOR_TEXT);
                dibujarLinea(contentStream, resumenX, y - PDF_ROW_HEIGHT, resumenX + 280f, y - PDF_ROW_HEIGHT, PDF_COLOR_BORDER, 0.4f);
                y -= PDF_ROW_HEIGHT;
            }
        }

        contentStream.close();
        return y;
    }

    private float dibujarFilaCabecera(PDPageContentStream contentStream, PDFont font, String[] columnas, float[] anchos, float y) throws IOException {
        float x = PDF_MARGIN;
        float totalWidth = 0f;
        for (float ancho : anchos) totalWidth += ancho;
        for (int i = 0; i < columnas.length; i++) {
            escribirTextoCelda(contentStream, font, 9f, x, y, anchos[i], PDF_ROW_HEIGHT, columnas[i], i >= anchos.length - 2, PDF_COLOR_HEADER);
            x += anchos[i];
        }
        
        dibujarLinea(contentStream, PDF_MARGIN, y, PDF_MARGIN + totalWidth, y, PDF_COLOR_BORDER, 0.5f);
        
        dibujarLinea(contentStream, PDF_MARGIN, y - PDF_ROW_HEIGHT, PDF_MARGIN + totalWidth, y - PDF_ROW_HEIGHT, PDF_COLOR_HEADER_BORDER, 0.8f);
        return y - PDF_ROW_HEIGHT;
    }

    private void dibujarFilaDatos(PDPageContentStream contentStream, PDFont font, String[] fila, float[] anchos, float y, int rowIndex) throws IOException {
        float x = PDF_MARGIN;
        float totalWidth = 0f;
        for (float ancho : anchos) totalWidth += ancho;
        for (int i = 0; i < anchos.length; i++) {
            boolean alineadoDerecha = i >= anchos.length - 2;
            String valor = i < fila.length ? fila[i] : "";
            escribirTextoCelda(contentStream, font, PDF_FONT_SIZE, x, y, anchos[i], PDF_ROW_HEIGHT, valor, alineadoDerecha, PDF_COLOR_TEXT);
            x += anchos[i];
        }
        dibujarLinea(contentStream, PDF_MARGIN, y - PDF_ROW_HEIGHT, PDF_MARGIN + totalWidth, y - PDF_ROW_HEIGHT, PDF_COLOR_BORDER, 0.4f);
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
        float textY = y - ((height + fontSize * 0.35f) / 2f);
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