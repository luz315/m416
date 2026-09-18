package com.example.stay.infrastructure;

import com.example.stay.catalog.application.port.out.RoomMappingRepository;
import com.example.stay.catalog.application.port.out.StayMappingRepository;
import com.example.stay.catalog.domain.model.*;
import com.example.stay.infrastructure.http.supplier.a.SupplierAClient;
import com.example.stay.infrastructure.http.supplier.b.SupplierBClient;
import com.example.stay.infrastructure.http.supplier.normalization.OfferNormalizer;
import com.example.stay.search.domain.model.*;
import com.sun.net.httpserver.*;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

class SupplierHttpTest {
    HttpServer server;
    ExecutorService executor;
    final AtomicReference<String> body=new AtomicReference<>("{\"items\":[]}");
    final AtomicInteger status=new AtomicInteger(200);
    final AtomicInteger unavailableResponses=new AtomicInteger();
    final AtomicBoolean hang=new AtomicBoolean();
    final List<String> queries=new CopyOnWriteArrayList<>();
    final List<String> keys=new CopyOnWriteArrayList<>();
    final List<String> paths=new CopyOnWriteArrayList<>();
    WebClient webClient;
    @BeforeEach void setup() throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        executor=Executors.newVirtualThreadPerTaskExecutor(); server.setExecutor(executor);
        server.createContext("/", exchange -> {
            queries.add(String.valueOf(exchange.getRequestURI().getQuery()));
            paths.add(exchange.getRequestURI().getPath());
            keys.add(String.valueOf(exchange.getRequestHeaders().getFirst("X-Api-Key")));
            if (hang.get()) return;
            int responseStatus = unavailableResponses.getAndUpdate(value -> Math.max(0, value - 1)) > 0 ? 503 : status.get();
            byte[] bytes=body.get().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/json");
            exchange.sendResponseHeaders(responseStatus,bytes.length);
            try(var out=exchange.getResponseBody()) { out.write(bytes); }
        });
        server.start();
        webClient = WebClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .defaultHeader("X-Api-Key", "demo-key")
                .build();
    }
    @AfterEach void cleanup() { server.stop(0); executor.shutdownNow(); }
    SearchCriteria query() { return new SearchCriteria(LocalDate.of(2026,9,1),LocalDate.of(2026,9,4),2,0); }
    @Test @DisplayName("HTTP 200 안의 E503을 공급사 장애로 판정한다")
    void bodyFailure() {
        body.set("{\"resultCode\":\"E503\",\"data\":null}");
        var client=supplierBClient();
        var error=assertThrows(CompletionException.class,()->client.search(List.of("H"),query()).toCompletableFuture().join());
        assertEquals(FailureCode.UNAVAILABLE,((SupplierException)error.getCause()).code());
    }
    @Test @DisplayName("HTTP 인증 오류는 재시도하지 않는다")
    void authenticationFailure() {
        status.set(401);
        var client=supplierAClient();
        var error=assertThrows(CompletionException.class,()->client.search(List.of("H"),query()).toCompletableFuture().join());
        assertEquals(FailureCode.AUTHENTICATION,((SupplierException)error.getCause()).code());
        assertEquals(1,queries.size());
    }
    @Test @DisplayName("일시적 503은 backoff 후 재시도해 정상 응답을 사용한다")
    void retryTransientFailure() {
        unavailableResponses.set(1);
        body.set("{\"items\":[]}");
        assertTrue(supplierAClient(3).catalog().toCompletableFuture().join().isEmpty());
        assertEquals(2, queries.size());
    }
    @Test @DisplayName("연결 후 무응답은 제한 시간 내 TIMEOUT으로 종료한다")
    void noResponse() {
        hang.set(true);
        var client=supplierAClient();
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(4),()-> {
            var error=assertThrows(CompletionException.class,()->client.search(List.of("H"),query()).toCompletableFuture().join());
            assertEquals(FailureCode.TIMEOUT,((SupplierException)error.getCause()).code());
        });
    }
    @Test @DisplayName("101개 숙소는 50개 이하의 세 요청으로 분할하고 API 키를 전달한다")
    void batchLimit() {
        var client=supplierAClient();
        var codes=IntStream.range(0,101).mapToObj(i->"H"+i).toList();
        assertTrue(client.search(codes,query()).toCompletableFuture().join().isEmpty());
        assertEquals(3,queries.size());
        assertTrue(keys.stream().allMatch("demo-key"::equals));
        Set<String> requested=new HashSet<>();
        for(String query:queries) {
            var segment=Arrays.stream(query.split("&")).filter(s->s.startsWith("hotelCodes=")).findFirst().orElseThrow().substring(11);
            var batch=segment.split(","); assertTrue(batch.length<=50); requested.addAll(List.of(batch));
        }
        assertEquals(new HashSet<>(codes),requested);
    }
    @Test @DisplayName("A 목록은 items 응답을 읽고 목록 API에는 조건을 보내지 않는다")
    void catalogAContract() {
        body.set("{\"items\":[{\"hotelCode\":\"H1\",\"hotelName\":\"숙소\",\"roomTypes\":[{\"roomTypeCode\":\"R1\",\"roomTypeName\":\"객실\",\"maxOccupancy\":2}]}]}");
        var result = supplierAClient().catalog().toCompletableFuture().join();
        assertEquals("H1", result.getFirst().stayCode());
        assertEquals(List.of("/a/v1/hotels"), paths);
        assertEquals(List.of("null"), queries);
        assertEquals(List.of("demo-key"), keys);
    }

    @Test @DisplayName("B 목록은 data.items를 읽고 본문 실패를 거부한다")
    void catalogBContract() {
        body.set("{\"resultCode\":\"0000\",\"data\":{\"items\":[]}}");
        var client = supplierBClient();
        assertTrue(client.catalog().toCompletableFuture().join().isEmpty());
        assertEquals(List.of("/b/api/properties"), paths);
        assertEquals(List.of("null"), queries);
        body.set("{\"resultCode\":\"E503\",\"data\":null}");
        var error = assertThrows(CompletionException.class, () -> client.catalog().toCompletableFuture().join());
        assertEquals(FailureCode.UNAVAILABLE, ((SupplierException) error.getCause()).code());
    }

    @Test @DisplayName("B 검색은 propertyIds와 ISO 날짜 및 성인·아동을 전달한다")
    void searchBContract() {
        body.set("{\"resultCode\":\"0000\",\"data\":{\"items\":[]}}");
        assertTrue(supplierBClient().search(List.of("B1", "B2"), query()).toCompletableFuture().join().isEmpty());
        assertEquals(List.of("/b/api/search"), paths);
        var params = Set.of(queries.getFirst().split("&"));
        assertEquals(Set.of("propertyIds=B1,B2", "checkIn=2026-09-01", "checkOut=2026-09-04", "adults=2", "children=0"), params);
        assertEquals(List.of("demo-key"), keys);
    }

    private static OfferNormalizer normalizer() {
        return new OfferNormalizer(new EmptyStays(), new EmptyRooms());
    }

    private SupplierAClient supplierAClient() {
        return supplierAClient(1);
    }

    private SupplierAClient supplierAClient(int retryAttempts) {
        return new SupplierAClient(webClient, normalizer(), 500, retryAttempts, 10);
    }

    private SupplierBClient supplierBClient() {
        return new SupplierBClient(webClient, normalizer(), 500, 1, 10);
    }

    private static class EmptyStays implements StayMappingRepository {
        public Optional<StayMapping> findBySupplierAndStayCode(String supplier, String stayCode) { return Optional.empty(); }
        public List<StayMapping> findAllBySupplier(String supplier) { return List.of(); }
        public List<StayMapping> findAllActiveBySupplier(String supplier) { return List.of(); }
        public StayMapping save(StayMapping mapping) { return mapping; }
    }

    private static class EmptyRooms implements RoomMappingRepository {
        public Optional<RoomMapping> findByStayIdAndRoomCode(Long stayId, String roomCode) { return Optional.empty(); }
        public List<RoomMapping> findAllByStayIds(List<Long> stayIds) { return List.of(); }
        public RoomMapping save(RoomMapping mapping) { return mapping; }
    }
}
