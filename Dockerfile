# ===== 1단계: 빌드 =====
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 의존성 캐시 최적화: 래퍼/빌드스크립트 먼저 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle* ./
RUN chmod +x gradlew

# 소스 복사 후 jar 빌드 (테스트는 배포 빌드에서 제외)
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

# ===== 2단계: 실행 =====
FROM eclipse-temurin:21-jre
WORKDIR /app

# 운영 프로파일로 구동 (Railway 변수로 덮어쓸 수 있음)
ENV SPRING_PROFILES_ACTIVE=prod

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
