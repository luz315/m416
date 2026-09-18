package com.example.stay.search.domain.model;

import com.example.stay.common.exception.InvalidSearchResultException;
import java.util.List;
public record SearchResult(List<Offer> offers, List<SupplierFailure> failures, int successfulSuppliers) {
    public SearchResult {
        if (offers == null || failures == null || successfulSuppliers < 0) {
            throw new InvalidSearchResultException("결과 목록은 필수이며 성공 공급사 수는 0 이상이어야 합니다.");
        }
        if (offers.stream().anyMatch(java.util.Objects::isNull)
                || failures.stream().anyMatch(java.util.Objects::isNull)) {
            throw new InvalidSearchResultException("결과 목록에 null을 포함할 수 없습니다.");
        }
        offers = List.copyOf(offers);
        failures = List.copyOf(failures);
    }
    public boolean partialFailure() { return !failures.isEmpty(); }
    public boolean allSuppliersFailed() { return successfulSuppliers == 0 && !failures.isEmpty(); }
}
