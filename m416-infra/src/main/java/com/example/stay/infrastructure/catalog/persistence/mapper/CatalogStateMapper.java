package com.example.stay.infrastructure.catalog.persistence.mapper;

import com.example.stay.catalog.domain.model.CatalogState;
import com.example.stay.infrastructure.catalog.persistence.entity.CatalogStateRecord;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CatalogStateMapper {
    public static CatalogState toDomain(CatalogStateRecord record) {
        return new CatalogState(
                record.getId(),
                record.getSupplier(),
                record.getSyncedAt()
        );
    }

    public static CatalogStateRecord toRecord(CatalogState domain) {
        return new CatalogStateRecord(
                domain.getId(),
                domain.getSupplier(),
                domain.getSyncedAt()
        );
    }
}
