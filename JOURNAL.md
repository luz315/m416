# 개발 기록

## 2026-09-18 — 공급사 HTTP 패키지 통일

사용자 컨벤션에 맞춰 `infrastructure/supplier`를 `infrastructure/http/supplier`로 이동했다. 공급사별 클라이언트·DTO, WebClient 설정과 공통 응답 정규화를 같은 HTTP 경계 아래 배치하고 생산 코드·테스트의 import를 수정했다. Core Port, 도메인 정책과 DB Repository는 이동하지 않았다. 이번 변경은 패키지 정리이며 응답 정규화 책임을 공급사별로 분리하는 추가 리팩터링은 포함하지 않는다.

## 2026-09-18 — Jakarta 기반 UseCase 등록과 트랜잭션

사용자 요청으로 모든 UseCase 구현에 Jakarta `@Named`, Lombok `@RequiredArgsConstructor`, Jakarta `@Transactional`을 적용했다. Core에는 Spring 대신 jakarta.inject·jakarta.transaction API만 추가했다. Infra의 컴포넌트 스캔으로 UseCase를 등록하고 중복 수동 Bean 등록과 TransactionTemplate 저장 래퍼를 제거했다. 매핑 저장에는 REQUIRED, 외부 HTTP를 조정하는 동기화·검색에는 NOT_SUPPORTED를 사용한다. 비동기 HTTP 작업까지 DB 트랜잭션이 전파되는 것으로 간주하지 않는다. 클래스 기반 트랜잭션 프록시를 허용하도록 UseCase 클래스의 final을 제거했다.

### 요청과 선택 이유

- 사용자는 Core에서 Spring 애너테이션 대신 Jakarta 표준을 사용하고, 생성자 주입은 Lombok으로 통일하도록 요청했다.
- 저장 UseCase는 여러 Repository 호출을 하나의 트랜잭션으로 묶어야 한다. 외부 API 대기를 포함하는 동기화·검색까지 같은 트랜잭션으로 묶으면 불필요하게 DB 자원을 오래 유지할 수 있어 NOT_SUPPORTED로 구분했다.
- 기존 수동 Bean과 새 컴포넌트 스캔을 동시에 유지하면 중복 등록될 수 있어 UseCase의 수동 등록을 제거했다. 트랜잭션 래퍼도 애너테이션과 책임이 겹쳐 제거했다.
- Core에서 Spring 의존성은 제거된 상태를 유지하지만 Jakarta API 의존성까지 없는 순수 Java 구성은 아니다. 실행 시 실제 Bean 등록과 트랜잭션 처리는 Spring이 담당한다.

### 검증 중 확인한 문제

첫 전체 테스트는 Gradle 테스트 결과 파일 누락으로 중단되었다. 순차 재실행에서는 Core와 JPA 매핑 테스트가 통과했으나 SupplierHttpTest에서 A 목록 요청 횟수가 예상 1회 대신 2회여서 실패했다. 이 결과를 전체 테스트 통과로 기록하지 않는다. 저장 롤백 검증은 기존 TransactionTemplate 래퍼를 제거한 실제 Jakarta 애너테이션 적용 경로로 수행했다. 실제 MySQL Smoke는 이번에 실행하지 않았다.

### 기록 범위

후속 분리 검증 `./gradlew :m416-infra:test --tests '*NormalizationAndMappingTest' :m416-rest:test --no-daemon --max-workers=1`은 통과했다. Jakarta 저장 트랜잭션의 롤백과 REST 테스트는 통과했지만 위 HTTP 요청 횟수 실패가 해결되었다는 의미는 아니다. 하네스 문서 검사도 통과했다.

이 문서는 사용자의 요청, 확인한 코드, 선택·수정·거부한 대안, 검증 결과를 기록한다. 과거 절의 구현 설명은 당시 상태이며 이후 변경 절이 우선한다. 예를 들어 아래의 TransactionTemplate 사용 기록은 이 절의 Jakarta 전환 이전 상태다.

## 2026-09-18 — 카탈로그 주기 동기화

사용자 요청으로 조회·저장을 조정하는 UseCase를 `SyncSupplierStaysUseCase`, 매핑 저장을 `SaveStayMappingsUseCase`로 변경했다. REST 모듈의 `CatalogSyncScheduler`가 UseCase를 호출하고, 공급사별 Infra 클라이언트가 WebClient로 목록을 가져온다. 기존 CatalogStartup은 제거하고 스케줄러의 ApplicationRunner로 기동 동기화를 통합했다. 정적 목록의 변경 빈도와 호출 비용을 고려해 초기값은 6시간 fixedDelay로 정했으며 환경 설정으로 조정한다. 실행 잠금은 단일 프로세스 범위이며 여러 인스턴스의 중복 실행은 별도 분산 잠금 또는 단일 작업 프로세스가 필요하다. 요금·재고를 주기적으로 저장하는 작업은 추가하지 않았다.

## 2026-09-18 — 공급사별 선언형 WebClient

사용자가 제시한 선언형 HTTP 인터페이스 스타일을 Java와 WebClient에 맞춰 적용했다. Retrofit 의존성을 추가하지 않고 Spring HTTP Interface를 사용하며, 공급사별 경로·요청 파라미터와 DTO를 Infra에 격리했다. Core의 SupplierClient Port, 50개 분할·동시성 제한·타임아웃·부분 실패 흐름을 유지했다. A 목록의 기존 `hotels` 응답 키가 스펙의 `items`와 달라 DTO와 Mock을 함께 수정했다. 카탈로그 경로·응답 구조, B 본문 오류, 검색 파라미터·인증 헤더를 검증하는 HTTP 테스트를 추가했다. 원본 과제 문서와 참고 코드 전문은 복사하지 않았다.

## 2026-09-18 — Record 생성 방식 통일

사용자 요청으로 매퍼 3개의 `toRecord`를 빌더 대신 명시적인 `new Record(...)` 호출로 변경했다. 각 인자를 줄별로 작성하고 Record에 필드 배정만 수행하는 public 생성자를 두었다. `@Builder`·`@AllArgsConstructor`는 제거하고 `@Getter`와 JPA용 protected 기본 생성자는 유지했다. 도메인 변환과 영속 엔티티 변환 모두 생성자를 직접 호출하는 형태다.

## 2026-09-18 — 카탈로그 장애 경계 보완

카탈로그 목록 요청은 모든 공급사를 먼저 시작한 뒤 성공·실패 결과로 정규화해 처리하도록 변경했다. 외부 공급사 요청 실패만 `CATALOG_UNAVAILABLE` 결과로 수집하고, DB 매핑 저장 실패와 도메인 오류는 공급사 장애로 감추지 않고 스케줄러의 내부 오류로 전파한다. 공급사 한 곳의 매핑 저장은 기존 `TransactionTemplate` 경계를 유지하므로 중복 데이터나 저장 중 오류가 발생하면 해당 공급사 매핑 전체가 롤백된다.

Supplier HTTP 계층에는 연결 timeout, batch 요청 하나의 timeout, 공급사 검색 전체의 8초 상한을 두고, timeout·연결 오류·일시적 5xx 및 B의 E500/E503에만 최대 3회 jitter backoff 재시도를 적용했다. 400·401·429·본문 데이터 오류는 재시도하지 않는다. 재시도 소진 시 Reactor 내부 예외가 실패 코드를 가리지 않도록 원래 `SupplierException`을 보존했다. 스케줄러는 중복 실행 skip, 전체 소요 시간, 부분 실패와 내부 오류를 로그에 남긴다. 현재 `AtomicBoolean` 잠금은 단일 JVM 범위이며 다중 인스턴스 배포에서는 DB lock·ShedLock·리더 선출이 필요하다.

검증으로 일시적 503 재시도 성공, 인증 실패 비재시도, 공급사 호출 실패만 부분 실패로 수집, 매핑 저장 오류 전파를 추가했다. `./gradlew test bootJar --no-daemon`과 `bash scripts/verify.sh harness`가 통과했다. 실제 MySQL Smoke는 이번 변경 후 실행하지 않았다.

## 2026-09-18 — 공급사 WebClient 흐름 단순화

사용자 리뷰로 Supplier A/B 두 곳과 네 endpoint 규모에 비해 선언형 REST 인터페이스, 공통 HTTP wrapper, batch 추상화가 과하다고 판단했다. `SupplierHttpClient`, `SupplierARestClient`, `SupplierBRestClient`, `BatchedSupplierClient`를 제거했다. 대신 `SupplierWebClientConfiguration`이 공급사별 WebClient를 만들고, 각 Supplier Client가 자신이 호출하는 endpoint, 50개 분할, 최대 4개 batch 병렬 처리, 응답 검증, DTO 변환, timeout·retry를 직접 가진다.

이 과정에서 응답 변환 뒤의 `publishOn(boundedElastic)`을 제거했다. 변환은 blocking I/O나 무거운 CPU 작업이 아니므로 불필요한 scheduler 전환보다 WebClient 이벤트 흐름을 유지하는 편이 단순하다. 각 batch 응답이 요청한 숙소 코드만 포함하는지 확인할 때도 List 순회 대신 `Set` 조회를 사용했다. 공통화는 공급사가 늘어나 중복이 실제 비용이 될 때 다시 검토한다.

## 2026-09-18 — UseCase 간 호출 제거

사용자는 UseCase가 다른 UseCase를 호출하지 않고 필요한 Repository를 직접 호출해야 한다는 규칙을 정했다. 이에 `SaveStayMappingsUseCase`를 제거하고, `SyncSupplierStaysUseCase`에 숙소·객실·동기화 상태 매핑 저장을 합쳤다. Spring 의존성을 피하려는 기존 의도에 따라 `@Named`, Jakarta `@Transactional`은 유지했다. 외부 카탈로그 요청 결과를 먼저 모두 수집한 뒤 Repository 저장을 수행한다.

## 2026-09-18 — 전역 ProblemDetail 예외 처리

Core에는 HTTP 상태를 알지 않는 `ErrorCode`, `StayBaseException`과 구체 예외를 추가했다. `InvalidSearchCriteriaException`, `InvalidCatalogException`, `InvalidMappingException`, `InvalidInventoryException`, `InvalidMoneyException`, `InvalidOfferException`, `InvalidSearchResultException`, `InvalidSupplierFailureException`, `InvalidSupplierResponseException`이 각각 고정된 오류 코드를 가진다. 기존 Core·Infra의 일반 `IllegalArgumentException` 발생 지점은 해당 예외로 전환했다.

REST의 `ApiExceptionHandler`는 이 기반 예외와 기존 HTTP 바인딩 예외를 RFC 9457 ProblemDetail로 변환한다. 검색 조건 오류만 400과 검증 메시지를 반환하고, 카탈로그·매핑·공급사 응답을 비롯한 내부 데이터 오류는 500과 일반 메시지로 숨긴다. 예상 가능한 요청 오류는 warn, 내부 오류와 처리하지 못한 오류는 stack trace를 포함한 error로 로그를 구분한다.

## 2026-09-17

### 목표와 결정

- Java 21과 Spring Boot, Gradle Kotlin DSL을 선택했다. 설치된 JDK 21과 Gradle을 활용했다.
- 로컬 모듈 지침의 기술 독립적인 Core, 명확한 Port, 얇은 Controller, DTO 변환 경계를 반영했다. Kotlin 전용 Kotest/MockK 대신 JUnit과 Fake 구현을 사용했다.
- 필수 통합 흐름에 집중했다. 카탈로그와 매핑은 영속화하고, 변화가 잦은 요금·재고는 검색 시 조회한다.
- WebFlux 전면 도입 대신 MVC 비동기 응답+WebClient를 사용했다. Core에는 Reactor를 노출하지 않았다.
- 가장 적은 정보로도 의미를 유지할 수 있는 세금 포함 전체 숙박 총액을 표준으로 선택했다. 날짜별 요금·세금 내역은 모든 공급사에 공통적이지 않아 표준 계약에서 제외했다.
- 이름으로 숙소를 병합하면 조식 조건 차이를 잃을 수 있어 공급사별 상품을 유지했다.
- 카탈로그 동기화 실패는 기존 데이터를 유지하고, 정상 빈 카탈로그와 미초기화 상태를 구별했다.

### 구현과 문제 해결

- core/infrastructure/rest/mock-supplier 네 모듈, 별도 Mock 포트, 파일 H2, 타입이 있는 공급사 DTO를 작성했다.
- Gradle 캐시의 샌드박스 제한으로 최초 빌드가 실패하여 필요한 권한으로 다시 실행했다. 애플리케이션 코드 문제와 구분했다.
- 테스트 중 정상 빈 결과+다른 공급사 실패를 전체 실패로 오인하지 않도록 성공 공급사 수를 결과에 포함했다.
- 네트워크 이벤트 루프에서 JDBC 정규화 조회를 실행하지 않도록 별도 스케줄러로 이동했다.
- 준비 확인이 카탈로그 동기화보다 앞서지 않도록 readiness를 사용했다.
- 원본 참고 문서는 로컬에 보존하고 커밋 대기 목록에서 제외했다. 실제 코드와 설계 문서만 배포 대상으로 구성했다.

### AI 활용

사용자는 AI에게 내부 참고 문서에 맞는 Java 구현과 로컬 Kotlin 모듈 규칙의 Java 변환을 요청했다. AI가 요구를 읽고 모델·모듈·테스트·문서 초안을 생성하고 실행 검증을 진행했다. 명시적 모듈 지침을 발견한 뒤, 이전의 일반 샘플 기반 스타일 추정보다 이를 우선 적용했다.

AI 제안 중 채택한 것: 순수 Java Core, CompletionStage Port, 총액 기준 요금, 공급사별 실패 격리, 안정적 매핑 키.
축소·제외한 것: 불필요한 전면 리액티브 Core, 사용하지 않는 페이징/인증 구조, 이름 기반 중복 병합.

## 2026-09-18 — 제출 문서·OpenAPI 정리

공개 저장소에 맞춰 로컬 원본 Markdown을 Git ignore 대상으로 추가하고 index에서 제외했다. README에는 실행 순서, 검색 예시, 표준 금액·재고 정책, 카탈로그와 실시간 조회의 분리, 공급사 호출 제어와 추가 방식만 자체 문장으로 정리했다.

Spring MVC용 SpringDoc UI를 추가해 `/swagger-ui.html`과 `/v3/api-docs`를 제공한다. 검색 endpoint의 요약·응답 상태와 입력값 예시를 OpenAPI에 반영하고, Smoke 검증에서 문서 JSON과 UI를 함께 확인한다.

카탈로그 목록 호출은 `NOT_SUPPORTED`로 외부 대기 동안 트랜잭션을 열지 않는다. `CatalogMappingWriter`가 공급사 한 곳의 Repository 저장만 `REQUIRES_NEW`로 처리해 저장 오류의 rollback 범위를 공급사 단위로 제한한다. 이는 UseCase 간 호출이 아니라 Repository를 직접 사용하는 애플리케이션 저장 서비스다.

이 기록은 실제 대화와 구현 과정에 근거한다. 가상의 여러 날짜 작업이나 커밋 이력을 만들지 않았다. 이후 사용자 리뷰와 수정 이유는 아래에 기록한다.

### 검증

테스트 항목과 실제 실행 결과는 docs/verification.md에 기록한다. 테스트가 다루지 않는 운영 부하·장애 장기화·다중 인스턴스 경합은 미검증이다.

## 모듈 이름 정리

루트 이름을 m416으로, 네 모듈을 m416-core/m416-infra/m416-rest/m416-mock-supplier로 변경했다. Gradle 의존성과 실행 JAR 경로를 함께 수정했다. 기존 DB 식별자를 유지하기 위해 내부 UUID 생성의 namespace 문자열은 변경하지 않았다.

## 사용자 리뷰에 따른 영속성 구조 개선

### 결정과 근거

- 사용자는 Kotlin 참고 자료의 계층 분리를 Java에도 적용하도록 요청했다. 도메인은 기술 독립적인 상태와 행동을 담당하고, JPA 엔티티는 `*Record`, 변환은 별도 Mapper가 담당한다.
- 단방향 매퍼와 Repository 내부의 엔티티 생성이 섞인 구현을 지적했다. 숙소 매핑·객실 매핑·동기화 상태마다 `toDomain`과 `toRecord`를 분리했다. 조회 결과의 내부 ID 두 개는 별도 `RoomMapping`으로 반환한다.
- 사용자는 Record의 `activate`·`deactivate`·`updateSyncedAt`을 도메인으로 옮기도록 요청했다. 조회 → 도메인 복원 → 도메인 상태 변경 → Record 변환 → JPA 저장으로 수정했다.
- Record의 기본 생성자는 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`로 제한한다. 이는 JPA용 기본 생성자에 대한 접근 제한이며, 모든 생성 경로를 막거나 도메인 검증을 대신하는 장치는 아니다.
- 영속 필드는 내부 ID, 공급사 코드, 숙소·객실 코드, 활성 상태와 동기화 시각으로 한정한다. 이름·수용 인원·요금·재고를 매핑 테이블에 중복 저장하지 않는다. 검색 응답의 상품 정보는 공급사 응답을 정규화해 만든다.
- JPA 객체 연관관계 대신 `stayId` 값으로 객실과 숙소를 연결한다. 공급사·숙소 코드로 숙소 매핑을 조회하고, 내부 숙소 ID·객실 코드로 객실 매핑을 조회한다. 조인 없는 단순한 조회 경계를 선택했지만, 별도 조회 횟수와 애플리케이션 수준 정합성 관리 비용은 남는다.
- 문자열 업데이트 쿼리를 제거하고 JPA 파생 조회 메서드와 `save`를 사용했다. 파생 메서드는 컴파일 시 검증을 보장하지 않으므로 JPA 통합 테스트를 함께 실행했다.
- 초기 JDBC·파일 H2 구현에서 현재 JPA·MySQL 구조로 전환된 상태다. 현재 ID는 DB 생성 Long이며, 위의 UUID namespace 설명은 초기 구현 당시 기록이다.

### AI 제안에서 바로잡은 부분

AI는 처음에 상태 변경 메서드를 Record에 두었고, 사용자 피드백으로 도메인으로 옮겼다. 이후 Record 생성자에 대한 사용자 의도를 단정해 명시적 생성자를 Lombok으로 바꿨으나, 이는 책임 분리 문제를 해결한 변경이 아니었다. 사용자가 제공한 예시에는 매퍼가 호출하는 명시적 Record 생성자가 있었다. 영속 필드 배정용 생성자와 도메인의 생성·상태 변경 규칙을 구분해야 한다는 점을 확인했다.

### 검증과 남은 확인

도메인으로 상태 변경을 옮긴 시점에 전체 테스트 19개가 통과했다. 이후 Lombok 생성자 대체 시점에는 컴파일을 확인했다. 이번 기록 정리 시점의 파일에서는 Record의 전체 인자 생성자와 `@AllArgsConstructor`가 모두 없으므로, 매퍼의 생성 호출과 일치하는지 재확인이 필요하다. 이전 테스트 결과를 현재 파일 상태의 검증 결과로 간주하지 않는다. 실제 MySQL Smoke는 이 변경 후 재실행하지 않았다.

## 객실 매핑의 저장·조회 도메인 통일


사용자는 저장과 조회에 같은 도메인을 사용하는 흐름을 요청했다. 현재 조회에는 별도 모델을 유지할 요구가 없어 `CatalogRoomMapping`을 `RoomMapping`으로 통일하고, ID 두 개만 가진 조회용 모델을 제거했다. Repository는 Record를 Mapper로 변환한 도메인을 그대로 반환하며 상품 정규화에서 필요한 ID를 읽는다. 별도 조회 DTO는 집계·복합 조회 등 실제 요구가 생겼을 때 도입한다.

매퍼가 호출하는 생성자가 누락된 Record 3개에는 사용자 예시와 같은 명시적 필드 배정 생성자를 복구했다. JPA 기본 생성자의 protected 접근 제한과 도메인의 상태 변경 책임은 유지했다. 조회 테스트는 ID뿐 아니라 숙소 ID·객실 코드·활성 상태까지 비교하도록 변경했다.

변경 후 `./gradlew test --no-daemon`과 `bash scripts/verify.sh harness`가 통과했다. 실제 MySQL Smoke는 재실행하지 않았다.

## 도메인의 최소 불변 조건 검증


사용자가 필수 값 검증 누락을 지적하여 매핑 도메인의 공급사·숙소·객실 코드와 참조 ID, 동기화 시각을 검사하도록 했다. 신규 생성의 미발급 ID는 허용하고 복원에는 양수 ID를 요구한다. 동기화 시각 변경도 null을 거부하며 실패 시 기존 값을 유지한다. 상품은 유효한 ID·이름·공급사·가격, 양수 수용 인원, 0 이상 재고를 요구한다. 금액의 null 통화, 실패 정보와 검색 결과의 필수 값도 검사한다. 무료 상품·품절 상품을 임의로 금지하지 않도록 금액과 재고 0은 유지했다. 카탈로그 전달 타입 이동은 이번 변경에 포함하지 않았다.

## Mapper의 도메인 생성 방식 단순화

사용자 요청으로 매핑 도메인 3개의 `restore`를 제거하고 Mapper가 public 생성자를 직접 호출하도록 변경했다. 필수 값 검증은 생성자에 유지한다. 생성자를 신규 생성과 변환에 공통 사용하므로 ID는 null 또는 양수를 허용하며, 0·음수는 거부한다. 기존 `create`는 신규 상태의 기본값을 지정하는 편의 메서드로 유지했다. `@Getter`와 도메인의 상태 변경 메서드는 유지한다.
