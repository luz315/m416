package com.example.stay.infrastructure.catalog.persistence.repository;

import com.example.stay.infrastructure.catalog.persistence.entity.RoomMappingRecord;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomMappingJpaRepository extends JpaRepository<RoomMappingRecord, Long> {

    Optional<RoomMappingRecord> findByStayIdAndRoomCode(Long stayId, String roomCode);

    List<RoomMappingRecord> findAllByStayIdIn(List<Long> stayIds);
}
