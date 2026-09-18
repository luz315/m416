package com.example.stay.search.application.usecase.impl;

import jakarta.inject.Named;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import com.example.stay.catalog.application.port.out.CatalogStateRepository;
import com.example.stay.catalog.application.port.out.StayMappingRepository;
import com.example.stay.search.application.port.out.SupplierClient;
import com.example.stay.search.application.usecase.SearchAvailableStaysUseCase;
import com.example.stay.search.domain.model.FailureCode;
import com.example.stay.search.domain.model.Offer;
import com.example.stay.search.domain.model.SearchCriteria;
import com.example.stay.search.domain.model.SearchResult;
import com.example.stay.search.domain.model.SupplierException;
import com.example.stay.search.domain.model.SupplierFailure;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

@Named
@RequiredArgsConstructor
@Transactional(Transactional.TxType.NOT_SUPPORTED)
public class SearchAvailableStaysUseCaseImpl implements SearchAvailableStaysUseCase {
    private final List<SupplierClient> clients;
    private final StayMappingRepository stays;
    private final CatalogStateRepository states;

    @Override
    public CompletionStage<SearchResult> execute(SearchCriteria criteria) {
        var requests = clients.stream().map(client -> request(client, criteria)).toList();
        return CompletableFuture.allOf(requests.toArray(CompletableFuture[]::new)).thenApply(ignored -> {
            List<Offer> offers = new ArrayList<>();
            List<SupplierFailure> failures = new ArrayList<>();
            for (var request : requests) {
                var result = request.join();
                offers.addAll(result.offers());
                failures.addAll(result.failures());
            }
            offers.sort(Comparator.comparing(Offer::supplier).thenComparing(Offer::stayId).thenComparing(Offer::roomTypeId));
            return new SearchResult(offers, failures, requests.stream().mapToInt(request -> request.join().successfulSuppliers()).sum());
        });
    }

    private CompletableFuture<SearchResult> request(SupplierClient client, SearchCriteria criteria) {
        try {
            if (!states.existsBySupplier(client.supplier())) {
                return CompletableFuture.completedFuture(failed(client.supplier(), FailureCode.CATALOG_UNAVAILABLE));
            }
            var codes = stays.findAllActiveBySupplier(client.supplier()).stream().map(mapping -> mapping.getStayCode()).toList();
            return client.search(codes, criteria).handle((offers, error) -> error == null
                    ? new SearchResult(offers.stream().filter(offer -> offer.maxOccupancy() >= criteria.guests()).toList(), List.of(), 1)
                    : failed(client.supplier(), failureCode(error))).toCompletableFuture();
        } catch (RuntimeException error) {
            return CompletableFuture.completedFuture(failed(client.supplier(), failureCode(error)));
        }
    }

    private static SearchResult failed(String supplier, FailureCode code) {
        return new SearchResult(List.of(), List.of(new SupplierFailure(supplier, code)), 0);
    }

    private static FailureCode failureCode(Throwable error) {
        while ((error instanceof CompletionException || error instanceof ExecutionException) && error.getCause() != null) {
            error = error.getCause();
        }
        return error instanceof SupplierException exception ? exception.code() : FailureCode.UNAVAILABLE;
    }
}
