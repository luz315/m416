package com.example.stay.common.exception;

public final class InvalidCatalogException extends StayBaseException {
    public InvalidCatalogException(String message) {
        super(ErrorCode.INVALID_CATALOG, message);
    }
}
