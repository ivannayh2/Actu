package co.dulcesydulces.provedor_backend.domain.entidades;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Egreso")
public class Egreso {

    @Id
    @Column(name = "numero_egreso", length = 30)
    private String numeroEgreso;

    @Column(name = "proveedor", nullable = false, length = 120)
    private String proveedor;

    @Column(name = "valor_egreso", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorEgreso;

    @Column(name = "fecha_documento", nullable = false)
    private LocalDate fechaDocumento;

    @Column(name = "ruta_documento", length = 300)
    private String rutaDocumento;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    // getters/setters
    public String getNumeroEgreso() { return numeroEgreso; }
    public void setNumeroEgreso(String numeroEgreso) { this.numeroEgreso = numeroEgreso; }

    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }

    public BigDecimal getValorEgreso() { return valorEgreso; }
    public void setValorEgreso(BigDecimal valorEgreso) { this.valorEgreso = valorEgreso; }

    public LocalDate getFechaDocumento() { return fechaDocumento; }
    public void setFechaDocumento(LocalDate fechaDocumento) { this.fechaDocumento = fechaDocumento; }

    public String getRutaDocumento() { return rutaDocumento; }
    public void setRutaDocumento(String rutaDocumento) { this.rutaDocumento = rutaDocumento; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
}
