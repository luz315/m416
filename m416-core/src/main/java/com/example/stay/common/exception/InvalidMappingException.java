package com.example.stay.common.exception;

public final class InvalidMappingException extends StayBaseException {
    public InvalidMappingException(String message) {
        super(ErrorCode.INVALID_MAPPING, message);
    }
}
