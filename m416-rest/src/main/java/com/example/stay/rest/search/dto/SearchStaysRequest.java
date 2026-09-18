package com.example.stay.rest.search.dto;

import com.example.stay.search.domain.model.SearchCriteria;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

public record SearchStaysRequest(
        @Schema(description = "체크인 일자", example = "2026-09-01")
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
        @Schema(description = "체크아웃 일자", example = "2026-09-04")
        @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
        @Schema(description = "성인 인원", example = "2", minimum = "1", maximum = "20")
        @NotNull @Min(1) @Max(20) Integer adults,
        @Schema(description = "아동 인원", example = "0", minimum = "0", maximum = "20")
        @NotNull @Min(0) @Max(20) Integer children) {
    public SearchCriteria toCriteria() { return new SearchCriteria(checkIn, checkOut, adults, children); }
}
