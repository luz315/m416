package com.example.stay.common.exception;

public final class InvalidSearchCriteriaException extends StayBaseException {
    public InvalidSearchCriteriaException(String message) {
        super(ErrorCode.INVALID_SEARCH_CRITERIA, message);
    }
}
