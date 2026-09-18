package com.example.stay.infrastructure.http.supplier.config;

import com.example.stay.infrastructure.http.supplier.a.SupplierAClient;
import com.example.stay.infrastructure.http.supplier.b.SupplierBClient;
import com.example.stay.infrastructure.http.supplier.normalization.OfferNormalizer;
import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

@Configuration(proxyBeanMethods = false)
public class SupplierWebClientConfiguration {
    @Bean("supplierAWebClient")
    WebClient supplierAWebClient(
            @Value("${suppliers.a.base-url}") String baseUrl,
            @Value("${suppliers.a.api-key}") String apiKey,
            @Value("${suppliers.connect-timeout-ms}") int connectTimeoutMs
    ) {
        return webClient(baseUrl, apiKey, connectTimeoutMs);
    }

    @Bean("supplierBWebClient")
    WebClient supplierBWebClient(
            @Value("${suppliers.b.base-url}") String baseUrl,
            @Value("${suppliers.b.api-key}") String apiKey,
            @Value("${suppliers.connect-timeout-ms}") int connectTimeoutMs
    ) {
        return webClient(baseUrl, apiKey, connectTimeoutMs);
    }

    @Bean
    SupplierAClient supplierAClient(
            @Qualifier("supplierAWebClient") WebClient webClient,
            OfferNormalizer normalizer,
            @Value("${suppliers.request-timeout-ms}") int requestTimeoutMs,
            @Value("${suppliers.retry-attempts}") int retryAttempts,
            @Value("${suppliers.retry-backoff-ms}") int retryBackoffMs
    ) {
        return new SupplierAClient(webClient, normalizer, requestTimeoutMs, retryAttempts, retryBackoffMs);
    }

    @Bean
    SupplierBClient supplierBClient(
            @Qualifier("supplierBWebClient") WebClient webClient,
            OfferNormalizer normalizer,
            @Value("${suppliers.request-timeout-ms}") int requestTimeoutMs,
            @Value("${suppliers.retry-attempts}") int retryAttempts,
            @Value("${suppliers.retry-backoff-ms}") int retryBackoffMs
    ) {
        return new SupplierBClient(webClient, normalizer, requestTimeoutMs, retryAttempts, retryBackoffMs);
    }

    private WebClient webClient(String baseUrl, String apiKey, int connectTimeoutMs) {
        var httpClient = HttpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs);
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Api-Key", apiKey)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                .build();
    }
}
