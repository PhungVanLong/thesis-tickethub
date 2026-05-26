# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY /target/identity*.jar /app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app.jar"]
