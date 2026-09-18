package com.example.stay.mock;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class MockSupplierController {
    private final Map<String, String> modes = new ConcurrentHashMap<>();
    @PostMapping("/control/{supplier}/mode")
    public Map<String, String> mode(@PathVariable String supplier, @RequestParam String value) {
        if (!Set.of("a", "b").contains(supplier) || !Set.of("normal", "error", "no-response").contains(value))
            throw new IllegalArgumentException("지원하지 않는 모드");
        modes.put(supplier, value); return Map.of("supplier", supplier, "mode", value);
    }
    @GetMapping("/a/v1/hotels")
    public ResponseEntity<?> hotels(@RequestHeader(value="X-Api-Key", required=false) String key) {
        if (!authorized(key)) return aError(401, "UNAUTHORIZED");
        return ResponseEntity.ok(Map.of("items", List.of(
                Map.of("hotelCode", "A-10023", "hotelName", "Harbor House", "roomTypes", List.of(Map.of("roomTypeCode", "DLX-TWN", "roomTypeName", "Deluxe Twin", "maxOccupancy", 2))),
                Map.of("hotelCode", "A-10044", "hotelName", "Park Lodge", "roomTypes", List.of(Map.of("roomTypeCode", "STD-DBL", "roomTypeName", "Standard Double", "maxOccupancy", 2))))));
    }
    @GetMapping("/b/api/properties")
    public ResponseEntity<?> properties(@RequestHeader(value="X-Api-Key", required=false) String key) {
        if (!authorized(key)) return bError("E401");
        return bSuccess(List.of(Map.of("propertyId", "B77120", "propertyName", "Harbor House", "rooms",
                List.of(Map.of("roomId", "R-401", "roomName", "Deluxe Twin", "maxOccupancy", 2)))));
    }
    @GetMapping("/a/v1/availability")
    public DeferredResult<ResponseEntity<?>> availability(@RequestHeader(value="X-Api-Key", required=false) String key,
            @RequestParam String hotelCodes, @RequestParam LocalDate checkIn, @RequestParam LocalDate checkOut,
            @RequestParam int adults, @RequestParam int children) {
        if (!authorized(key)) return completed(aError(401, "UNAUTHORIZED"));
        if (!valid(hotelCodes, checkIn, checkOut, adults, children)) return completed(aError(400, "INVALID_PARAMETER"));
        String mode = modes.getOrDefault("a", "normal");
        if (mode.equals("no-response")) return pending();
        if (mode.equals("error")) return completed(aError(503, "SERVICE_UNAVAILABLE"));
        List<Map<String, Object>> items = new ArrayList<>();
        if (adults + children <= 2) for (String code : hotelCodes.split(",")) {
            if (!Set.of("A-10023", "A-10044").contains(code)) continue;
            boolean first = code.equals("A-10023");
            List<Map<String, Object>> rates = new ArrayList<>();
            int i = 0;
            for (LocalDate day = checkIn; day.isBefore(checkOut); day = day.plusDays(1), i++) {
                long net = first ? (i == 1 ? 150000 : 120000) : (i == 1 ? 99000 : 88000);
                rates.add(Map.of("date", day.toString(), "remainingRooms", first ? (i == 1 ? 1 : 3) : (i == 1 ? 0 : 2),
                        "nightlyRate", net, "taxAmount", net / 10));
            }
            items.add(Map.of("hotelCode", code, "hotelName", first ? "Harbor House" : "Park Lodge",
                    "roomTypeCode", first ? "DLX-TWN" : "STD-DBL", "roomTypeName", first ? "Deluxe Twin" : "Standard Double",
                    "maxOccupancy", 2, "breakfastIncluded", false, "currency", "KRW", "dailyRates", rates));
        }
        return completed(ResponseEntity.ok(Map.of("items", items)));
    }
    @GetMapping("/b/api/search")
    public DeferredResult<ResponseEntity<?>> search(@RequestHeader(value="X-Api-Key", required=false) String key,
            @RequestParam String propertyIds, @RequestParam LocalDate checkIn, @RequestParam LocalDate checkOut,
            @RequestParam int adults, @RequestParam int children) {
        if (!authorized(key)) return completed(bError("E401"));
        if (!valid(propertyIds, checkIn, checkOut, adults, children)) return completed(bError("E400"));
        String mode = modes.getOrDefault("b", "normal");
        if (mode.equals("no-response")) return pending();
        if (mode.equals("error")) return completed(bError("E503"));
        if (adults + children > 2 || !Arrays.asList(propertyIds.split(",")).contains("B77120")) return completed(bSuccess(List.of()));
        var inventory = checkIn.datesUntil(checkOut).map(day -> Map.<String,Object>of("date", day.toString(), "remainingRooms", day.equals(checkIn.plusDays(1)) ? 1 : 3)).toList();
        return completed(bSuccess(List.of(Map.of("propertyId", "B77120", "propertyName", "Harbor House",
                "roomId", "R-401", "roomName", "Deluxe Twin", "maxOccupancy", 2, "breakfastIncluded", true,
                "currency", "KRW", "totalPrice", ChronoUnit.DAYS.between(checkIn, checkOut) * 150000 + 2000,
                "taxIncluded", true, "inventory", inventory))));
    }
    private boolean authorized(String key) { return "local-demo-key".equals(key); }
    private boolean valid(String codes, LocalDate start, LocalDate end, int adults, int children) {
        return !codes.isBlank() && codes.split(",", -1).length <= 50 && end.isAfter(start)
                && ChronoUnit.DAYS.between(start, end) <= 30 && adults > 0 && adults <= 20 && children >= 0 && children <= 20;
    }
    private ResponseEntity<?> aError(int status, String error) { return ResponseEntity.status(status).body(Map.of("error", error, "message", "Mock failure")); }
    private ResponseEntity<?> bError(String code) {
        Map<String,Object> body = new LinkedHashMap<>(); body.put("resultCode", code); body.put("resultMessage", "Mock failure"); body.put("data", null);
        return ResponseEntity.ok(body);
    }
    private ResponseEntity<?> bSuccess(List<?> items) { return ResponseEntity.ok(Map.of("resultCode", "0000", "resultMessage", "SUCCESS", "data", Map.of("items", items))); }
    private DeferredResult<ResponseEntity<?>> completed(ResponseEntity<?> value) { var result = pending(); result.setResult(value); return result; }
    private DeferredResult<ResponseEntity<?>> pending() { return new DeferredResult<>(600000L); }
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<?> invalid() { return aError(400, "INVALID_PARAMETER"); }
}
