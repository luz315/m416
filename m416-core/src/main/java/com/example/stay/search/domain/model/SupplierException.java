package com.example.stay.search.domain.model;

public final class SupplierException extends RuntimeException {
    private final FailureCode code;
    public SupplierException(FailureCode code, String message) { super(message); this.code = code; }
    public SupplierException(FailureCode code, Throwable cause) { super(code.name(), cause); this.code = code; }
    public FailureCode code() { return code; }
}
