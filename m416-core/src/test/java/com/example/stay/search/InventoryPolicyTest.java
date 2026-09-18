package com.example.stay.search;

import com.example.stay.common.exception.InvalidInventoryException;
import com.example.stay.common.exception.InvalidSearchCriteriaException;
import com.example.stay.search.domain.model.SearchCriteria;
import com.example.stay.search.domain.service.InventoryPolicy;
import org.junit.jupiter.api.*;
import java.time.LocalDate;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class InventoryPolicyTest {
    private final LocalDate start = LocalDate.of(2026, 9, 1);
    @Test @DisplayName("연박 재고는 모든 숙박일의 최솟값이며 0도 그대로 유지한다")
    void minimumAcrossNights() {
        var query = new SearchCriteria(start, start.plusDays(3), 2, 0);
        assertEquals(1, InventoryPolicy.availableRooms(query, Map.of(start, 3, start.plusDays(1), 1, start.plusDays(2), 5)));
        assertEquals(0, InventoryPolicy.availableRooms(query, Map.of(start, 3, start.plusDays(1), 0, start.plusDays(2), 5)));
    }
    @Test @DisplayName("누락 날짜나 체크아웃 날짜를 대신 넣은 응답은 거부한다")
    void missingDate() {
        var query = new SearchCriteria(start, start.plusDays(2), 2, 0);
        assertThrows(InvalidInventoryException.class, () -> InventoryPolicy.availableRooms(query, Map.of(start, 2)));
        assertThrows(InvalidInventoryException.class, () -> InventoryPolicy.availableRooms(query, Map.of(start, 2, start.plusDays(2), 3)));
    }
    @Test @DisplayName("잘못된 기간과 인원은 외부 조회 전에 거부한다")
    void invalidCriteria() {
        assertThrows(InvalidSearchCriteriaException.class, () -> new SearchCriteria(start, start, 2, 0));
        assertThrows(InvalidSearchCriteriaException.class, () -> new SearchCriteria(start, start.plusDays(31), 2, 0));
        assertThrows(InvalidSearchCriteriaException.class, () -> new SearchCriteria(start, start.plusDays(1), 0, 0));
    }
}
