package com.example.stay.infrastructure;

import com.example.stay.catalog.domain.model.RoomMapping;
import com.example.stay.catalog.domain.model.CatalogState;
import com.example.stay.catalog.domain.model.StayMapping;
import com.example.stay.infrastructure.catalog.persistence.mapper.RoomMappingMapper;
import com.example.stay.infrastructure.catalog.persistence.mapper.CatalogStateMapper;
import com.example.stay.infrastructure.catalog.persistence.mapper.StayMappingMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CatalogMapperTest {
    @Test
    @DisplayName("양방향 매핑은 기존 ID·코드·비활성 상태·동기화 시각을 보존한다")
    void preservesPersistedState() {
        var stay = StayMappingMapper.toDomain(StayMappingMapper.toRecord(
                new StayMapping(10L, "A", "H1", false)));
        assertEquals(10L, stay.getId());
        assertEquals("A", stay.getSupplier());
        assertEquals("H1", stay.getStayCode());
        assertFalse(stay.isActive());

        var room = RoomMappingMapper.toDomain(RoomMappingMapper.toRecord(
                new RoomMapping(20L, 10L, "R1", false)));
        assertEquals(20L, room.getId());
        assertEquals(10L, room.getStayId());
        assertEquals("R1", room.getRoomCode());
        assertFalse(room.isActive());

        var time = LocalDateTime.of(2026, 9, 17, 12, 30);
        var state = CatalogStateMapper.toDomain(CatalogStateMapper.toRecord(
                new CatalogState(30L, "A", time)));
        assertEquals(30L, state.getId());
        assertEquals("A", state.getSupplier());
        assertEquals(time, state.getSyncedAt());
    }
}
