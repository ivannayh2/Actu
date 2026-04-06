package co.dulcesydulces.provedor_backend.domain.entidades;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "egreso")
public class Egreso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_documento", nullable = false)
    private LocalDate fechaDocumento;

    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    @OneToMany(
        mappedBy = "egreso",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private List<EgresoSoportePF> soportes = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public LocalDate getFechaDocumento() {
        return fechaDocumento;
    }

    public void setFechaDocumento(LocalDate fechaDocumento) {
        this.fechaDocumento = fechaDocumento;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public List<EgresoSoportePF> getSoportes() {
        return soportes;
    }

    public void setSoportes(List<EgresoSoportePF> soportes) {
        this.soportes = soportes;
    }

    public void agregarSoporte(EgresoSoportePF soporte) {
        soporte.setEgreso(this);
        this.soportes.add(soporte);
    }

    public void removerSoporte(EgresoSoportePF soporte) {
        soporte.setEgreso(null);
        this.soportes.remove(soporte);
    }
}