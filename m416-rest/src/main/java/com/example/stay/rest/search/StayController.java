package com.example.stay.rest.search;

import com.example.stay.search.application.usecase.SearchAvailableStaysUseCase;
import com.example.stay.rest.search.dto.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.CompletionStage;

@RestController
@RequestMapping("/api/v1/stays")
public class StayController {
    private final SearchAvailableStaysUseCase search;
    public StayController(SearchAvailableStaysUseCase search) { this.search = search; }
    @GetMapping("/search")
    public CompletionStage<SearchStaysResponse> searchStays(@Valid @ModelAttribute SearchStaysRequest request) {
        return search.execute(request.toCriteria()).thenApply(SearchStaysResponse::from);
    }
}
