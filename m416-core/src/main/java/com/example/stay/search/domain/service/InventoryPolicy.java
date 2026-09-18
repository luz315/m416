package com.example.stay.search.domain.service;

import com.example.stay.common.exception.InvalidInventoryException;
import com.example.stay.search.domain.model.SearchCriteria;
import java.time.LocalDate;
import java.util.Map;

public final class InventoryPolicy {
    private InventoryPolicy() {}
    public static int availableRooms(SearchCriteria criteria, Map<LocalDate, Integer> inventory) {
        if (inventory.size() != criteria.nights()) throw new InvalidInventoryException("숙박일 재고가 누락되거나 초과되었습니다.");
        int available = Integer.MAX_VALUE;
        for (LocalDate date = criteria.checkIn(); date.isBefore(criteria.checkOut()); date = date.plusDays(1)) {
            Integer remaining = inventory.get(date);
            if (remaining == null || remaining < 0) throw new InvalidInventoryException("유효하지 않은 일별 재고입니다.");
            available = Math.min(available, remaining);
        }
        return available;
    }
}
