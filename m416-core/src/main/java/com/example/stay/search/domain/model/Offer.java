package com.example.stay.search.domain.model;

import com.example.stay.common.exception.InvalidOfferException;

public record Offer(Long stayId, String stayName, Long roomTypeId, String roomTypeName,
                    int maxOccupancy, int availableRooms, String supplier,
                    boolean breakfastIncluded, Money price) {
    public Offer {
        if (stayId == null || stayId <= 0 || roomTypeId == null || roomTypeId <= 0) {
            throw new InvalidOfferException("숙소·객실 ID는 양수여야 합니다.");
        }
        if (stayName == null || stayName.isBlank() || roomTypeName == null || roomTypeName.isBlank()) {
            throw new InvalidOfferException("숙소·객실 이름은 필수입니다.");
        }
        if (supplier == null || supplier.isBlank() || price == null) {
            throw new InvalidOfferException("공급사와 가격은 필수입니다.");
        }
        if (maxOccupancy < 1 || availableRooms < 0) {
            throw new InvalidOfferException("수용 인원은 양수, 재고는 0 이상이어야 합니다.");
        }
    }
}
