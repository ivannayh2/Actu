package co.dulcesydulces.provedor_backend.domain.dto;

public class HistorialCargaView {
    private final Long id;
    private final String usuario;
    private final String nombreEgresos;
    private final String nombreFacturas;
    private final String nombreNotas;
    private final String fechaCarga;
    private final String movimiento;

    public HistorialCargaView(
        Long id,
        String usuario,
        String nombreEgresos,
        String nombreFacturas,
        String nombreNotas,
        String fechaCarga,
        String movimiento
    ) {
        this.id = id;
        this.usuario = usuario;
        this.nombreEgresos = nombreEgresos;
        this.nombreFacturas = nombreFacturas;
        this.nombreNotas = nombreNotas;
        this.fechaCarga = fechaCarga;
        this.movimiento = movimiento;
    }

    public Long getId() { return id; }
    public String getUsuario() { return usuario; }
    public String getNombreEgresos() { return nombreEgresos; }
    public String getNombreFacturas() { return nombreFacturas; }
    public String getNombreNotas() { return nombreNotas; }
    public String getFechaCarga() { return fechaCarga; }
    public String getMovimiento() { return movimiento; }
}