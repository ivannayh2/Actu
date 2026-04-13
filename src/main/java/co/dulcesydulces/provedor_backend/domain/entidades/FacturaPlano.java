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
@Table(name = "facturas_plano")
public class FacturaPlano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "upload_id", nullable = false)
    private Uploads uploads;

    @Column(name = "tipo_docto", length = 50)
    private String tipoDocto;

    @Column(name = "docto_causacion", length = 50)
    private String doctoCausacion;

    @Column(name = "documento", length = 50)
    private String documento;

    @Column(name = "docto_referencia", length = 80)
    private String doctoReferencia;

    @Column(name = "proveedor", length = 50)
    private String proveedor;

    @Column(name = "razon_social_proveedor", length = 200)
    private String razonSocialProveedor;

    @Column(name = "fecha")
    private LocalDate fecha;

    @Column(name = "item", length = 50)
    private String item;

    @Column(name = "desc_item", length = 250)
    private String descItem;

    @Column(name = "codigo_barra_principal", length = 80)
    private String codigoBarraPrincipal;

    @Column(name = "um", length = 20)
    private String um;

    @Column(name = "cantidad", precision = 14, scale = 3)
    private BigDecimal cantidad;

    @Column(name = "valor_subtotal", precision = 14, scale = 2)
    private BigDecimal valorSubtotal;

    @Column(name = "valor_imptos", precision = 14, scale = 2)
    private BigDecimal valorImptos;

    @Column(name = "valor_dsctos", precision = 14, scale = 2)
    private BigDecimal valorDsctos;

    @Column(name = "valor_neto", precision = 14, scale = 2)
    private BigDecimal valorNeto;

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

    public void setUploads(Uploads uploads) {
        this.uploads = uploads;
    }

    public String getTipoDocto() {
        return tipoDocto;
    }

    public void setTipoDocto(String tipoDocto) {
        this.tipoDocto = tipoDocto;
    }
    
    public String getDoctoCausacion() {
        return doctoCausacion;
    }

    public void setDoctoCausacion(String doctoCausacion) {
        this.doctoCausacion = doctoCausacion;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getDoctoReferencia() {
        return doctoReferencia;
    }

    public void setDoctoReferencia(String doctoReferencia) {
        this.doctoReferencia = doctoReferencia;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public String getRazonSocialProveedor() {
        return razonSocialProveedor;
    }

    public void setRazonSocialProveedor(String razonSocialProveedor) {
        this.razonSocialProveedor = razonSocialProveedor;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getDescItem() {
        return descItem;
    }

    public void setDescItem(String descItem) {
        this.descItem = descItem;
    }

    public String getCodigoBarraPrincipal() {
        return codigoBarraPrincipal;
    }

    public void setCodigoBarraPrincipal(String codigoBarraPrincipal) {
        this.codigoBarraPrincipal = codigoBarraPrincipal;
    }

    public String getUm() {
        return um;
    }

    public void setUm(String um) {
        this.um = um;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public void setCantidad(BigDecimal cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getValorSubtotal() {
        return valorSubtotal;
    }

    public void setValorSubtotal(BigDecimal valorSubtotal) {
        this.valorSubtotal = valorSubtotal;
    }

    public BigDecimal getValorImptos() {
        return valorImptos;
    }

    public void setValorImptos(BigDecimal valorImptos) {
        this.valorImptos = valorImptos;
    }

    public BigDecimal getValorDsctos() {
        return valorDsctos;
    }

    public void setValorDsctos(BigDecimal valorDsctos) {
        this.valorDsctos = valorDsctos;
    }

    public BigDecimal getValorNeto() {
        return valorNeto;
    }

    public void setValorNeto(BigDecimal valorNeto) {
        this.valorNeto = valorNeto;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }
}