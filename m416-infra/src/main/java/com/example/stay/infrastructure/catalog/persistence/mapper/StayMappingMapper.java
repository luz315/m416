package com.example.stay.infrastructure.catalog.persistence.mapper;

import com.example.stay.catalog.domain.model.StayMapping;
import com.example.stay.infrastructure.catalog.persistence.entity.StayMappingRecord;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StayMappingMapper {
    public static StayMapping toDomain(StayMappingRecord record) {
        return new StayMapping(
                record.getId(),
                record.getSupplier(),
                record.getStayCode(),
                record.isActive()
        );
    }

    public static StayMappingRecord toRecord(StayMapping domain) {
        return new StayMappingRecord(
                domain.getId(),
                domain.getSupplier(),
                domain.getStayCode(),
                domain.isActive()
        );
    }
}
