# Stay Gateway

외부 숙소 공급사의 카탈로그를 내부 식별자로 관리하고, 고객의 날짜·인원 조건에 맞는 실시간 요금과 재고를 통합 조회하는 Java 21/Spring Boot 애플리케이션입니다.

## 실행

JDK 21, MySQL, Gradle Wrapper가 필요합니다. 기본 DB 접속 정보는 `root` / `0000`, DB 이름은 `m416`입니다.

```bash
mysql -u root -p0000 -e 'CREATE DATABASE IF NOT EXISTS m416 CHARACTER SET utf8mb4'
./gradlew :m416-mock-supplier:bootRun
./gradlew :m416-rest:bootRun
```

Mock은 `http://127.0.0.1:9090`, 검색 API는 `http://127.0.0.1:8080`에서 실행됩니다.

```bash
curl 'http://127.0.0.1:8080/api/v1/stays/search?checkIn=2026-09-01&checkOut=2026-09-04&adults=2&children=0'
```

API 문서는 [Swagger UI](http://127.0.0.1:8080/swagger-ui.html), OpenAPI JSON은 `http://127.0.0.1:8080/v3/api-docs`에서 확인할 수 있습니다.

전체 검사:

```bash
./gradlew test bootJar --no-daemon
bash scripts/verify.sh harness
```

## 설계 결정

- 카탈로그는 변동이 적으므로 기동 시와 6시간 간격으로 받아 숙소·객실 타입 코드와 내부 ID 매핑만 저장합니다. 요금·재고·이름·수용 인원은 저장하지 않고 검색 시 공급사에 다시 요청합니다.
- 공급사가 다르면 동일한 이름의 숙소여도 별도 상품으로 유지합니다. 공통 키가 없는 이름 기반 병합은 오판 위험이 큽니다.
- A의 날짜별 순액과 세금을 합산하고, B의 세금 포함 전체 금액은 그대로 사용합니다. 모든 금액은 통화 최소 단위의 정수입니다.
- 예약 가능 객실 수는 체크인 포함·체크아웃 제외 기간의 일별 재고 최솟값입니다. 재고가 `0`인 상품도 결과에 포함해 매진 상태를 보여 줍니다.
- 활성 숙소 코드를 공급사별로 최대 50개씩 나누고, batch는 최대 4개까지 병렬 호출합니다. 연결 timeout은 300ms, 요청 timeout은 1.5초, 한 공급사 검색 전체 상한은 8초입니다. timeout·연결 오류·일시적 공급사 오류만 jitter backoff로 최대 3회 재시도합니다.
- 한 공급사 실패는 `failures`에 기록하고 다른 공급사의 상품은 반환합니다. B의 본문 결과 코드도 A의 HTTP 오류와 같은 `FailureCode`로 정규화합니다.

## 구성

`m416-core`는 도메인 모델·UseCase·Port를, `m416-infra`는 JPA 매핑과 WebClient 어댑터를, `m416-rest`는 HTTP API·예외 처리·카탈로그 스케줄을, `m416-mock-supplier`는 로컬 공급사 Mock을 담당합니다.

새 공급사는 Infra에 전용 WebClient·응답 DTO·`SupplierClient` 구현을 추가하고, 설정에 base URL/API 키를 등록하면 됩니다. Core 검색 흐름은 공급사 구현 세부사항을 알지 않습니다.
