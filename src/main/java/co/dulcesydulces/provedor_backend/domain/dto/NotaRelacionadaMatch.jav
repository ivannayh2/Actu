package co.dulcesydulces.provedor_backend.domain.dto;

import co.dulcesydulces.provedor_backend.domain.entidades.NotaPlano;

public class NotaRelacionadaMatch {

    private final NotaPlano notaPlano;
    private final String campoCoincidencia;

    public NotaRelacionadaMatch(NotaPlano notaPlano, String campoCoincidencia) {
        this.notaPlano = notaPlano;
        this.campoCoincidencia = campoCoincidencia;
    }

    public NotaPlano getNotaPlano() {
        return notaPlano;
    }

    public String getCampoCoincidencia() {
        return campoCoincidencia;
    }
}