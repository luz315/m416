package com.example.stay.infrastructure.catalog.persistence.mapper;

import com.example.stay.catalog.domain.model.RoomMapping;
import com.example.stay.infrastructure.catalog.persistence.entity.RoomMappingRecord;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RoomMappingMapper {
    public static RoomMapping toDomain(RoomMappingRecord record) {
        return new RoomMapping(
                record.getId(),
                record.getStayId(),
                record.getRoomCode(),
                record.isActive()
        );
    }

    public static RoomMappingRecord toRecord(RoomMapping domain) {
        return new RoomMappingRecord(
                domain.getId(),
                domain.getStayId(),
                domain.getRoomCode(),
                domain.isActive()
        );
    }
}
