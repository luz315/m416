package com.example.stay.rest.search.dto;

import com.example.stay.search.domain.model.SearchCriteria;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

public record SearchStaysRequest(
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
        @NotNull @Min(1) @Max(20) Integer adults,
        @NotNull @Min(0) @Max(20) Integer children) {
    public SearchCriteria toCriteria() { return new SearchCriteria(checkIn, checkOut, adults, children); }
}
