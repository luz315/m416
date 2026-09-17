package com.example.stay.catalog.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public final class CatalogState {
    private final Long id;
    private final String supplier;
    private LocalDateTime syncedAt;

    private CatalogState(
            Long id,
            String supplier,
            LocalDateTime syncedAt
    ) {
        if (supplier == null || supplier.isBlank()) {
            throw new IllegalArgumentException("공급사는 필수입니다.");
        }
        if (syncedAt == null) {
            throw new IllegalArgumentException("동기화 시각은 필수입니다.");
        }
        this.id = id;
        this.supplier = supplier;
        this.syncedAt = syncedAt;
    }

    public static CatalogState restore(
            Long id,
            String supplier,
            LocalDateTime syncedAt
    ) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("복원할 ID는 양수여야 합니다.");
        }
        return new CatalogState(
                id,
                supplier,
                syncedAt
        );
    }

    public static CatalogState create(
            String supplier,
            LocalDateTime syncedAt
    ) {
        return new CatalogState(
                null,
                supplier,
                syncedAt
        );
    }

    public void updateSyncedAt(LocalDateTime syncedAt) {
        if (syncedAt == null) {
            throw new IllegalArgumentException("동기화 시각은 필수입니다.");
        }
        this.syncedAt = syncedAt;
    }
}
