package co.dulcesydulces.provedor_backend.domain.entidades;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "Usuarios")
public class Usuarios {

    @Id
    @Column(name="codigo", length = 30)
    private String codigo;

    @Column(name="rol", nullable = false, length = 15)
    private String rol;

    @Column(name="nombre_usuario", nullable = false, length = 100)
    @JsonProperty("nombre_usuario")
    private String nombreUsuario;

    @Column(name="email", length = 120)
    private String email;

    @Column(name="password_hash", length = 150)
    private String password_hash;

    @Column(name="estado_u", length = 150)
    private String estado_u;

    @Column(name="creado_en", length = 150)
    private LocalDateTime creado_en;


    // getters/setters
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public String getEmail() { return email; }
    public void setEmail (String email) { this.email = email; }

    public String getPassword_hash() { return password_hash; }
    public void setPassword_hash(String password_hash) { this.password_hash = password_hash; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    
    public String getEstado_u() { return estado_u; }
    public void setEstado_u(String estado_u) { this.estado_u = estado_u; }

   

    

@PrePersist
public void prePersist() {
  if (this.creado_en == null) {
    this.creado_en = java.time.LocalDateTime.now();
  }
  if (this.estado_u == null || this.estado_u.isBlank()) {
    this.estado_u = "activo";
  }
}

@PreUpdate
public void preUpdate() {
    java.time.LocalDateTime.now();
}
}