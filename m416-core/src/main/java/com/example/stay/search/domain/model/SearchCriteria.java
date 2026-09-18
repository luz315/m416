package com.example.stay.search.domain.model;

import com.example.stay.common.exception.InvalidSearchCriteriaException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record SearchCriteria(LocalDate checkIn, LocalDate checkOut, int adults, int children) {
    public SearchCriteria {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn))
            throw new InvalidSearchCriteriaException("체크아웃은 체크인 이후여야 합니다.");
        if (ChronoUnit.DAYS.between(checkIn, checkOut) > 30)
            throw new InvalidSearchCriteriaException("한 번에 최대 30박까지 검색할 수 있습니다.");
        if (adults < 1 || adults > 20 || children < 0 || children > 20)
            throw new InvalidSearchCriteriaException("성인은 1~20명, 아동은 0~20명이어야 합니다.");
    }
    public int nights() { return Math.toIntExact(ChronoUnit.DAYS.between(checkIn, checkOut)); }
    public int guests() { return adults + children; }
}
