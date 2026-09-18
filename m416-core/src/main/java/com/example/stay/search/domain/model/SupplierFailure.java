package com.example.stay.search.domain.model;

public record SupplierFailure(String supplier, FailureCode code) {
    public SupplierFailure {
        if (supplier == null || supplier.isBlank() || code == null) {
            throw new IllegalArgumentException("공급사와 실패 코드는 필수입니다.");
        }
    }
}
