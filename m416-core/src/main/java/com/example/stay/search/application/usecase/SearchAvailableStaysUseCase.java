package com.example.stay.search.application.usecase;

import com.example.stay.search.domain.model.SearchCriteria;
import com.example.stay.search.domain.model.SearchResult;
import java.util.concurrent.CompletionStage;

public interface SearchAvailableStaysUseCase {
    CompletionStage<SearchResult> execute(SearchCriteria criteria);
}
