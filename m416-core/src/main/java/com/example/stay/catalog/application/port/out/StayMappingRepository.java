package com.example.stay.catalog.application.port.out;

import com.example.stay.catalog.domain.model.StayMapping;
import java.util.List;
import java.util.Optional;

public interface StayMappingRepository {
    Optional<StayMapping> findBySupplierAndStayCode(String supplier, String stayCode);
    List<StayMapping> findAllBySupplier(String supplier);
    List<StayMapping> findAllActiveBySupplier(String supplier);
    StayMapping save(StayMapping mapping);
}
