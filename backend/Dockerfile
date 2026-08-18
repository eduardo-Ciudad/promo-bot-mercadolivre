# Stage 1: Build (Maven + JDK 17)
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:resolve
COPY src ./src
RUN mvn clean package -DskipTests


# Stage 2: Run (Playwright + JRE 17, inclui Chromium)
FROM mcr.microsoft.com/playwright/java:v1.48.0-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]