package com.example.stay.catalog.domain.model;

import lombok.Getter;

@Getter
public final class StayMapping {
    private final Long id;
    private final String supplier;
    private final String stayCode;
    private boolean active;

    private StayMapping(
            Long id,
            String supplier,
            String stayCode,
            boolean active
    ) {
        if (supplier == null || supplier.isBlank()) {
            throw new IllegalArgumentException("공급사는 필수입니다.");
        }
        if (stayCode == null || stayCode.isBlank()) {
            throw new IllegalArgumentException("숙소 코드는 필수입니다.");
        }
        this.id = id;
        this.supplier = supplier;
        this.stayCode = stayCode;
        this.active = active;
    }

    public static StayMapping restore(
            Long id,
            String supplier,
            String stayCode,
            boolean active
    ) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("복원할 ID는 양수여야 합니다.");
        }
        return new StayMapping(
                id,
                supplier,
                stayCode,
                active
        );
    }

    public static StayMapping create(
            String supplier,
            String stayCode
    ) {
        return new StayMapping(
                null,
                supplier,
                stayCode,
                true
        );
    }

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
    }
}
