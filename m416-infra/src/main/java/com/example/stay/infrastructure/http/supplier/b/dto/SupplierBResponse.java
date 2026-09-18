package com.example.stay.infrastructure.http.supplier.b.dto;

import java.time.LocalDate;
import java.util.List;

public final class SupplierBResponse {
    private SupplierBResponse() {
    }

    public record Properties(String resultCode, String resultMessage, PropertyData data) {
    }

    public record PropertyData(List<Property> items) {
    }

    public record Property(String propertyId, String propertyName, List<Room> rooms) {
    }

    public record Room(String roomId, String roomName, Integer maxOccupancy) {
    }

    public record Search(String resultCode, String resultMessage, SearchData data) {
    }

    public record SearchData(List<Item> items) {
    }

    public record Item(
            String propertyId,
            String propertyName,
            String roomId,
            String roomName,
            Integer maxOccupancy,
            Boolean breakfastIncluded,
            String currency,
            Long totalPrice,
            Boolean taxIncluded,
            List<Inventory> inventory
    ) {
    }

    public record Inventory(LocalDate date, Integer remainingRooms) {
    }
}
