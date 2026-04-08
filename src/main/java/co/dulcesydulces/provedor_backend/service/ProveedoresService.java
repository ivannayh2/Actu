package co.dulcesydulces.provedor_backend.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import co.dulcesydulces.provedor_backend.domain.entidades.Proveedores;
import co.dulcesydulces.provedor_backend.repository.ProveedoresRepository;

@Service
public class ProveedoresService {

    private static final Logger logger = LoggerFactory.getLogger(ProveedoresService.class);

    private final ProveedoresRepository repo;

    public ProveedoresService(ProveedoresRepository repo) {
        this.repo = repo;
    }

    public List<Proveedores> getListaEnOptions() {
        try {
            return repo.findAllByOrderByUsuarioCodigoAsc();
        } catch (DataAccessException ex) {
            logger.warn("No fue posible cargar la tabla Proveedores; se usara una lista vacia.", ex);
            return List.of();
        }
    }
}