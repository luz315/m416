package com.example.stay.rest.search;

import com.example.stay.search.application.usecase.SearchAvailableStaysUseCase;
import com.example.stay.rest.search.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.CompletionStage;

@RestController
@RequestMapping("/api/v1/stays")
@Tag(name = "Stays", description = "숙소 통합 검색")
public class StayController {
    private final SearchAvailableStaysUseCase search;
    public StayController(SearchAvailableStaysUseCase search) { this.search = search; }

    @GetMapping("/search")
    @Operation(summary = "숙소 검색", description = "날짜와 인원으로 활성 숙소를 검색하고 공급사별 결과와 실패 정보를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 결과"),
            @ApiResponse(responseCode = "400", description = "잘못된 날짜 또는 인원 조건"),
            @ApiResponse(responseCode = "500", description = "처리할 수 없는 내부 오류")
    })
    public CompletionStage<SearchStaysResponse> searchStays(@Valid @ModelAttribute SearchStaysRequest request) {
        return search.execute(request.toCriteria()).thenApply(SearchStaysResponse::from);
    }
}
