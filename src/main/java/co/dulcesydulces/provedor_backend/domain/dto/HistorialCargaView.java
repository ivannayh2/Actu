package co.dulcesydulces.provedor_backend.domain.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class HistorialCargaView {
    private final Long id;
    private final String usuario;
    private final String nombreEgresos;
    private final String nombreFacturas;
    private final String nombreNotas;
    private final LocalDateTime fechaCarga;
    private final String movimiento;

    public HistorialCargaView(
        Long id,
        String usuario,
        String nombreEgresos,
        String nombreFacturas,
        String nombreNotas,
        LocalDateTime fechaCarga,
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
    public LocalDateTime getFechaCarga() { return fechaCarga; }
    public String getMovimiento() { return movimiento; }

    public String getFechaCargaFormateada() {
        if (fechaCarga == null) return "Sin fecha";
        return fechaCarga.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}