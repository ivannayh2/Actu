package co.dulcesydulces.provedor_backend.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class EgresoCreateRequest {

    @NotBlank
    @Size(max = 30)
    private String numeroEgreso;

    @NotBlank
    @Size(max = 120)
    private String proveedor;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal valorEgreso;

    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaDocumento;

    // getters/setters
    public String getNumeroEgreso() { return numeroEgreso; }
    public void setNumeroEgreso(String numeroEgreso) { this.numeroEgreso = numeroEgreso; }

    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }

    public BigDecimal getValorEgreso() { return valorEgreso; }
    public void setValorEgreso(BigDecimal valorEgreso) { this.valorEgreso = valorEgreso; }

    public LocalDate getFechaDocumento() { return fechaDocumento; }
    public void setFechaDocumento(LocalDate fechaDocumento) { this.fechaDocumento = fechaDocumento; }
}
