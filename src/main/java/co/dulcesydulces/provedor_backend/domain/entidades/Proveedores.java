package co.dulcesydulces.provedor_backend.domain.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "Proveedores")
public class Proveedores {

    @Id
    @Column(name = "usuario_codigo", nullable = false, length = 100)
    private String usuarioCodigo;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_codigo", referencedColumnName = "codigo", insertable = false, updatable = false)
    private Usuarios usuario;

    public String getUsuarioCodigo() {
        return usuarioCodigo;
    }

    public void setUsuarioCodigo(String usuarioCodigo) {
        this.usuarioCodigo = usuarioCodigo;
    }

    public Usuarios getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuarios usuario) {
        this.usuario = usuario;
    }
}