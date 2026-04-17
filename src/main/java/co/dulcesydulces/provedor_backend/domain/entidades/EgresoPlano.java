package co.dulcesydulces.provedor_backend.domain.entidades;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "egresos_plano")
public class EgresoPlano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "upload_id", nullable = false)
    private Uploads uploads;

    @Column(name = "docto_egreso", length = 50)
    private String doctoEgreso;

    @Column(name = "fecha_egreso")
    private LocalDate fechaEgreso;

    @Column(name = "tercero", length = 50)
    private String tercero;

    @Column(name = "suc", length = 10)
    private String suc;

    @Column(name = "razon_social", length = 200)
    private String razonSocial;

    @Column(name = "docto_sa", length = 80)
    private String doctoSa;

    @Column(name = "docto_causacion", length = 80)
    private String doctoCausacion;

    @Column(name = "vlr_egreso", precision = 14, scale = 2)
    private BigDecimal vlrEgreso;

    @Column(name = "valor_docto", precision = 14, scale = 2)
    private BigDecimal valorDocto;

    @Column(name = "pronto_pago", precision = 14, scale = 2)
    private BigDecimal prontoPago;

    @Column(name = "notas", columnDefinition = "TEXT")
    private String notas;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Uploads getUploads() {
        return uploads;
    }

    public void setUpload(Uploads uploads) {
        this.uploads = uploads;
    }

    public String getDoctoEgreso() {
        return doctoEgreso;
    }

    public void setDoctoEgreso(String doctoEgreso) {
        this.doctoEgreso = doctoEgreso;
    }

    public LocalDate getFechaEgreso() {
        return fechaEgreso;
    }

    public void setFechaEgreso(LocalDate fechaEgreso) {
        this.fechaEgreso = fechaEgreso;
    }

    public String getTercero() {
        return tercero;
    }

    public void setTercero(String tercero) {
        this.tercero = tercero;
    }

    public String getSuc() {
        return suc;
    }

    public void setSuc(String suc) {
        this.suc = suc;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getDoctoSa() {
        return doctoSa;
    }

    public void setDoctoSa(String doctoSa) {
        this.doctoSa = doctoSa;
    }

    public String getDoctoCausacion() {
        return doctoCausacion;
    }

    public void setDoctoCausacion(String doctoCausacion) {
        this.doctoCausacion = doctoCausacion;
    }

    public BigDecimal getVlrEgreso() {
        return vlrEgreso;
    }

    public void setVlrEgreso(BigDecimal vlrEgreso) {
        this.vlrEgreso = vlrEgreso;
    }

    public BigDecimal getProntoPago() {
        return prontoPago;
    }

    public void setProntoPago(BigDecimal prontoPago) {
        this.prontoPago = prontoPago;
    }

    public BigDecimal getValorDocto() {
        return valorDocto;
    }

    public void setValorDocto(BigDecimal valorDocto) {
        this.valorDocto = valorDocto;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }
}