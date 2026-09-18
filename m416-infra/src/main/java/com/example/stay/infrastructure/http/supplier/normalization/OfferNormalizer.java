package com.example.stay.infrastructure.http.supplier.normalization;

import com.example.stay.catalog.application.port.out.RoomMappingRepository;
import com.example.stay.catalog.application.port.out.StayMappingRepository;
import com.example.stay.catalog.domain.model.StayMapping;
import com.example.stay.infrastructure.http.supplier.a.dto.SupplierAResponse;
import com.example.stay.infrastructure.http.supplier.b.dto.SupplierBResponse;
import com.example.stay.search.domain.model.*;
import com.example.stay.search.domain.service.InventoryPolicy;
import java.time.LocalDate;
import java.util.*;

public final class OfferNormalizer {
    private final StayMappingRepository stays;
    private final RoomMappingRepository rooms;

    public OfferNormalizer(StayMappingRepository stays, RoomMappingRepository rooms) {
        this.stays = stays;
        this.rooms = rooms;
    }
    public Offer fromA(SupplierAResponse.Item item, SearchCriteria criteria) {
        Map<LocalDate, Integer> inventory = new HashMap<>();
        long total = 0;
        for (var day : item.dailyRates()) {
            addDay(inventory, day.date(), day.remainingRooms());
            if (day.nightlyRate() == null || day.taxAmount() == null || day.nightlyRate() < 0 || day.taxAmount() < 0)
                throw new IllegalArgumentException("유효하지 않은 일별 요금");
            total = Math.addExact(total, Math.addExact(day.nightlyRate(), day.taxAmount()));
        }
        return offer("A", item.hotelCode(), item.hotelName(), item.roomTypeCode(), item.roomTypeName(), item.maxOccupancy(), item.breakfastIncluded(),
                new Money(item.currency(), total), InventoryPolicy.availableRooms(criteria, inventory));
    }
    public Offer fromB(SupplierBResponse.Item item, SearchCriteria criteria) {
        if (!Boolean.TRUE.equals(item.taxIncluded()) || item.totalPrice() == null)
            throw new IllegalArgumentException("세금 포함 총액을 확인할 수 없습니다.");
        Map<LocalDate, Integer> inventory = new HashMap<>();
        for (var day : item.inventory()) addDay(inventory, day.date(), day.remainingRooms());
        return offer("B", item.propertyId(), item.propertyName(), item.roomId(), item.roomName(), item.maxOccupancy(), item.breakfastIncluded(),
                new Money(item.currency(), item.totalPrice()), InventoryPolicy.availableRooms(criteria, inventory));
    }
    private Offer offer(String supplier, String stayCode, String stayName, String roomCode, String roomName,
                        Integer occupancy, Boolean breakfast, Money money, int inventory) {
        var stay = stays.findBySupplierAndStayCode(supplier, stayCode)
                .filter(StayMapping::isActive)
                .orElseThrow(() -> new IllegalArgumentException("미등록 숙소 매핑"));
        var mapping = rooms.findByStayIdAndRoomCode(stay.getId(), roomCode)
                .filter(room -> room.isActive())
                .orElseThrow(() -> new IllegalArgumentException("미등록 객실 매핑"));
        if (occupancy == null || occupancy < 1 || breakfast == null)
            throw new IllegalArgumentException("객실 기준 정보가 잘못되었습니다.");
        return new Offer(mapping.getStayId(), stayName, mapping.getId(), roomName,
                occupancy, inventory, supplier, breakfast, money);
    }
    private static void addDay(Map<LocalDate, Integer> inventory, LocalDate date, Integer remaining) {
        if (date == null || remaining == null || remaining < 0 || inventory.putIfAbsent(date, remaining) != null)
            throw new IllegalArgumentException("중복 또는 잘못된 재고 날짜");
    }
}
