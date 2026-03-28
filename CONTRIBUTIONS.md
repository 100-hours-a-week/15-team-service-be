# 개발 기여 내역 (신윤지)

- **기여자**: 신윤지(Theta) @tl1l1l1s
- **참여 버전**: V1 ~ V3
- **총 PR**: 107개 · **총 Issue**: 84개

---

## 기술적 도전

### 🏗️ Event Sourcing + MongoDB Projection 기반 이력서 아키텍처 전환

**문제 상황**

이력서 생성/수정은 AI 서버와의 비동기 연동으로 처리되며, `QUEUED → PROCESSING → SUCCEEDED / FAILED` 상태 전이가 발생합니다.

기존 MySQL CRUD 구조에서는 두 가지 문제가 있었습니다.
1. 이전 버전의 AI 완료 콜백이 뒤늦게 도달해 최신 상태를 잘못 덮어쓸 수 있음
2. 버전별 이력서 내용을 조회할 때마다 이벤트를 처음부터 replay해야 해 p90 응답시간이 높게 측정됨

**해결 방안**

- Event Sourcing + CQRS 패턴 도입
    - `resume_events` 컬렉션: 이력서 이벤트를 append-only로 저장
    - `resumes` 컬렉션: Read Model/Projection으로 최신 상태를 미리 계산해 저장
    - `lastAppliedVersionNo` 필드로 중복 이벤트를 멱등하게 처리
- 원자성 보장
    - `findAndModify`로 Check-Then-Act 경쟁 조건 방지
    - `@Retryable` 3회 + 지수 백오프, `@Recover`로 락 강제 해제
- 운영 지원
    - CLI `--rebuild-projection=<resumeId|all>` 옵션으로 이벤트 리플레이 및 Projection 재구성 가능

**성과**

- 상세 조회 p90 기준 **-27%**, 목록 조회 p90 **-31%**, 생성/수정 p90 **-28%** 응답시간 단축
- 이력서 이벤트 이력 영구 보관 및 감사 추적 가능

---

### 🔒 Redis 분산 락을 통한 중복 요청 차단 및 정합성 강화

**문제 상황**

이력서 생성/수정 요청은 AI 서버 처리에 최대 5분이 소요됩니다. 이 대기 시간 동안 동일한 요청이 중복으로 들어오면 모든 요청이 MongoDB까지 도달해 불필요한 쿼리 부하가 발생하고, p95/p99 지연시간이 불안정해졌습니다.

**해결 방안**

- Redis 기반 분산 락으로 MongoDB 도달 전 중복 요청을 409로 즉시 차단
    - CREATE 락: `resume:lock:create:{userId}` / EDIT 락: `resume:lock:edit:{resumeId}`
    - 획득 결과를 `ACQUIRED / BUSY / FALLBACK` enum으로 표현해 서비스 레이어에서 상태별 분기 처리
- Lua Script로 락 해제 원자화
    - `GET → DEL`을 분리하면 TTL 만료 직후 다른 요청이 새 락을 획득한 사이에 이전 DEL이 그 락을 삭제할 수 있음
    - `SET NX PX` 획득 + Lua compare-and-del 해제로 본인이 획득한 락만 삭제 보장
- fast-path / fallback-path 분기로 정상 경로 비용 절감
    - `ACQUIRED` 시: `findAndModify` 중복 검증 하지 않는 메서드 사용 (Redis가 1차 동시성을 보장)
    - `FALLBACK` 시: Redis 예외·장애 시에만 기존 MongoDB 중복 검증 경로 유지
    - `BUSY` 시: 즉시 409 반환
- MongoDB partial unique 인덱스로 최종 쓰기 invariant 보장
    - `resume_events`에 `is_pending=true` partial unique 인덱스를 추가해 동일 resume에 pending 이벤트가 동시에 2개 저장되지 않도록 DB가 최종 차단

**성과**

- MongoDB 도달 전 중복 요청 차단으로 DB 쿼리 부하 감소
- Redis 정상 경로에서 MongoDB 중복 검증 비용 제거로 p95/p99 지연시간 안정화
- Redis 장애 시 명시적 fallback으로 서비스 연속성 유지
- MongoDB partial unique 인덱스로 애플리케이션 레벨에 의존하지 않는 최종 쓰기 보장

---

### 📨 Outbox + RabbitMQ 기반 신뢰성 있는 비동기 이벤트 처리

**문제 상황**

AI 서버와의 이력서 생성/수정 연동에서 두 가지 신뢰성 문제가 있었습니다.

1. **이벤트 유실**: AI 요청 발행 직후 서버가 재시작되면 발행이 유실될 수 있음
2. **중복 소비**: RabbitMQ의 at-least-once 환경에서 동일 이벤트가 재전달될 경우 중복 처리 부작용 발생 가능

**해결 방안**

- Outbox 패턴으로 유실 방지
    - 도메인 로직과 같은 트랜잭션 내에서 Outbox 테이블에 이벤트 적재
    - `OutboxEventPublisherWorker`가 PENDING/RETRY 이벤트를 주기 발행 (publish-confirm 포함)
- EventConsumeLog 기반 멱등성으로 중복 소비 무해화
    - 이벤트 소비 시작 시점에 STARTED 상태로 선점
    - 이미 성공한 이벤트는 `ALREADY_SUCCEEDED` 판별 후 즉시 skip
- Lease 기반 stale reclaim으로 비정상 종료 복구
    - `lease_expires_at` 컬럼 추가: 서버가 처리 중 죽은 경우 일정 시간 후 다음 소비자가 해당 이벤트를 회수해 재처리
    - 이력서는 유저당 동시에 하나만 생성·수정 가능한 기획이 있었기에 PROCESSING 고착은 치명적인 UX 문제, lease 기반 자동 복구가 필수
- 외부 I/O를 트랜잭션 밖으로 분리
    - AI HTTP 호출, publish-confirm 대기를 트랜잭션 외부로 이동해 DB 커넥션 점유 시간 단축

**성과**

- at-least-once 전달 보장, 중복 소비 완전 무해화
- 서버 재시작 시에도 이벤트 유실 없음
- 비정상 종료 시 lease 만료 후 자동 재처리

---

### 🧹 Spring Batch + MySQL RANGE 파티셔닝 기반 운영 데이터 정리 자동화

**문제 상황**

RabbitMQ 이벤트 아키텍처 도입 후 Outbox 테이블과 EventConsumeLog 테이블에 이벤트 데이터가 지속적으로 누적됩니다. 단순 DELETE 방식으로 대량 데이터를 정리하면 디스크 파편화와 인덱스 재정렬로 성능 저하가 발생할 수 있습니다.

**해결 방안**

- Outbox 정리: Spring Batch + Keyset Pagination
    - 매일 새벽 2시 자동 실행
    - 오프셋 방식은 삭제 중 데이터가 밀려 다음 페이지를 건너뛰는 문제 발생 → `WHERE id > :lastId` Keyset Pagination으로 정확한 조회 보장
    - ID 전용 Reader로 메모리 부하 최소화, `deleteAllByIdInBatch`로 청크당 단 1번 DELETE 쿼리
- EventConsumeLog 정리: MySQL RANGE 파티셔닝
    - TIMESTAMP 기반 월별 파티션 구성
    - 매월 1일 +3개월 미래 파티션 자동 생성 (`REORGANIZE PARTITION pFuture INTO (...)`)
    - 오래된 파티션 `DROP PARTITION` → OS 레벨 파일 삭제로 수백만 건도 ms 내 완료, 디스크 파편화 없음
- ShedLock JDBC 기반 분산 락
    - 배치의 목적이 DB 정리이므로 DB 자체를 락 저장소로 사용 (의존성 최소화, 장애 도메인 격리)
    - `shedlock` 테이블로 다중 인스턴스 환경에서 중복 실행 방지

**성과**

- 운영 인력 개입 없이 데이터 자동 정리
- 대량 데이터 삭제 없이 파티션 DROP으로 즉각 공간 회수

---

### ⚡ 트랜잭션 범위 최적화 및 Lock Contention 감소

**문제 상황**

1. 토큰 재발급 시 토큰 생성·해시 계산·Redis 캐시 갱신이 모두 DB 트랜잭션 내에 포함되어 외부 I/O 대기 시간만큼 커넥션을 점유
2. 이력서 상세 조회(`GET /resumes/{id}`)에서 `SELECT ... FOR UPDATE`(PESSIMISTIC_WRITE)를 사용해, 이름 변경/삭제/수정 등 write 작업이 조회 요청에 블로킹되는 상황 발생

**해결 방안**

- 트랜잭션 범위 명시적 제어
    - `TransactionOperations`를 활용해 실제 DB 의존 로직만 트랜잭션 블록 안에 담도록 범위를 명시적으로 제한
    - DB 의존성 없는 토큰 생성·해시 계산을 트랜잭션 밖으로 이동
- DB 커밋 후 Redis 작업 실행
    - `TransactionSynchronizationManager`를 활용해 DB 커밋 후 Redis 캐시 갱신을 처리하는 `RefreshTokenCacheAfterCommitService`를 분리
    - 활성 트랜잭션이 없는 경우 즉시 실행 (호환성 유지)
- PESSIMISTIC_WRITE 제거 후 조건부 UPDATE로 전환
    - 이력서 조회 시 락 없이 일반 조회
    - preview 노출이 필요한 시점에만 `previewShownAt IS NULL` 조건부 UPDATE 수행
    - 동시에 여러 요청이 들어와도 단 하나의 요청만 update 성공 (나머지 0 rows affected)

**성과**

- 토큰 재발급 경로에서 DB 커넥션 점유 시간 감소
- 이력서 조회 중 write 작업 대기 해소, lock contention 감소

---

### 🔍 LCS 기반 의미론적 이력서 Diff 엔진 구현

**문제 상황**

이력서 버전 간 변경점 조회 API에서 인덱스 기반 라인 diff를 사용하면 두 가지 오탐 상황이 발생합니다.

1. 프로젝트 순서를 변경하거나 항목을 삽입하면, 내용 변경이 없어도 이후 모든 항목을 "변경됨"으로 과다 보고
2. 자기소개처럼 긴 텍스트에서 단어 하나를 수정해도 전체 문장을 교체로 인식

**해결 방안**

- 고유 키 기반 배열 매칭으로 순서 변경 감지
    - 배열 항목의 `id`, `repoUrl`, `name` 등 고유 식별자를 추출해 인덱스 대신 식별자 기준으로 이전·이후 버전 항목을 매칭
    - 순서가 바뀐 항목과 실제로 내용이 변경된 항목을 정확히 구분
- 긴 텍스트에 LCS(Longest Common Subsequence) 알고리즘 적용
    - 자기소개 등 multiline 필드는 LCS 기반으로 단어·줄 수준의 변경점 계산
- 고유 키 부재 시 인덱스 비교 폴백
    - 고유 키가 없거나 중복인 경우 안전하게 배열 인덱스 기반 비교로 전환

FE의 라인 기반 diff(즉각적인 UI 반영에 최적화)와 상호보완적으로 동작합니다.

**성과**

- 구조적 변경(순서 변경, 항목 삽입)과 내용 변경을 정확히 구분
- diff 과다 보고 제거로 사용자 변경 내역 가독성 향상

---

### 📡 Redis 기반 분산 SSE 라우팅 (타겟 인스턴스 선별 전달)

**문제 상황**

다중 서버 인스턴스 환경에서 SSE 알림을 전송할 때, 알림 이벤트가 생성된 인스턴스와 해당 유저의 SSE 연결이 맺어진 인스턴스가 다를 수 있습니다. 이 경우 로컬 emitter에만 발송하면 알림이 전달되지 않습니다.

**해결 방안 비교**

두 가지 대안을 검토했습니다.

| 방식 | 장점 | 단점 |
|------|------|------|
| Redis Pub/Sub 브로드캐스트 | 구현 단순, Redis read 없이 publish만 수행 | 인스턴스 수·알림 빈도 증가 시 모든 인스턴스가 불필요하게 동작 |
| Redis 인스턴스 매핑 후 타겟 전달 | 필요한 인스턴스에만 전달 → fan-out 비용 최소 | 재연결/끊김/장애 처리를 포함해 구현 복잡 |

인스턴스 수 증가 시 브로드캐스트 방식은 불필요한 수신·처리가 비례해 증가하고, 유저별 SSE 연결 수가 제한된 구조에서는 타겟 인스턴스를 특정하는 편이 더 합리적이라 판단해 **대안 2를 선택**했습니다.

- Redis에 `sse:route` 키로 유저-인스턴스 매핑 저장 (SET 자료구조, TTL 갱신)
- 알림 발행 시 Redis 조회 → 로컬 인스턴스면 `SseLocalDeliveryDispatcher`로 직접 전달, 원격 인스턴스면 Redis Pub/Sub 채널로 발행
- `RedisSseDeliverySubscriber`가 메시지 수신 후 `targetInstanceId` 확인 → 로컬 디스패처로 전달
- `SseStreamKey(streamType, streamKey)` 구조로 emitter 간 이름 충돌 방지
- SSE 연결 시 Last-Event-ID 헤더 파싱 후 누락 알림 재처리

**성과**

- 다중 인스턴스 환경에서 SSE 알림 전달 보장
- 불필요한 fan-out 없이 타겟 인스턴스에만 전달

---

### 📊 쿼리 실행 계획 분석 및 복합 인덱스 적용

**문제 상황**

채팅방 목록 조회 쿼리가 부하 테스트 종료 후 아예 응답하지 않는 문제가 발생했습니다. 원인을 분석해보니 채팅방별 최신 메시지를 가져오기 위해 `GROUP BY chatroom_id` + `MAX(created_at)` 중 `MAX(id)` 방식을 사용하고 있었고, 이 쿼리가 인덱스를 제대로 활용하지 못하고 풀 스캔에 가깝게 동작하고 있었습니다.

**해결 방안**

- 쿼리 구조 단순화
    - `GROUP BY + MAX()` 중첩 집계를 `ORDER BY createdAt DESC, id DESC LIMIT 1`로 대체
    - 채팅방별 최신 1건을 서브쿼리나 집계 없이 정렬 후 상위 1건 추출
- 복합 인덱스 추가
    - `(chatroom_id, created_at, id)` 복합 인덱스로 정렬 키 전체를 인덱스가 커버하도록 설계
    - ORDER BY 절의 컬럼 순서와 인덱스 컬럼 순서를 일치시켜 filesort 제거

**성과**

- HTTP 처리량 **+55.0%** 증가
- 지연 시간 p95 기준 **-58.3%** 개선
- 부하 테스트 후 채팅방 목록 조회 불가 문제 해결

---

### 🔔 SSE 알림 시스템 구축 및 동시성 안전성 확보

**문제 상황**

SSE는 단방향 스트림 특성상 세 가지 운영 문제를 함께 해결해야 했습니다.

1. 클라이언트가 연결 도중 이탈하면 죽은 emitter가 목록에 남아 계속 전송 시도
2. 서버 재시작 또는 네트워크 단절 시 전달되지 않은 알림이 유실
3. 여러 스레드가 동시에 emitter 목록을 순회·수정할 경우 ConcurrentModificationException 발생

**해결 방안**

- 하트비트 스케줄러로 죽은 emitter 정리
    - 30초 주기로 모든 emitter에 heartbeat 전송, 전송 실패한 emitter는 연결이 끊긴 것으로 판단해 목록에서 제거
- Last-Event-ID 기반 누락 알림 재전송
    - SSE 재연결 시 `Last-Event-ID` 헤더를 파싱해 해당 ID 이후 알림을 DB에서 조회 후 재전송
- 동시성 안전한 자료구조 선택
    - emitter 맵: `ConcurrentHashMap` → 여러 스레드 동시 접근 시 안전
    - emitter 목록 순회: `CopyOnWriteArrayList` → 순회 시작 후 새로 추가된 emitter가 현재 순회에 끼어들지 않도록 스냅샷 기반 순회 보장

**성과**

- 장기 연결 환경에서 emitter 누수 없이 안정적 운영
- 재연결 시 유실 알림 자동 복구

---

### 🔄 @TransactionalEventListener(AFTER_COMMIT) 기반 아키텍처 전환

**문제 상황**

AI 콜백을 처리하는 컨트롤러가 두 가지 책임을 동시에 지고 있었습니다.

1. 이력서 도메인 상태 변경 (DB 저장)
2. SSE 알림 발송

이 구조에서는 DB 트랜잭션이 롤백되더라도 SSE 알림이 이미 발송될 수 있고, 컨트롤러와 알림 서비스가 강하게 결합되어 각 계층을 독립적으로 테스트하거나 변경하기 어려웠습니다.

**해결 방안**

- 도메인 이벤트 클래스 분리
    - `ResumeEditCompletedEvent`, `ResumeEditFailedEvent` 클래스 추가
    - 콜백 서비스는 상태 변경 후 이벤트만 발행, SSE 발송 여부는 관여하지 않음
- `@TransactionalEventListener(phase = AFTER_COMMIT)` 적용
    - `AFTER_COMMIT` 단계에서만 이벤트를 수신하도록 설정
    - DB 트랜잭션이 성공적으로 커밋된 이후에만 SSE 알림 발송을 보장
    - 트랜잭션 롤백 시 이벤트 리스너가 호출되지 않아 잘못된 알림 원천 차단

**성과**

- DB 상태 변경 성공 시에만 SSE 알림 발송 보장 (일관성 확보)
- 컨트롤러·알림 서비스 결합도 제거, 각 계층 단독 테스트 가능

---

### 🛡️ Cache Penetration 방지 설계 (Negative Cache + Rate Limit)

**문제 상황**

존재하지 않는 리소스에 대한 반복 요청이 캐시를 통과해 DB까지 도달하는 Cache Penetration이 발생할 수 있습니다. 특히 Refresh Token 조회처럼 유효하지 않은 토큰 요청이 반복될 경우 DB에 불필요한 부하가 집중됩니다.

**해결 방안 비교**

다양한 대안을 검토했습니다.

| 방안 | 특징 | 한계 |
|------|------|------|
| Negative Cache | invalid 결과를 짧은 TTL로 캐싱 | 매 요청마다 다른 random key가 들어오면 캐시 재사용 불가 |
| Bloom Filter | 존재 가능한 ID 집합 사전 판별 | 삭제 잦은 데이터(토큰 폐기 등)는 Counting Bloom Filter 또는 주기 재구성 필요, 구현 복잡 |
| L1 Local + L2 Redis 이중화 | 짧은 스파이크 보호에 효과적 | 인스턴스 간 캐시 불일치 발생 가능 |
| Single-flight | 동일 요청 중복 조회 방지 | 분산 인스턴스 환경에서는 Redis lock 등 추가 구현 필요 |
| **Rate Limit** (선택) | 동일 사용자/IP 비정상 반복 요청 자체를 제한 | — |

- **Negative Cache**: 동일한 invalid 키 반복 요청에 효과적, 짧은 TTL로 실제 생성 시 오판 방지
- **사용자 기준 Rate Limit**: 매 요청마다 다른 random key를 보내는 경우에도 유효. 이 서비스는 대부분 인증 이후 접근이므로 익명 대량 요청보다 세션 단위 제한이 적합

두 방안을 조합하여 한계를 보완했습니다.

**성과**

- invalid 요청 반복 시 DB 도달 차단
- random key 공격 패턴에 대한 Rate Limit 방어선 구성

---

## 구현 기능 목록

### 프로젝트 초기 설정

| PR | 작업 |
|----|------|
| #1 | 공통 응답 객체, 공통 예외 처리, 페이지네이션 DTO 추가 |
| #2 | REST Docs 기반 API 명세 / Swagger UI / Actuator 헬스 체크 엔드포인트 설정 |
| #3 | 도메인 엔티티 설계 |
| #8 | GitHub Issue/PR 템플릿 정비 |

### 인증 / 보안

| PR | 작업 |
|----|------|
| #4 | GitHub OAuth 로그인 API |
| #5 | 로그아웃 API |
| #7 | Access Token 암호화 저장 |
| #15 | Auth 리다이렉션 URL 변경 및 CORS 설정 |
| #52 | 토큰 재발급 API |
| #197 | Refresh Token Redis 캐시 최적화 및 탈퇴 시 즉시 무효화 |
| #220 | Refresh Token Redis 캐시 갱신을 트랜잭션 커밋 후로 분리 |
| #222 | Refresh Token Rotation 트랜잭션 점유 시간 감축 |

### 사용자 관리

| PR | 작업 |
|----|------|
| #6 | 온보딩 API, 희망 포지션 조회 API, 쿠키 설정 분리 |
| #10 | `@CurrentUserId` 도입 및 공통 DTO 구조 개선 |
| #11 | 유저 정보 조회/수정 API |
| #14 | 유저 설정 조회/수정 API |
| #181 | 탈퇴 후 1일 대기 재가입 로직 및 기존 데이터 격리 |

### 채팅 도메인 (be 측)

| PR | 작업 |
|----|------|
| #17 | 채팅방 목록 조회 API |
| #20 | 채팅 내역 조회 API |
| #30 | 채팅 내역 조회 정렬 기준 수정 |
| #46 | 웹소켓 연결 및 채팅 전송 API |

### 파일 업로드

| PR | 작업 |
|----|------|
| #40 | S3 Presigned URL 발급 및 업로드 완료 확정 API |
| #47 | S3 업로드 이미지 조회용 URL 발급 API |

### 이력서 도메인

| PR | 작업 |
|----|------|
| #196 | 유저 이력서 인적사항 추가/수정 API, `syncEntitiesById()` 공통 패턴 도입 |
| #226 | 개인정보 스냅샷 저장, 이력서 응답에 인적사항 포함, 트랜잭션 구조 개선 |
| #239 | 이력서 버전 목록 조회 API, 의미 기반 Diff 엔진 구현 |
| #242 | MongoDB Projection 기반 이력서 최신 버전 조회 구조 전환 |
| #244 | 이력서 생성·수정 Redis 분산 락 적용 |
| #245 | Redis 락 정합성 강화 및 fast-path/fallback-path 분리 |

### 알림 / 실시간

| PR | 작업 |
|----|------|
| #192 | 이력서 생성/수정 완료 알림 payload 보강 |
| #218 | 로컬 캐시 warm-up 및 Redis Pub/Sub 기반 캐시 갱신 API |
| #224 | RabbitMQ + Outbox 패턴 기반 비동기 이벤트 처리 아키텍처 도입 |

### 성능 / 운영

| PR | 작업 |
|----|------|
| #238 | Spring Batch + MySQL RANGE 파티셔닝 기반 운영 데이터 정리 자동화 |
