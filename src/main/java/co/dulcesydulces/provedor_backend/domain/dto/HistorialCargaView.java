package co.dulcesydulces.provedor_backend.domain.dto;

public class HistorialCargaView {

    private final Long uploadId;
    private final String usuario;
    private final String nombreEgresos;
    private final String nombreFacturas;
    private final String nombreNotas;
    private final String fechaCarga;

    public HistorialCargaView(
        Long uploadId,
        String usuario,
        String nombreEgresos,
        String nombreFacturas,
        String nombreNotas,
        String fechaCarga
    ) {
        this.uploadId = uploadId;
        this.usuario = usuario;
        this.nombreEgresos = nombreEgresos;
        this.nombreFacturas = nombreFacturas;
        this.nombreNotas = nombreNotas;
        this.fechaCarga = fechaCarga;
    }

    public Long getUploadId() {
        return uploadId;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getNombreEgresos() {
        return nombreEgresos;
    }

    public String getNombreFacturas() {
        return nombreFacturas;
    }

    public String getNombreNotas() {
        return nombreNotas;
    }

    public String getFechaCarga() {
        return fechaCarga;
    }
}