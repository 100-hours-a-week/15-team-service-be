# 빌드는 CI에서 실행됐을 예정이므로 생략

# 실행 스테이지 단계 (이미지 경량화)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# CI에서 이미 만들어둔 jar 파일 복사
COPY build/libs/*.jar app.jar

# 채팅 서버 포트 (기본 8080)
EXPOSE 8080

# 실행 명령
ENTRYPOINT ["java", "-jar", "app.jar"]