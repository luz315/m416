package com.example.stay.common.exception;

public final class InvalidSupplierResponseException extends StayBaseException {
    public InvalidSupplierResponseException(String message) {
        super(ErrorCode.INVALID_SUPPLIER_RESPONSE, message);
    }
}
