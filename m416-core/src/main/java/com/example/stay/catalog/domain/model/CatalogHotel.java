package com.example.stay.catalog.domain.model;

import java.util.List;
/** 외부 카탈로그를 기술 독립적으로 옮긴 스냅샷 값. */
public record CatalogHotel(String stayCode, String name, List<CatalogRoom> rooms) {
    public CatalogHotel {
        if (stayCode == null || stayCode.isBlank() || name == null || name.isBlank() || rooms == null)
            throw new IllegalArgumentException("숙소 정보가 잘못되었습니다.");
        rooms = List.copyOf(rooms);
    }
    public record CatalogRoom(String code, String name, int maxOccupancy) {
        public CatalogRoom {
            if (code == null || code.isBlank() || name == null || name.isBlank() || maxOccupancy < 1)
                throw new IllegalArgumentException("객실 정보가 잘못되었습니다.");
        }
    }
}
