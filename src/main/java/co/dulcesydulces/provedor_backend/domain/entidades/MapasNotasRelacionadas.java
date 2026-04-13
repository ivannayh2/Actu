package co.dulcesydulces.provedor_backend.domain.entidades;

import java.util.Map;

public class MapasNotasRelacionadas {

    private final Map<String, NotaPlano> porDoctoProveedor;
    private final Map<String, NotaPlano> porNroDocumento;
    private final Map<String, NotaPlano> porReferencia1;

    public MapasNotasRelacionadas(
            Map<String, NotaPlano> porDoctoProveedor,
            Map<String, NotaPlano> porNroDocumento,
            Map<String, NotaPlano> porReferencia1) {

        this.porDoctoProveedor = porDoctoProveedor;
        this.porNroDocumento = porNroDocumento;
        this.porReferencia1 = porReferencia1;
    }

    public Map<String, NotaPlano> getPorDoctoProveedor() {
        return porDoctoProveedor;
    }

    public Map<String, NotaPlano> getPorNroDocumento() {
        return porNroDocumento;
    }

    public Map<String, NotaPlano> getPorReferencia1() {
        return porReferencia1;
    }
}