package kz.iitu.springlab.service;

import kz.iitu.springlab.audit.Audited;
import org.springframework.stereotype.Service;

/** Separate bean for demonstrating that calls through a proxy are intercepted. */
@Service
public class CatalogRemovalService {
    @Audited(action = "CATALOG_REMOVE")
    public String remove(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Invalid identifier: " + id);
        }
        return "Removed item no. " + id;
    }
}
