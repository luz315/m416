package com.example.stay.catalog;

import com.example.stay.catalog.application.port.out.CatalogStateRepository;
import com.example.stay.catalog.application.port.out.RoomMappingRepository;
import com.example.stay.catalog.application.port.out.StayMappingRepository;
import com.example.stay.catalog.application.usecase.impl.SyncSupplierStaysUseCaseImpl;
import com.example.stay.catalog.domain.model.CatalogHotel;
import com.example.stay.catalog.domain.model.CatalogState;
import com.example.stay.catalog.domain.model.RoomMapping;
import com.example.stay.catalog.domain.model.StayMapping;
import com.example.stay.search.application.port.out.SupplierClient;
import com.example.stay.search.domain.model.FailureCode;
import com.example.stay.search.domain.model.Offer;
import com.example.stay.search.domain.model.SearchCriteria;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SyncSupplierStaysUseCaseTest {
    @Test
    @DisplayName("공급사 목록 호출 실패만 실패 결과로 수집하고 성공한 공급사의 매핑은 저장한다")
    void collectsOnlySupplierFailures() {
        var failed = new FakeClient("A", CompletableFuture.failedFuture(new IllegalStateException("timeout")));
        var succeeded = new FakeClient("B", CompletableFuture.completedFuture(List.of(hotel("B1"))));
        var stays = new FakeStays();

        var failures = new SyncSupplierStaysUseCaseImpl(List.of(failed, succeeded), stays, new EmptyRooms(), new EmptyStates()).execute();

        assertEquals(List.of("B"), stays.savedSuppliers);
        assertEquals(1, failures.size());
        assertEquals("A", failures.getFirst().supplier());
        assertEquals(FailureCode.CATALOG_UNAVAILABLE, failures.getFirst().code());
    }

    @Test
    @DisplayName("Repository 저장 실패는 공급사 장애로 바꾸지 않고 호출자에게 전파한다")
    void propagatesRepositoryFailure() {
        var client = new FakeClient("A", CompletableFuture.completedFuture(List.of(hotel("A1"))));
        StayMappingRepository failingStays = new FakeStays() {
            @Override
            public StayMapping save(StayMapping mapping) {
                throw new IllegalStateException("database unavailable");
            }
        };

        assertThrows(IllegalStateException.class,
                () -> new SyncSupplierStaysUseCaseImpl(List.of(client), failingStays, new EmptyRooms(), new EmptyStates()).execute());
    }

    private static CatalogHotel hotel(String stayCode) {
        return new CatalogHotel(stayCode, "숙소", List.of());
    }

    private record FakeClient(String supplier, CompletionStage<List<CatalogHotel>> catalog) implements SupplierClient {
        @Override
        public CompletionStage<List<Offer>> search(List<String> codes, SearchCriteria criteria) {
            return CompletableFuture.completedFuture(List.of());
        }
    }

    private static class FakeStays implements StayMappingRepository {
        final List<String> savedSuppliers = new ArrayList<>();

        @Override
        public Optional<StayMapping> findBySupplierAndStayCode(String supplier, String stayCode) {
            return Optional.empty();
        }

        @Override
        public List<StayMapping> findAllBySupplier(String supplier) {
            return List.of();
        }

        @Override
        public List<StayMapping> findAllActiveBySupplier(String supplier) {
            return List.of();
        }

        @Override
        public StayMapping save(StayMapping mapping) {
            savedSuppliers.add(mapping.getSupplier());
            return new StayMapping(1L, mapping.getSupplier(), mapping.getStayCode(), mapping.isActive());
        }
    }

    private static class EmptyRooms implements RoomMappingRepository {
        @Override
        public Optional<RoomMapping> findByStayIdAndRoomCode(Long stayId, String roomCode) {
            return Optional.empty();
        }

        @Override
        public List<RoomMapping> findAllByStayIds(List<Long> stayIds) {
            return List.of();
        }

        @Override
        public RoomMapping save(RoomMapping mapping) {
            return mapping;
        }
    }

    private static class EmptyStates implements CatalogStateRepository {
        @Override
        public Optional<CatalogState> findBySupplier(String supplier) {
            return Optional.empty();
        }

        @Override
        public CatalogState save(CatalogState state) {
            return state;
        }

        @Override
        public boolean existsBySupplier(String supplier) {
            return false;
        }
    }
}
