# CommitMe Backend

## 프로젝트 소개

**CommitMe**는 GitHub Repository를 기반으로 AI가 이력서를 자동 생성하고, 모의 면접까지 연습할 수 있는 취업 준비 플랫폼입니다. <br />
개발자가 자신의 GitHub 레포지토리를 선택하면 AI가 프로젝트 내역을 분석해 이력서 초안을 작성해줍니다. AI 채팅으로 해당 이력서룰 수정하거나 면접 질문을 받아 모의 면접을 진행할 수 있습니다.

### [Wiki 링크](https://github.com/100-hours-a-week/15-team-service-wiki/wiki)
### [Backend wiki 링크](https://github.com/100-hours-a-week/15-team-service-wiki/wiki/Backend-Wiki)

## 시연 영상
클릭 시 유튜브로 이동합니다.

[![Video Label](https://github.com/user-attachments/assets/47de4c94-b541-4958-8b5d-1431b69dfdbc)](https://youtu.be/l2KNFv0aIR0)

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language / Framework | Java 21, Spring Boot 3.5 |
| Web / Security | Spring Web, Validation, Security, OAuth2 |
| Data | Spring Data JPA, MongoDB, Redis / MySQL, MongoDB, Redis |
| Messaging | Spring AMQP, RabbitMQ |
| Batch | Spring Batch |
| Migration | Flyway |
| Observability | Micrometer, Prometheus |
| Storage | AWS S3 SDK |
| Test / Docs | REST Docs, OpenAPI, JUnit 5 |

## 아키텍처

### 구성

<img width="1500" alt="인프라 아키텍처" src="https://github.com/user-attachments/assets/b10a90a0-c466-4fbc-a888-55d7b5bbb4e9" />

### 화면 구성
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/8f6e722d-b275-459c-a81c-8d20ad1f3182" />
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/1705919c-fab2-47f7-a487-28bc79169542" />
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/7f48ef33-137c-4a8e-86b1-3626f6dc40c9" />
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/0bd5b60f-22b2-48b0-808d-bea45d5c3d73" />
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/48bef472-35ae-40d5-8aeb-62ec47355d8b" />

## 디렉터리 구조

```text
be/
├─ src/main/java/com/sipomeokjo/commitme
│  ├─ domain/          # 인증, 유저, 이력서, 면접, 알림 등 비즈니스 도메인
│  ├─ security/        # JWT, 인증/인가 필터 및 보안 설정
│  ├─ config/          # 공통 설정
│  ├─ batch/           # 배치/정리 작업
│  ├─ metrics/         # 관측성 관련 코드
│  └─ api/             # 공통 응답/예외/검증 계층
├─ src/main/resources
│  ├─ db/migration/    # Flyway 마이그레이션
│  └─ application-*.yml
├─ loadtest/           # k6 시나리오 및 실행 가이드
└─ build.gradle
```

## 개발 기여 내역
| 이름                                                                                                                                               | 역할      | 담당 기능                          |
|--------------------------------------------------------------------------------------------------------------------------------------------------|-------------|------------------------------------|
| [<img src="https://avatars.githubusercontent.com/u/96182623?v=4" height=130 width=130> <br/> @tl1l1l1s](https://github.com/tl1l1l1s) **신윤지(Theta)**     | 풀스택  | 이력서, 채팅, 알림 <br /> **[담당 구현 내역 및 기술적 도전](./CONTRIBUTIONS.md)** |
| [<img src="https://avatars.githubusercontent.com/u/145419432?v=4" height=130 width=130> <br/> @minzero0](https://github.com/minzero0) **안민영(Zero)**     | 풀스택  | 이력서, 모의면접 <br />  |
