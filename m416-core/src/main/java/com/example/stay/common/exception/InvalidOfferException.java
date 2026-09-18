package com.example.stay.common.exception;

public final class InvalidOfferException extends StayBaseException {
    public InvalidOfferException(String message) {
        super(ErrorCode.INVALID_OFFER, message);
    }
}
