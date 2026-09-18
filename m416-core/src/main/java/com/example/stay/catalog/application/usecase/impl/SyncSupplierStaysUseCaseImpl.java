package com.example.stay.catalog.application.usecase.impl;

import jakarta.inject.Named;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import com.example.stay.catalog.application.port.out.CatalogStateRepository;
import com.example.stay.catalog.application.port.out.RoomMappingRepository;
import com.example.stay.catalog.application.port.out.StayMappingRepository;
import com.example.stay.catalog.application.usecase.SyncSupplierStaysUseCase;
import com.example.stay.catalog.domain.model.CatalogHotel;
import com.example.stay.catalog.domain.model.CatalogState;
import com.example.stay.catalog.domain.model.RoomMapping;
import com.example.stay.catalog.domain.model.StayMapping;
import com.example.stay.search.application.port.out.SupplierClient;
import com.example.stay.search.domain.model.FailureCode;
import com.example.stay.search.domain.model.SupplierFailure;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Named
@RequiredArgsConstructor
@Transactional
public class SyncSupplierStaysUseCaseImpl implements SyncSupplierStaysUseCase {
    private final List<SupplierClient> clients;
    private final StayMappingRepository stays;
    private final RoomMappingRepository rooms;
    private final CatalogStateRepository states;

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
            saveMappings(request.client().supplier(), response.catalog());
        }
        return List.copyOf(failures);
    }

    private record CatalogRequest(SupplierClient client, CompletableFuture<CatalogResponse> response) {
    }

    private record CatalogResponse(List<CatalogHotel> catalog, Throwable error) {
    }

    private void saveMappings(String supplier, List<CatalogHotel> hotels) {
        List<Long> stayIds = stays.findAllBySupplier(supplier).stream().map(StayMapping::getId).toList();
        rooms.findAllByStayIds(stayIds).forEach(mapping -> {
            mapping.deactivate();
            rooms.save(mapping);
        });
        stays.findAllBySupplier(supplier).forEach(mapping -> {
            mapping.deactivate();
            stays.save(mapping);
        });

        Set<String> stayCodes = new HashSet<>();
        for (CatalogHotel hotel : hotels) {
            if (!stayCodes.add(hotel.stayCode())) {
                throw new IllegalArgumentException("중복 숙소 코드");
            }
            StayMapping stay = stays.findBySupplierAndStayCode(supplier, hotel.stayCode())
                    .map(existing -> {
                        existing.activate();
                        return stays.save(existing);
                    })
                    .orElseGet(() -> stays.save(StayMapping.create(supplier, hotel.stayCode())));

            Set<String> roomCodes = new HashSet<>();
            for (CatalogHotel.CatalogRoom room : hotel.rooms()) {
                if (!roomCodes.add(room.code())) {
                    throw new IllegalArgumentException("중복 객실 코드");
                }
                rooms.findByStayIdAndRoomCode(stay.getId(), room.code()).ifPresentOrElse(existing -> {
                    existing.activate();
                    rooms.save(existing);
                }, () -> rooms.save(RoomMapping.create(stay.getId(), room.code())));
            }
        }

        LocalDateTime now = LocalDateTime.now();
        CatalogState state = states.findBySupplier(supplier).orElseGet(() -> CatalogState.create(supplier, now));
        state.updateSyncedAt(now);
        states.save(state);
    }
}
