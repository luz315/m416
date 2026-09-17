package com.example.stay.catalog.domain.model;

import lombok.Getter;

@Getter
public final class RoomMapping {
    private final Long id;
    private final Long stayId;
    private final String roomCode;
    private boolean active;

    public RoomMapping(
            Long id,
            Long stayId,
            String roomCode,
            boolean active
    ) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("ID는 지정된 경우 양수여야 합니다.");
        }
        if (roomCode == null || roomCode.isBlank()) {
            throw new IllegalArgumentException("객실 코드는 필수입니다.");
        }
        if (stayId == null || stayId <= 0) {
            throw new IllegalArgumentException("숙소 ID는 양수여야 합니다.");
        }
        this.id = id;
        this.stayId = stayId;
        this.roomCode = roomCode;
        this.active = active;
    }

    public static RoomMapping create(
            Long stayId,
            String roomCode
    ) {
        return new RoomMapping(
                null,
                stayId,
                roomCode,
                true
        );
    }

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
    }
}
