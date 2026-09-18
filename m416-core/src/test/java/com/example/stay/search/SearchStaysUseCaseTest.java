package com.example.stay.search;

import com.example.stay.catalog.application.port.out.CatalogStateRepository;
import com.example.stay.catalog.application.port.out.StayMappingRepository;
import com.example.stay.catalog.domain.model.*;
import com.example.stay.search.application.port.out.SupplierClient;
import com.example.stay.search.application.usecase.impl.SearchAvailableStaysUseCaseImpl;
import com.example.stay.search.domain.model.*;
import org.junit.jupiter.api.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class SearchStaysUseCaseTest {
    @Test @DisplayName("A 완료를 기다리지 않고 B도 시작하며 A 실패 시 B 결과를 보존한다")
    void parallelPartialFailure() {
        var a = new FakeClient("A"); var b = new FakeClient("B");
        var repository = new FakeRepository();
        var usecase = new SearchAvailableStaysUseCaseImpl(List.of(a,b), repository, repository);
        var result = usecase.execute(new SearchCriteria(LocalDate.of(2026,9,1), LocalDate.of(2026,9,2),2,0)).toCompletableFuture();
        assertTrue(a.called && b.called);
        assertFalse(result.isDone());
        a.response.completeExceptionally(new SupplierException(FailureCode.TIMEOUT, "timeout"));
        b.response.complete(List.of(new Offer(1L, "숙소", 2L, "객실", 2, 1, "B", true, new Money("KRW", 452000))));
        assertEquals(1,result.join().offers().size());
        assertEquals(FailureCode.TIMEOUT,result.join().failures().getFirst().code());
        assertFalse(result.join().allSuppliersFailed());
    }
    @Test @DisplayName("한 공급사의 정상 빈 결과와 다른 공급사 실패는 전체 실패와 구분한다")
    void successfulEmptyResult() {
        var a = new FakeClient("A"); var b = new FakeClient("B");
        a.response.complete(List.of()); b.response.completeExceptionally(new SupplierException(FailureCode.UNAVAILABLE,"failed"));
        var repository = new FakeRepository();
        var result = new SearchAvailableStaysUseCaseImpl(List.of(a,b),repository, repository)
                .execute(new SearchCriteria(LocalDate.of(2026,9,1), LocalDate.of(2026,9,2),2,0)).toCompletableFuture().join();
        assertTrue(result.offers().isEmpty()); assertFalse(result.allSuppliersFailed()); assertTrue(result.partialFailure());
    }
    private static class FakeClient implements SupplierClient {
        final String name; boolean called;
        final CompletableFuture<List<Offer>> response = new CompletableFuture<>();
        FakeClient(String name) { this.name=name; }
        public String supplier() { return name; }
        public CompletionStage<List<CatalogHotel>> catalog() { return CompletableFuture.completedFuture(List.of()); }
        public CompletionStage<List<Offer>> search(List<String> codes, SearchCriteria query) { called=true; return response; }
    }
    private static class FakeRepository implements StayMappingRepository, CatalogStateRepository {
        public Optional<StayMapping> findBySupplierAndStayCode(String supplier, String stayCode) { return Optional.empty(); }
        public List<StayMapping> findAllBySupplier(String supplier) { return List.of(); }
        public List<StayMapping> findAllActiveBySupplier(String supplier) { return List.of(StayMapping.create(supplier, "hotel")); }
        public StayMapping save(StayMapping mapping) { return mapping; }
        public Optional<CatalogState> findBySupplier(String supplier) { return Optional.empty(); }
        public CatalogState save(CatalogState state) { return state; }
        public boolean existsBySupplier(String supplier) { return true; }
    }
}
