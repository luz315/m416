package com.example.stay.rest.common;

import com.example.stay.common.exception.ErrorCode;
import com.example.stay.common.exception.StayBaseException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String INVALID_REQUEST_DETAIL = "날짜와 인원 조건을 확인해 주세요. 최대 30박까지 검색 가능합니다.";

    @ExceptionHandler(StayBaseException.class)
    public ProblemDetail handleStayBaseException(HttpServletRequest request, StayBaseException exception) {
        HttpStatus status = statusOf(exception.getErrorCode());
        if (status.is5xxServerError()) {
            log.error("요청 처리 오류. code={}, path={}", exception.getErrorCode(), request.getRequestURI(), exception);
        } else {
            log.warn("잘못된 요청. code={}, path={}", exception.getErrorCode(), request.getRequestURI());
        }
        return problem(status, exception.getErrorCode(), detailOf(status, exception), request);
    }

    @ExceptionHandler({IllegalArgumentException.class, BindException.class, MethodArgumentTypeMismatchException.class})
    public ProblemDetail handleInvalidRequest(HttpServletRequest request, Exception exception) {
        log.warn("잘못된 요청. type={}, path={}", exception.getClass().getSimpleName(), request.getRequestURI());
        return problem(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST, INVALID_REQUEST_DETAIL, request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(HttpServletRequest request, Exception exception) {
        log.error("처리하지 못한 서버 오류. path={}", request.getRequestURI(), exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "요청을 처리할 수 없습니다.", request);
    }

    private ProblemDetail problem(HttpStatus status, ErrorCode code, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setType(URI.create("urn:stay:error:" + code.name().toLowerCase()));
        problem.setProperty("code", code.name());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    private HttpStatus statusOf(ErrorCode code) {
        return switch (code) {
            case INVALID_SEARCH_CRITERIA, INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
            case INVALID_CATALOG, INVALID_MAPPING, INVALID_INVENTORY, INVALID_MONEY,
                    INVALID_OFFER, INVALID_SEARCH_RESULT, INVALID_SUPPLIER_FAILURE,
                    INVALID_SUPPLIER_RESPONSE, INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private String detailOf(HttpStatus status, StayBaseException exception) {
        return status.is4xxClientError() ? exception.getMessage() : "요청을 처리할 수 없습니다.";
    }
}
