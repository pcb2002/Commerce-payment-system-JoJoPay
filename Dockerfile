FROM eclipse-temurin:17-jdk-alpine
LABEL authors="PMS"

# 1. 작업 폴더 이름을 /jojopay 로 변경
WORKDIR /jojopay

# 2. 복사해 올 파일 이름을 jojopay-api.jar 로 변경
COPY build/libs/*.jar jojopay-api.jar

# 3. 실행할 때도 바뀐 이름으로 실행!
ENTRYPOINT ["java", "-jar", "jojopay-api.jar"]