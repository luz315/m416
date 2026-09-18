# 검증

## 명령과 범위

| 명령 | 범위 |
|---|---|
| `bash scripts/verify.sh harness` | 필수 문서·Skill 메타데이터·단순 상대 링크·셸 문법 |
| `bash scripts/verify-app.sh` | Gradle 테스트, 네 모듈 컴파일, 실행 JAR 생성 |
| `bash scripts/verify.sh` | 하네스와 제품 검증 |
| `bash scripts/smoke.sh` | 빌드한 두 프로세스와 전용 MySQL DB의 실제 HTTP 통합 검증 |

하네스 구조 통과는 코드 품질이나 Skill 행동 품질을 보장하지 않는다. Python 검증기는 사용하지 않는다. 링크 검사는 코드 블록/inline code 밖의 단순 상대 링크만 다루며 외부 URL과 복잡한 Markdown은 검사하지 않는다.

제품 검증은 실제 Gradle 결과를 따른다. Smoke는 curl/jq와 사용 가능한 로컬 포트가 필요하며 격리된 임시 DB를 만들고 자신이 띄운 프로세스만 종료한다. 하네스의 0은 통과, 1은 실패, 2는 미설정/사용법 오류다. 환경 차단·미실행은 통과로 보고하지 않는다.

## 자동 테스트

- Core: 연박 최소 재고와 0, 누락 날짜, 기간/인원 검증, 병렬 요청 시작, 부분 실패, 정상 빈 결과와 전체 실패 구분.
- Infrastructure: MySQL 호환 모드 H2 JPA에서 매핑의 복합키·ID 안정성·비활성화/복구·원자적 rollback, A/B 요금 정규화, 조식 차이, 중복 재고 날짜, B 본문 실패, HTTP 인증 실패, 실제 HTTP 무응답 timeout, 101개 숙소의 분할과 API 키 전달.
- REST: 내부 ID/가격/실패 응답 계약, 누락/잘못된 날짜 400, 전체 실패 상태.

JUnit의 Fake와 로컬 HttpServer를 사용한다. 외부 상용 API를 호출하지 않는다. 기본 테스트는 Docker 의존성을 피하기 위해 H2의 MySQL 호환 모드를 사용하며, 실제 MySQL 연결은 전용 DB 접속 정보를 준 Smoke로 확인한다. REST DTO 변환만을 반복하는 테스트 대신 HTTP 계약을 검사한다.

## 실행 결과 — 2026-09-17

매퍼·JPA 변경 후 `./gradlew test --no-daemon`: 테스트 19개, 실패 0, 오류 0. 양방향 매핑의 필드 보존, 빈 카탈로그와 재활성화, 공급사 격리, 중복 객실 롤백을 추가 검증했다. 이번 변경 후 실제 MySQL Smoke와 실행 JAR 생성은 재실행하지 않았다. 아래 결과는 이전 실행 기록이다.

`./gradlew test bootJar --no-daemon`: 테스트 16개, 실패 0, 오류 0. Core 5개, Infrastructure 8개, REST 3개. Mock 모듈은 실제 프로세스 Smoke에서 함께 검증한다.

`bash scripts/verify.sh harness`: 통과.

`bash scripts/smoke.sh`: 통과. 정상 상품 3개, A 무응답/B 정상, A 무응답+B 본문 오류, 입력 오류 400, 정상 복구, 실제 프로세스 재시작 후 ID 유지를 확인했다. 임시 로그 디렉토리: `stay-smoke.2M5Yk6`.

## 실행 결과 — 2026-09-18

`./gradlew test bootJar --no-daemon`: 통과. Core·Infrastructure·REST 테스트와 두 실행 JAR 생성을 확인했다. Supplier HTTP 계약 테스트는 재시도 정책 자체를 검증하는 경우에만 재시도를 활성화해 Mock 전송 계층의 일시 오류가 계약 검증을 흔들지 않도록 분리했다.

`bash scripts/verify.sh harness`: 통과.

Smoke 검증은 로컬 MySQL이 `127.0.0.1:3306`에서 실행 중이지 않아 이번 실행에서는 시작할 수 없었다. 스크립트는 OpenAPI JSON과 Swagger UI 확인을 포함한다.

## 하네스 모델 배정

Codex CLI 0.154.0의 이전 읽기 전용 시험에서 Main Terra/medium, light-worker Luna/low, deep-worker Sol/high 설정이 실제 스레드에 반영됨을 확인했다. 모델 시험을 일반 제품 변경 때 반복하지 않는다. 난이도별 자동 선택 정확도와 사용량 절약률까지 검증한 것은 아니다.
