package com.example.stay.infrastructure.catalog.persistence.repository;

import com.example.stay.catalog.application.port.out.RoomMappingRepository;
import com.example.stay.catalog.domain.model.RoomMapping;
import com.example.stay.infrastructure.catalog.persistence.entity.RoomMappingRecord;
import com.example.stay.infrastructure.catalog.persistence.mapper.RoomMappingMapper;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RoomMappingRepositoryImpl implements RoomMappingRepository {

    private final RoomMappingJpaRepository jpaRepository;

    @Override
    public Optional<RoomMapping> findByStayIdAndRoomCode(Long stayId, String roomCode) {
        return jpaRepository.findByStayIdAndRoomCode(stayId, roomCode)
                .map(RoomMappingMapper::toDomain);
    }

    @Override
    public List<RoomMapping> findAllByStayIds(List<Long> stayIds) {
        return jpaRepository.findAllByStayIdIn(stayIds)
                .stream()
                .map(RoomMappingMapper::toDomain)
                .toList();
    }

    @Override
    public RoomMapping save(RoomMapping roomMapping) {
        RoomMappingRecord record = RoomMappingMapper.toRecord(roomMapping);
        return RoomMappingMapper.toDomain(jpaRepository.save(record));
    }
}
