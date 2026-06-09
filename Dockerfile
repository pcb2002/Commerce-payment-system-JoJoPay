# 1. 빌드 스테이지: Gradle을 이용해 스프링 코드를 실행 가능한 JAR 파일로 바꿉니다.
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY . .
# GitHub Actions 환경에서 이미 테스트를 거치므로 빌드 시에는 테스트를 제외합니다.
RUN ./gradlew clean bootJar -x test

# 2. 실행 스테이지: 가볍고 안전한 자바 환경에서 JAR 파일만 올려서 실행합니다.
FROM openjdk:17-jdk-slim
WORKDIR /app
# 빌드 스테이지에서 생성된 JAR 파일을 가져옵니다.
COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar

# 서버 시간대를 대한민국 서울 시간으로 고정합니다. (결제/환불 시간 정합성 중요)
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

EXPOSE 8080
# 서버 운영 환경 프로필(prod)을 활성화하여 실행합니다.
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]