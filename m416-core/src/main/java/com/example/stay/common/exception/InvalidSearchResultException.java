package com.example.stay.common.exception;

public final class InvalidSearchResultException extends StayBaseException {
    public InvalidSearchResultException(String message) {
        super(ErrorCode.INVALID_SEARCH_RESULT, message);
    }
}
