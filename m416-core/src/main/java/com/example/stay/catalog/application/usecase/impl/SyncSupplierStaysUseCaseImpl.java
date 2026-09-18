package com.example.stay.catalog.application.usecase.impl;

import jakarta.inject.Named;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import com.example.stay.catalog.application.service.CatalogMappingWriter;
import com.example.stay.catalog.application.usecase.SyncSupplierStaysUseCase;
import com.example.stay.catalog.domain.model.CatalogHotel;
import com.example.stay.search.application.port.out.SupplierClient;
import com.example.stay.search.domain.model.FailureCode;
import com.example.stay.search.domain.model.SupplierFailure;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Named
@RequiredArgsConstructor
@Transactional(Transactional.TxType.NOT_SUPPORTED)
public class SyncSupplierStaysUseCaseImpl implements SyncSupplierStaysUseCase {
    private final List<SupplierClient> clients;
    private final CatalogMappingWriter writer;

    @Override
    public List<SupplierFailure> execute() {
        var requests = clients.stream()
                .map(client -> new CatalogRequest(client, client.catalog()
                        .handle((catalog, error) -> new CatalogResponse(catalog, error))
                        .toCompletableFuture()))
                .toList();
        CompletableFuture.allOf(requests.stream().map(CatalogRequest::response).toArray(CompletableFuture[]::new)).join();

        List<SupplierFailure> failures = new ArrayList<>();
        for (CatalogRequest request : requests) {
            CatalogResponse response = request.response().join();
            if (response.error() != null) {
                failures.add(new SupplierFailure(request.client().supplier(), FailureCode.CATALOG_UNAVAILABLE));
                continue;
            }
            writer.write(request.client().supplier(), response.catalog());
        }
        return List.copyOf(failures);
    }

    private record CatalogRequest(SupplierClient client, CompletableFuture<CatalogResponse> response) {
    }

    private record CatalogResponse(List<CatalogHotel> catalog, Throwable error) {
    }

}
