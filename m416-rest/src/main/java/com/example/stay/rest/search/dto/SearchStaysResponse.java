package com.example.stay.rest.search.dto;

import com.example.stay.search.domain.model.*;
import java.util.List;

public record SearchStaysResponse(String status, boolean partialFailure, List<StayResponse> offers,
                                  List<FailureResponse> failures) {
    public static SearchStaysResponse from(SearchResult result) {
        String status = result.allSuppliersFailed() ? "ALL_FAILED" : result.partialFailure() ? "PARTIAL_FAILURE" : "SUCCESS";
        return new SearchStaysResponse(status, result.partialFailure(), result.offers().stream().map(StayResponse::from).toList(),
                result.failures().stream().map(f -> new FailureResponse(f.supplier(), f.code().name())).toList());
    }
    public record StayResponse(Long stayId, String stayName, Long roomTypeId, String roomTypeName,
                               int maxOccupancy, int availableRooms, String supplier, boolean breakfastIncluded,
                               PriceResponse price) {
        static StayResponse from(Offer offer) {
            return new StayResponse(offer.stayId(), offer.stayName(), offer.roomTypeId(), offer.roomTypeName(),
                    offer.maxOccupancy(), offer.availableRooms(), offer.supplier(), offer.breakfastIncluded(),
                    new PriceResponse(offer.price().currency(), offer.price().totalIncludingTax(), true, "STAY_PER_ROOM"));
        }
    }
    public record PriceResponse(String currency, long totalIncludingTax, boolean taxIncluded, String basis) {}
    public record FailureResponse(String supplier, String code) {}
}
