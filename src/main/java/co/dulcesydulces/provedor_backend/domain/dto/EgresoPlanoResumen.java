package co.dulcesydulces.provedor_backend.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface EgresoPlanoResumen {
    String getDoctoEgreso();
    LocalDate getFechaEgreso();
    String getTercero();
    String getRazonSocial();
    BigDecimal getVlrEgreso();
}