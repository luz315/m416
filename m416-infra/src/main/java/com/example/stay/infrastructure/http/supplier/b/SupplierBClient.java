package com.example.stay.infrastructure.http.supplier.b;

import com.example.stay.catalog.domain.model.CatalogHotel;
import com.example.stay.common.exception.InvalidSupplierResponseException;
import com.example.stay.infrastructure.http.supplier.b.dto.SupplierBResponse;
import com.example.stay.infrastructure.http.supplier.normalization.OfferNormalizer;
import com.example.stay.search.application.port.out.SupplierClient;
import com.example.stay.search.domain.model.FailureCode;
import com.example.stay.search.domain.model.Offer;
import com.example.stay.search.domain.model.SearchCriteria;
import com.example.stay.search.domain.model.SupplierException;
import io.netty.channel.ConnectTimeoutException;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

public final class SupplierBClient implements SupplierClient {
    private final WebClient webClient;
    private final OfferNormalizer normalizer;
    private final Duration requestTimeout;
    private final int retryAttempts;
    private final Duration retryBackoff;

    public SupplierBClient(
            WebClient webClient,
            OfferNormalizer normalizer,
            int requestTimeoutMs,
            int retryAttempts,
            int retryBackoffMs
    ) {
        this.webClient = webClient;
        this.normalizer = normalizer;
        this.requestTimeout = Duration.ofMillis(requestTimeoutMs);
        this.retryAttempts = retryAttempts;
        this.retryBackoff = Duration.ofMillis(retryBackoffMs);
    }

    @Override
    public String supplier() {
        return "B";
    }

    @Override
    public CompletionStage<List<CatalogHotel>> catalog() {
        return request(webClient.get()
                .uri("/b/api/properties")
                .retrieve()
                .bodyToMono(SupplierBResponse.Properties.class)
                .map(response -> {
                    checkResult(response.resultCode());
                    return response.data().items().stream()
                            .map(property -> new CatalogHotel(
                                    property.propertyId(),
                                    property.propertyName(),
                                    property.rooms().stream()
                                            .map(room -> new CatalogHotel.CatalogRoom(room.roomId(), room.roomName(), room.maxOccupancy()))
                                            .toList()
                            ))
                            .toList();
                }))
                .toFuture();
    }

    @Override
    public CompletionStage<List<Offer>> search(List<String> codes, SearchCriteria criteria) {
        if (codes.isEmpty()) {
            return java.util.concurrent.CompletableFuture.completedFuture(List.of());
        }
        return Flux.fromIterable(codes)
                .buffer(50)
                .flatMap(batch -> searchBatch(batch, criteria), 4)
                .collectList()
                .timeout(Duration.ofSeconds(8))
                .onErrorMap(SupplierBClient::translate)
                .toFuture();
    }

    private Flux<Offer> searchBatch(List<String> codes, SearchCriteria criteria) {
        Set<String> requestedCodes = Set.copyOf(codes);
        return request(webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/b/api/search")
                        .queryParam("propertyIds", String.join(",", codes))
                        .queryParam("checkIn", criteria.checkIn())
                        .queryParam("checkOut", criteria.checkOut())
                        .queryParam("adults", criteria.adults())
                        .queryParam("children", criteria.children())
                        .build())
                .retrieve()
                .bodyToMono(SupplierBResponse.Search.class)
                .map(response -> {
                    checkResult(response.resultCode());
                    Set<List<String>> seen = new HashSet<>();
                    return response.data().items().stream().map(item -> {
                        if (!requestedCodes.contains(item.propertyId())) {
                            throw new InvalidSupplierResponseException("요청하지 않은 숙소 응답");
                        }
                        if (!seen.add(List.of(item.propertyId(), item.roomId()))) {
                            throw new InvalidSupplierResponseException("중복 상품");
                        }
                        return normalizer.fromB(item, criteria);
                    }).toList();
                }))
                .flatMapMany(Flux::fromIterable);
    }

    private <T> Mono<T> request(Mono<T> response) {
        return response
                .switchIfEmpty(Mono.error(new SupplierException(FailureCode.INVALID_DATA, "빈 공급사 응답")))
                .timeout(requestTimeout)
                .onErrorMap(SupplierBClient::translate)
                .retryWhen(Retry.backoff(retryAttempts - 1L, retryBackoff)
                        .jitter(0.5)
                        .filter(SupplierBClient::isRetryable)
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
    }

    private static void checkResult(String code) {
        if ("0000".equals(code)) {
            return;
        }
        FailureCode failure = switch (String.valueOf(code)) {
            case "E400" -> FailureCode.INVALID_REQUEST;
            case "E401" -> FailureCode.AUTHENTICATION;
            case "E429" -> FailureCode.RATE_LIMIT;
            case "E500", "E503" -> FailureCode.UNAVAILABLE;
            default -> FailureCode.INVALID_DATA;
        };
        throw new SupplierException(failure, "공급사 본문 실패");
    }

    private static Throwable translate(Throwable error) {
        if (error instanceof SupplierException) {
            return error;
        }
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof TimeoutException || cause instanceof ConnectTimeoutException
                    || cause instanceof io.netty.handler.timeout.TimeoutException) {
                return new SupplierException(FailureCode.TIMEOUT, error);
            }
        }
        if (error instanceof WebClientResponseException response) {
            return new SupplierException(httpFailure(response.getStatusCode().value()), error);
        }
        if (error instanceof WebClientRequestException) {
            return new SupplierException(FailureCode.UNAVAILABLE, error);
        }
        return new SupplierException(FailureCode.INVALID_DATA, error);
    }

    private static FailureCode httpFailure(int status) {
        return switch (status) {
            case 400 -> FailureCode.INVALID_REQUEST;
            case 401, 403 -> FailureCode.AUTHENTICATION;
            case 429 -> FailureCode.RATE_LIMIT;
            default -> FailureCode.UNAVAILABLE;
        };
    }

    private static boolean isRetryable(Throwable error) {
        return error instanceof SupplierException supplierError
                && (supplierError.code() == FailureCode.TIMEOUT || supplierError.code() == FailureCode.UNAVAILABLE);
    }
}
