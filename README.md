# m416

서로 다른 숙박 공급사의 카탈로그와 실시간 요금·재고를 하나의 검색 API로 제공하는 Java 백엔드입니다. 외부 상용 서비스 대신 별도 포트의 로컬 Mock을 사용합니다.

## 실행

빌드 필요 환경: **JDK 21 설치** (애플리케이션 실행은 21 이상), 인터넷 연결(최초 Gradle 의존성 다운로드). 소스와 테스트는 Java 21을 기준으로 컴파일합니다. Gradle Wrapper가 포함되어 있어 Gradle 별도 설치는 필요하지 않습니다.

```sh
./gradlew test bootJar
```

저장소 루트에서 터미널 두 개로 순서대로 실행합니다.

```sh
# 터미널 1: Mock 공급사
java -jar m416-mock-supplier/build/libs/m416-mock-supplier-0.1.0.jar
```

```sh
# 터미널 2: 검색 서버 — Mock 준비 후 실행
java -jar m416-rest/build/libs/m416-rest-0.1.0.jar
```

기본 주소는 검색 서버 `127.0.0.1:8080`, Mock `127.0.0.1:9090`입니다. 검색 서버는 기동 시 카탈로그를 수집하며 `GET /actuator/health/readiness`가 준비 완료를 나타냅니다. 기본 MySQL 접속 정보는 `127.0.0.1:3306/m416`, 사용자 `root`, 비밀번호 `0000`입니다.

`m416` 데이터베이스는 미리 생성되어 있어야 하며, `root` 계정에는 테이블 생성·조회·수정 권한이 필요합니다. 애플리케이션은 매핑 테이블만 기동 시 생성합니다.

```sh
curl 'http://127.0.0.1:8080/api/v1/stays/search?checkIn=2026-09-01&checkOut=2026-09-04&adults=2&children=0'
```

기본 데이터는 상품 3개를 반환합니다. A의 Harbor House 총액은 429,000 KRW, B는 조식 포함 452,000 KRW입니다. A의 Park Lodge는 중간 숙박일 재고가 없어 예약 가능 객실 수가 0입니다. 서로 다른 공급사 상품은 별개로 유지합니다.

## 장애 재현

```sh
# A는 연결되지만 응답하지 않음 → B 결과와 A TIMEOUT 반환
curl -X POST 'http://127.0.0.1:9090/control/a/mode?value=no-response'

# B는 HTTP 200 안에 실패 코드 반환 → 두 공급사 실패 상태
curl -X POST 'http://127.0.0.1:9090/control/b/mode?value=error'

# 복구
curl -X POST 'http://127.0.0.1:9090/control/a/mode?value=normal'
curl -X POST 'http://127.0.0.1:9090/control/b/mode?value=normal'
```

A의 `error`는 HTTP 503을 반환합니다. Mock 제어는 로컬 개발용이며 외부에 공개하지 않습니다. Mock API 키는 예시 값 `local-demo-key`입니다. 연동 어댑터는 localhost/127.0.0.1 HTTP 주소만 허용합니다.

## 모듈과 Java 컨벤션

```text
m416-core/             순수 Java 도메인 규칙, UseCase, 외부 Port
m416-infra/            WebClient, 공급사 DTO·정규화, JPA 매핑 저장소, Bean 조립
m416-rest/             Spring MVC Controller, 요청·응답 DTO, 실행 진입점
m416-mock-supplier/    별도 프로세스의 공급사 모의 서버
```

`m416-rest → m416-core`, `m416-rest → m416-infra → m416-core` 방향입니다. Core는 Spring·Reactor·HTTP·DB 구현을 참조하지 않으며 `CompletionStage`로 비동기 Port를 표현합니다. UseCase끼리 호출하지 않습니다. Kotlin의 값 객체와 DTO는 Java record, 생성자 주입과 확장 변환 함수는 명시적 생성자·정적 팩토리로 옮겼습니다. DB 스키마·SQL과 API 응답 DTO는 Core에서 분리합니다. 사용하지 않는 페이지·정렬·인증 모듈은 만들지 않았습니다.

## 주요 선택

- **요금:** 객실 1실, 전체 숙박 기간, 세금 포함 총액, 통화 최소 단위 `long`. A의 일별 순액+세금을 합산하고 B의 총액은 그대로 보존합니다. B에 없는 세금액·일별 요금을 역산하지 않습니다. 금액 오버플로는 정규화 실패입니다.
- **재고:** 체크인부터 체크아웃 전날까지 날짜가 모두 존재해야 하며 최솟값을 사용합니다. 0인 상품도 보여주어 매진과 조회 실패를 구별합니다. 누락·중복·음수 재고는 공급사 데이터 오류로 처리합니다.
- **상품 조건:** 조식 포함 여부와 통화를 유지합니다. 이름만 같은 숙소를 병합하지 않고 서로 다른 통화의 금액을 비교하거나 환산하지 않습니다.
- **매핑:** 공급사+숙소 코드와 내부 숙소 ID, 내부 숙소 ID+객실 코드와 내부 객실 ID의 유일성을 DB에 저장합니다. 내부 ID는 DB가 생성하며, 검색은 반드시 저장된 매핑을 사용합니다. 숙소·객실 이름은 공급사 검색 응답을 사용합니다.
- **카탈로그:** 기동 시 동기화합니다. 성공한 공급사의 스냅샷만 트랜잭션으로 교체하고 제거된 항목은 비활성화하여 ID를 유지합니다. 실패 시 기존 매핑을 보존합니다. 최초 동기화 실패는 검색 응답의 `CATALOG_UNAVAILABLE`로 드러납니다. Mock을 늦게 켰다면 검색 서버를 재시작합니다.
- **검색:** 모든 보유 숙소를 공급사별 50개씩 나눠 병렬 조회합니다. 한 검색에서 공급사별 최대 4개 요청, 전체 공급사 작업은 최대 8초입니다. 한 묶음이 실패하면 해당 공급사 결과 전체를 실패 처리하여 조용한 일부 누락을 피합니다.
- **타임아웃:** 연결 300ms, 응답 1,200ms, 한 HTTP 요청 전체 1,500ms. 로컬 Mock에서 정상 호출과 고장을 명확하게 구별하기 위한 초기값이며 운영값은 지연 분포로 조정해야 합니다. MVC 비동기 응답은 10초 제한입니다.
- **재시도:** 현재 구현은 자동 재시도하지 않습니다. 장애 때 지연과 요청 증폭을 피하고, 인증·잘못된 요청·정규화 오류를 반복하지 않기 위한 선택입니다.
- **부분 실패:** 검색 처리 결과는 HTTP 200의 `SUCCESS / PARTIAL_FAILURE / ALL_FAILED`로 구분합니다. 고객 입력 오류는 400입니다. `partialFailure`는 하나 이상 공급사가 실패했는지를 나타내고, `status`가 전체 실패 여부를 구별합니다.
- **MVC + WebClient:** 외부 호출은 비동기이며 servlet 응답도 CompletionStage로 반환합니다. JPA를 사용하는 정규화는 Reactor의 별도 bounded-elastic 스케줄러로 옮겨 네트워크 이벤트 루프에서 DB 호출을 막습니다.

## 검증

```sh
# 컴파일·단위/통합 테스트·실행 파일 생성
bash scripts/verify-app.sh

# 빌드 후 실제 두 서버를 띄우는 검증 (추가 도구: curl, jq)
bash scripts/smoke.sh

# 문서·하네스 구조 검사
bash scripts/verify.sh harness
```

Smoke는 기본 18080/19090 포트와 전용 MySQL DB를 사용합니다. `SMOKE_MYSQL_URL`, `SMOKE_MYSQL_USERNAME`, `SMOKE_MYSQL_PASSWORD`를 설정한 뒤 실행하며, 운영 DB를 사용하면 안 됩니다. 정상 검색, A 무응답, B 본문 오류, 전체 실패, 잘못된 입력, 복구·재시작 시 ID 유지를 검사하고 실행한 프로세스를 종료합니다. 로그와 결과 JSON은 출력된 임시 디렉토리에 남습니다. 포트 변경은 `SMOKE_API_PORT`, `SMOKE_MOCK_PORT` 환경 변수로 가능합니다.

## 문서

- [아키텍처와 확장](docs/architecture.md)
- [도메인 모델](docs/domain/overview.md)
- [API 계약](docs/api.md)
- [개발 안내](docs/development.md)
- [검증 범위와 한계](docs/verification.md)
- [과정 및 AI 활용 기록](JOURNAL.md)

원본 참고 문서와 로컬 다운로드 자료는 배포 대상에 포함하지 않습니다. 코드 자동 생성 이후 사람의 최종 검토와 설명 가능성 확인은 별도로 필요합니다.
