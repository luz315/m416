package com.example.stay.infrastructure.catalog.persistence.repository;

import com.example.stay.infrastructure.catalog.persistence.entity.StayMappingRecord;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StayMappingJpaRepository extends JpaRepository<StayMappingRecord, Long> {

    Optional<StayMappingRecord> findBySupplierAndStayCode(String supplier, String stayCode);

    List<StayMappingRecord> findAllBySupplier(String supplier);

    List<StayMappingRecord> findAllBySupplierAndActiveTrueOrderByStayCode(String supplier);
}
