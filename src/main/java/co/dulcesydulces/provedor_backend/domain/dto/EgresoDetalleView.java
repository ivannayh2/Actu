package co.dulcesydulces.provedor_backend.domain.dto;

import java.math.BigDecimal;

import co.dulcesydulces.provedor_backend.domain.entidades.EgresoPlano;

public class EgresoDetalleView {

    private EgresoPlano egreso;
    private String doctoSaBase;
    private boolean mostrarNotaRelacionada;
    private String doctoProveedorRelacionado;
    private String notaPlanoRelacionada;

    public EgresoDetalleView() {
    }

    public EgresoDetalleView(EgresoPlano egreso) {
        this.egreso = egreso;
    }

    public EgresoPlano getEgreso() {
        return egreso;
    }

    public void setEgreso(EgresoPlano egreso) {
        this.egreso = egreso;
    }

    public String getDoctoSaBase() {
        return doctoSaBase;
    }

    public void setDoctoSaBase(String doctoSaBase) {
        this.doctoSaBase = doctoSaBase;
    }

    public boolean isMostrarNotaRelacionada() {
        return mostrarNotaRelacionada;
    }

    public void setMostrarNotaRelacionada(boolean mostrarNotaRelacionada) {
        this.mostrarNotaRelacionada = mostrarNotaRelacionada;
    }

    public String getDoctoProveedorRelacionado() {
        return doctoProveedorRelacionado;
    }

    public void setDoctoProveedorRelacionado(String doctoProveedorRelacionado) {
        this.doctoProveedorRelacionado = doctoProveedorRelacionado;
    }

    public String getNotaPlanoRelacionada() {
        return notaPlanoRelacionada;
    }

    public void setNotaPlanoRelacionada(String notaPlanoRelacionada) {
        this.notaPlanoRelacionada = notaPlanoRelacionada;
    }

    public String getDoctoSa() {
        return egreso != null ? egreso.getDoctoSa() : null;
    }

    public String getDoctoCausacion() {
        return egreso != null ? egreso.getDoctoCausacion() : null;
    }

    public BigDecimal getVlrEgreso() {
        return egreso != null ? egreso.getVlrEgreso() : null;
    }

    public BigDecimal getProntoPago() {
        return egreso != null ? egreso.getProntoPago() : null;
    }

    public BigDecimal getValorDocto() {
        return egreso != null ? egreso.getValorDocto() : null;
    }

    public String getNotas() {
        return egreso != null ? egreso.getNotas() : null;
    }
}