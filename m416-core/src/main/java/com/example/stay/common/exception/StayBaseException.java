package com.example.stay.common.exception;

import java.util.Objects;

public class StayBaseException extends RuntimeException {
    private final ErrorCode errorCode;

    public StayBaseException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "오류 코드는 필수입니다.");
    }

    protected StayBaseException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = Objects.requireNonNull(errorCode, "오류 코드는 필수입니다.");
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
