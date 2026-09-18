package com.example.stay.common.exception;

public final class InvalidInventoryException extends StayBaseException {
    public InvalidInventoryException(String message) {
        super(ErrorCode.INVALID_INVENTORY, message);
    }
}
