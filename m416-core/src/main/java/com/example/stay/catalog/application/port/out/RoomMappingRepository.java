package com.example.stay.catalog.application.port.out;

import com.example.stay.catalog.domain.model.RoomMapping;
import java.util.List;
import java.util.Optional;

public interface RoomMappingRepository {

    Optional<RoomMapping> findByStayIdAndRoomCode(Long stayId, String roomCode);

    List<RoomMapping> findAllByStayIds(List<Long> stayIds);

    RoomMapping save(RoomMapping mapping);
}
