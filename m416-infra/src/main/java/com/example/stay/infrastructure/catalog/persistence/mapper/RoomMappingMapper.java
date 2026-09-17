package com.example.stay.infrastructure.catalog.persistence.mapper;

import com.example.stay.catalog.domain.model.CatalogRoomMapping;
import com.example.stay.infrastructure.catalog.persistence.entity.RoomMappingRecord;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RoomMappingMapper {
    public static CatalogRoomMapping toDomain(RoomMappingRecord record) {
        return CatalogRoomMapping.restore(
                record.getId(),
                record.getStayId(),
                record.getRoomCode(),
                record.isActive()
        );
    }

    public static RoomMappingRecord toRecord(CatalogRoomMapping domain) {
        return new RoomMappingRecord(
                domain.getId(),
                domain.getStayId(),
                domain.getRoomCode(),
                domain.isActive()
        );
    }
}
