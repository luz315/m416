package com.example.stay.infrastructure.catalog.persistence.repository;

import com.example.stay.catalog.application.port.out.CatalogStateRepository;
import com.example.stay.catalog.domain.model.CatalogState;
import com.example.stay.infrastructure.catalog.persistence.entity.CatalogStateRecord;
import com.example.stay.infrastructure.catalog.persistence.mapper.CatalogStateMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CatalogStateRepositoryImpl implements CatalogStateRepository {

    private final CatalogStateJpaRepository jpaRepository;

    @Override
    public Optional<CatalogState> findBySupplier(String supplier) {
        return jpaRepository.findBySupplier(supplier)
                .map(CatalogStateMapper::toDomain);
    }

    @Override
    public CatalogState save(CatalogState state) {
        CatalogStateRecord record = CatalogStateMapper.toRecord(state);
        return CatalogStateMapper.toDomain(jpaRepository.save(record));
    }

    @Override
    public boolean existsBySupplier(String supplier) {
        return jpaRepository.existsBySupplier(supplier);
    }
}
