package kz.iitu.springlab.service;

import kz.iitu.springlab.audit.Audited;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

@Service
public class CatalogService {
    private final CatalogRemovalService removalService;

    public CatalogService(CatalogRemovalService removalService) {
        this.removalService = removalService;
    }

    public String findById(long id) {
        sleep(50);
        return "Item no. " + id;
    }

    @Audited(action = "CATALOG_LIST", logArguments = true)
    public List<String> findAll(int limit) {
        sleep(300);
        return IntStream.rangeClosed(1, limit)
                .mapToObj(id -> "Item no. " + id)
                .toList();
    }

    @Audited(action = "CATALOG_REMOVE")
    public String remove(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Invalid identifier: " + id);
        }
        return "Removed item no. " + id;
    }

    public String removeTwice(long id) {
        // Calls through this bypass the Spring AOP proxy.
        String first = this.remove(id);
        String second = this.remove(id + 1);
        return first + "; " + second;
    }

    public String removeTwiceFixed(long id) {
        // Crossing into another service bean goes through all matching advice.
        String first = removalService.remove(id);
        String second = removalService.remove(id + 1);
        return first + "; " + second;
    }

    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
