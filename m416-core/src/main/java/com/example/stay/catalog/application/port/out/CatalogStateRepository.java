package com.example.stay.catalog.application.port.out;

import com.example.stay.catalog.domain.model.CatalogState;
import java.util.Optional;

public interface CatalogStateRepository {
    Optional<CatalogState> findBySupplier(String supplier);
    CatalogState save(CatalogState state);
    boolean existsBySupplier(String supplier);
}
