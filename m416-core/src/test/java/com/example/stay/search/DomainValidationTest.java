package com.example.stay.search;

import com.example.stay.catalog.domain.model.*;
import com.example.stay.common.exception.StayBaseException;
import com.example.stay.search.domain.model.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.*;

class DomainValidationTest {
    private final LocalDateTime time = LocalDateTime.of(2026, 9, 17, 12, 0);

    @TestFactory
    @DisplayName("도메인은 잘못된 필수 값과 복원 ID를 거부한다")
    Stream<DynamicTest> invalidInputs() {
        return Stream.of(
                invalid("빈 공급사", () -> StayMapping.create(" ", "H1")),
                invalid("숙소 코드 누락", () -> StayMapping.create("A", null)),
                invalid("숙소 ID 음수", () -> new StayMapping(-1L, "A", "H1", true)),
                invalid("객실의 숙소 ID 누락", () -> RoomMapping.create(null, "R1")),
                invalid("객실의 숙소 ID 0", () -> RoomMapping.create(0L, "R1")),
                invalid("빈 객실 코드", () -> RoomMapping.create(1L, " ")),
                invalid("객실 복원 ID 음수", () -> new RoomMapping(-1L, 1L, "R1", true)),
                invalid("동기화 공급사 누락", () -> CatalogState.create(null, time)),
                invalid("동기화 시각 누락", () -> CatalogState.create("A", null)),
                invalid("동기화 복원 ID 0", () -> new CatalogState(0L, "A", time)),
                invalid("통화 누락", () -> new Money(null, 0)),
                invalid("음수 금액", () -> new Money("KRW", -1)),
                invalid("가격 누락", () -> offer(1, 0, null)),
                invalid("수용 인원 0", () -> offer(0, 0, new Money("KRW", 0))),
                invalid("음수 재고", () -> offer(1, -1, new Money("KRW", 0))),
                invalid("실패 코드 누락", () -> new SupplierFailure("A", null)),
                invalid("결과 목록 누락", () -> new SearchResult(null, List.of(), 1)),
                invalid("음수 성공 공급사 수", () -> new SearchResult(List.of(), List.of(), -1))
        );
    }

    @Test
    @DisplayName("신규 ID 미지정과 0 재고·0 금액·빈 성공 결과는 허용한다")
    void validBoundaries() {
        assertNull(StayMapping.create("A", "H1").getId());
        assertNull(RoomMapping.create(1L, "R1").getId());
        assertNull(CatalogState.create("A", time).getId());
        assertEquals(0, offer(1, 0, new Money("KRW", 0)).availableRooms());
        assertFalse(new SearchResult(List.of(), List.of(), 1).allSuppliersFailed());
    }

    @Test
    @DisplayName("잘못된 동기화 시각으로 변경해도 기존 상태를 보존한다")
    void failedUpdatePreservesState() {
        var state = new CatalogState(1L, "A", time);
        assertThrows(StayBaseException.class, () -> state.updateSyncedAt(null));
        assertEquals(time, state.getSyncedAt());
    }

    private DynamicTest invalid(String name, Executable action) {
        return DynamicTest.dynamicTest(name, () -> assertThrows(StayBaseException.class, action));
    }

    private Offer offer(int occupancy, int inventory, Money price) {
        return new Offer(1L, "숙소", 2L, "객실", occupancy, inventory, "A", false, price);
    }
}
