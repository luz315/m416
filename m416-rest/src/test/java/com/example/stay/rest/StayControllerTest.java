package com.example.stay.rest;

import com.example.stay.rest.common.ApiExceptionHandler;
import com.example.stay.rest.search.StayController;
import com.example.stay.common.exception.InvalidCatalogException;
import com.example.stay.common.exception.InvalidSearchCriteriaException;
import com.example.stay.search.application.usecase.SearchAvailableStaysUseCase;
import com.example.stay.search.domain.model.*;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StayControllerTest {
    MockMvc mvc(SearchResult result) {
        SearchAvailableStaysUseCase usecase = query -> CompletableFuture.completedFuture(result);
        return MockMvcBuilders.standaloneSetup(new StayController(usecase)).setControllerAdvice(new ApiExceptionHandler()).build();
    }
    @Test @DisplayName("검색 응답은 내부 ID와 총액 및 부분 실패를 함께 반환한다")
    void responseContract() throws Exception {
        var offer=new Offer(1L,"숙소",2L,"객실",2,1,"B",true,new Money("KRW",452000));
        var mvc=mvc(new SearchResult(List.of(offer),List.of(new SupplierFailure("A",FailureCode.TIMEOUT)),1));
        var result=mvc.perform(get("/api/v1/stays/search").param("checkIn","2026-09-01").param("checkOut","2026-09-04")
                .param("adults","2").param("children","0")).andExpect(request().asyncStarted()).andReturn();
        mvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PARTIAL_FAILURE"))
                .andExpect(jsonPath("$.offers[0].stayId").value(1))
                .andExpect(jsonPath("$.offers[0].price.totalIncludingTax").value(452000))
                .andExpect(jsonPath("$.failures[0].code").value("TIMEOUT"));
    }
    @Test @DisplayName("필수 값 누락과 잘못된 날짜 순서는 400이다")
    void invalidInput() throws Exception {
        var mvc=mvc(new SearchResult(List.of(),List.of(),2));
        mvc.perform(get("/api/v1/stays/search").param("adults","2").param("children","0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.path").value("/api/v1/stays/search"));
        mvc.perform(get("/api/v1/stays/search").param("checkIn","2026-09-04").param("checkOut","2026-09-01")
                .param("adults","2").param("children","0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/stays/search").param("checkIn","bad-date").param("checkOut","2026-09-04")
                .param("adults","2").param("children","0")).andExpect(status().isBadRequest());
    }
    @Test @DisplayName("전체 공급사 실패는 정상 빈 검색과 다른 상태를 반환한다")
    void allFailed() throws Exception {
        var mvc=mvc(new SearchResult(List.of(),List.of(new SupplierFailure("A",FailureCode.TIMEOUT),new SupplierFailure("B",FailureCode.UNAVAILABLE)),0));
        var result=mvc.perform(get("/api/v1/stays/search").param("checkIn","2026-09-01").param("checkOut","2026-09-04")
                .param("adults","2").param("children","0")).andReturn();
        mvc.perform(asyncDispatch(result)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ALL_FAILED"));
    }

    @Test @DisplayName("검색 조건 예외는 ProblemDetail 형식으로 상태와 오류 코드를 반환한다")
    void searchCriteriaException() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new ErrorController()).setControllerAdvice(new ApiExceptionHandler()).build();
        mvc.perform(get("/test/error"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SEARCH_CRITERIA"))
                .andExpect(jsonPath("$.path").value("/test/error"))
                .andExpect(jsonPath("$.detail").value("잘못된 요청입니다."));
    }

    @Test
    @DisplayName("내부 카탈로그 예외는 코드만 남기고 상세 원인을 숨긴다")
    void internalException() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new ErrorController()).setControllerAdvice(new ApiExceptionHandler()).build();

        mvc.perform(get("/test/internal-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INVALID_CATALOG"))
                .andExpect(jsonPath("$.detail").value("요청을 처리할 수 없습니다."));
    }

    @RestController
    static class ErrorController {
        @GetMapping("/test/error")
        void error() {
            throw new InvalidSearchCriteriaException("잘못된 요청입니다.");
        }

        @GetMapping("/test/internal-error")
        void internalError() {
            throw new InvalidCatalogException("중복 숙소 코드");
        }
    }
}
