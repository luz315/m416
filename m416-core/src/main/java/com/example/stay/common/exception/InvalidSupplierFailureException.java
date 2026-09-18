package com.example.stay.common.exception;

public final class InvalidSupplierFailureException extends StayBaseException {
    public InvalidSupplierFailureException(String message) {
        super(ErrorCode.INVALID_SUPPLIER_FAILURE, message);
    }
}
