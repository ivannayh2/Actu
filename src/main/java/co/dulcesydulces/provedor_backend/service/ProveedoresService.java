package co.dulcesydulces.provedor_backend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import co.dulcesydulces.provedor_backend.domain.entidades.Proveedores;
import co.dulcesydulces.provedor_backend.repository.ProveedoresRepository;

@Service
public class ProveedoresService {

    private final ProveedoresRepository repo;

    public ProveedoresService(ProveedoresRepository repo) {
        this.repo = repo;
    }

    public List<Proveedores> getListaEnOptions() {
        return repo.findAllByOrderByUsuarioCodigoAsc();
    }
}