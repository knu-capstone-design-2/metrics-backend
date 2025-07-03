# 1단계: 빌드용 이미지 (Gradle + JDK 21)
FROM gradle:8.5.0-jdk21 AS build
WORKDIR /app

# 루트 프로젝트 전체 복사
COPY . .

# consumer 모듈만 빌드
RUN gradle clean :consumer:build -x test

# 2단계: 실행용 이미지 (JRE 21)
FROM eclipse-temurin:21-jre
WORKDIR /app

# 빌드된 JAR 복사
COPY --from=build /app/consumer/build/libs/consumer-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]


# 기존 방식
#FROM eclipse-temurin:21-jre
#WORKDIR /app
#COPY build/libs/consumer-0.0.1-SNAPSHOT.jar app.jar
#ENTRYPOINT ["java", "-jar", "app.jar"]