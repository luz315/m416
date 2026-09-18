package com.example.stay.infrastructure.catalog.persistence.repository;

import com.example.stay.infrastructure.catalog.persistence.entity.CatalogStateRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogStateJpaRepository extends JpaRepository<CatalogStateRecord, Long> {

    Optional<CatalogStateRecord> findBySupplier(String supplier);

    boolean existsBySupplier(String supplier);
}
