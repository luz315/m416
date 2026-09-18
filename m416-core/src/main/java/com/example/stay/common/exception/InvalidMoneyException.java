package com.example.stay.common.exception;

public final class InvalidMoneyException extends StayBaseException {
    public InvalidMoneyException(String message) {
        super(ErrorCode.INVALID_MONEY, message);
    }

    public InvalidMoneyException(String message, Throwable cause) {
        super(ErrorCode.INVALID_MONEY, message, cause);
    }
}
