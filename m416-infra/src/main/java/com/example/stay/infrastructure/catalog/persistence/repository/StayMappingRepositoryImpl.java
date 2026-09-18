package com.example.stay.infrastructure.catalog.persistence.repository;

import com.example.stay.catalog.application.port.out.StayMappingRepository;
import com.example.stay.catalog.domain.model.StayMapping;
import com.example.stay.infrastructure.catalog.persistence.entity.StayMappingRecord;
import com.example.stay.infrastructure.catalog.persistence.mapper.StayMappingMapper;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class StayMappingRepositoryImpl implements StayMappingRepository {

    private final StayMappingJpaRepository jpaRepository;

    @Override
    public Optional<StayMapping> findBySupplierAndStayCode(String supplier, String stayCode) {
        return jpaRepository.findBySupplierAndStayCode(supplier, stayCode)
                .map(StayMappingMapper::toDomain);
    }

    @Override
    public List<StayMapping> findAllBySupplier(String supplier) {
        return jpaRepository.findAllBySupplier(supplier)
                .stream()
                .map(StayMappingMapper::toDomain)
                .toList();
    }

    @Override
    public List<StayMapping> findAllActiveBySupplier(String supplier) {
        return jpaRepository.findAllBySupplierAndActiveTrueOrderByStayCode(supplier)
                .stream()
                .map(StayMappingMapper::toDomain)
                .toList();
    }

    @Override
    public StayMapping save(StayMapping stayMapping) {
        StayMappingRecord record = StayMappingMapper.toRecord(stayMapping);
        return StayMappingMapper.toDomain(jpaRepository.save(record));
    }
}
