package com.example.stay.catalog.application.usecase;

import com.example.stay.search.domain.model.SupplierFailure;
import java.util.List;

public interface SyncSupplierStaysUseCase {
    List<SupplierFailure> execute();
}
