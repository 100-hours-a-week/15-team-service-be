# Flyway 적용 사항 및 사용 가이드

## 1) 적용 현황 요약
- 프로파일별 설정
  - `application-dev.yml`: `spring.flyway.enabled=false` (데이터베이스 버전이 달라 현재는 dev 프로파일에서는 비활성, 추후 dev DB 생성 시 추가 설정 예정)
  - `application-prod.yml`: `spring.flyway.enabled=true`, `locations=classpath:db/migration`, `baseline-on-migrate=true`, `baseline-version=20260204.0`
- 마이그레이션 파일 위치
  - `src/main/resources/db/migration/`
  - 예: `V20260204_0__init_schema.sql`, `V20260204_1__seed_positions_and_chatrooms.sql`

## 2) 사용 절차

### A. 새 마이그레이션 작성
1. 파일 생성
   - 위치: `src/main/resources/db/migration/`
   - 파일명 규칙: `VyyyyMMdd_n__설명.sql`
     - 예시: `V20260205_0__add_user_index.sql`
2. SQL 작성 시 주의
   - 운영 환경 기준으로 반복 실행에 안전하도록 작성해주세요.
   - seed 데이터는 중복 삽입 방지 SQL문을 사용합니다.

### B. 어떤 파일을 바꿔야 하나요?
- **스키마/데이터 변경**: `src/main/resources/db/migration/V*.sql` 신규 추가
- **Flyway 동작 변경**: `application-dev.yml` / `application-prod.yml`의 `spring.flyway` 설정 수정
- **의존성 변경**: `build.gradle`

## 3) 변경 후 해야 할 일

### 공통
- 신규 마이그레이션 파일이 실행 가능하도록 파일명 규칙, SQL 문법 필히 확인할 것
- 마이그레이션 실행 로그에서 적용 여부 확인할 것

### prod 배포 시
- 배포 로그에서 Flyway 적용 여부 확인
- 초기 덤프(`V20260204_0__init_schema.sql`)와 운영 스키마 정합성 점검

## 4) 트러블슈팅 간단 가이드
- 적용 실패 시
  - 오류 로그의 실패 버전/SQL 확인
  - 해당 버전 SQL 수정 후 새 버전으로 재배포 (**기존 버전 수정 금지**)
- seed 데이터 중복 발생 시
  - `INSERT ... WHERE NOT EXISTS` 형태로 작성

## 5) 권장 운영 원칙
- 운영 DB에 적용된 마이그레이션은 **절대 수정 금지**입니다.
- 롤백이 필요한 경우 **역방향 마이그레이션**을 신규 버전으로 추가합니다.
