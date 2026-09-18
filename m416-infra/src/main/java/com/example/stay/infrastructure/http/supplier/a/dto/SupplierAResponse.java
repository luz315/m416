package com.example.stay.infrastructure.http.supplier.a.dto;

import java.time.LocalDate;
import java.util.List;

public final class SupplierAResponse {
    private SupplierAResponse() {
    }

    public record Hotels(List<Hotel> items) {
    }

    public record Hotel(String hotelCode, String hotelName, List<Room> roomTypes) {
    }

    public record Room(String roomTypeCode, String roomTypeName, Integer maxOccupancy) {
    }

    public record Availability(List<Item> items) {
    }

    public record Item(
            String hotelCode,
            String hotelName,
            String roomTypeCode,
            String roomTypeName,
            Integer maxOccupancy,
            Boolean breakfastIncluded,
            String currency,
            List<DailyRate> dailyRates
    ) {
    }

    public record DailyRate(LocalDate date, Integer remainingRooms, Long nightlyRate, Long taxAmount) {
    }
}
