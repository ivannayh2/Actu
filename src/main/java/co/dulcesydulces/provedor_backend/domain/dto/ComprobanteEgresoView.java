package co.dulcesydulces.provedor_backend.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ComprobanteEgresoView {

    private final Long uploadId;
    private final String doctoEgreso;
    private final LocalDate fechaEgreso;
    private final String tercero;
    private final String razonSocial;
    private final BigDecimal valorEgreso;
    private final String notas;

    public ComprobanteEgresoView(
        Long uploadId,
        String doctoEgreso,
        LocalDate fechaEgreso,
        String tercero,
        String razonSocial,
        BigDecimal valorEgreso,
        String notas
    ) {
        this.uploadId = uploadId;
        this.doctoEgreso = doctoEgreso;
        this.fechaEgreso = fechaEgreso;
        this.tercero = tercero;
        this.razonSocial = razonSocial;
        this.valorEgreso = valorEgreso;
        this.notas = notas;
    }

    public Long getUploadId() {
        return uploadId;
    }

    public String getDoctoEgreso() {
        return doctoEgreso;
    }

    public LocalDate getFechaEgreso() {
        return fechaEgreso;
    }

    public String getTercero() {
        return tercero;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public BigDecimal getValorEgreso() {
        return valorEgreso;
    }

    public String getNotas() {
        return notas;
    }
}