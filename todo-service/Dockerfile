# ---------- STAGE 1 : Build ----------
FROM gradle:8.5-jdk21 AS builder
WORKDIR /app

COPY . .
RUN gradle bootJar --no-daemon

# ---------- STAGE 2 : Runtime ----------
FROM eclipse-temurin:21-jdk
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
