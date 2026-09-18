package com.example.stay.search.domain.model;

import com.example.stay.common.exception.InvalidMoneyException;
import java.util.Currency;

/** 통화의 최소 단위로 표현한 객실 1실의 전체 숙박 결제 금액. */
public record Money(String currency, long totalIncludingTax) {
    public Money {
        if (currency == null || currency.isBlank()) {
            throw new InvalidMoneyException("통화는 필수입니다.");
        }
        try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException exception) {
            throw new InvalidMoneyException("유효하지 않은 통화입니다.", exception);
        }
        if (totalIncludingTax < 0) throw new InvalidMoneyException("음수 요금은 허용하지 않습니다.");
    }
}
