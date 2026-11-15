# Build stage
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -B -DskipTests package

# Runtime stage
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/target/api-service-*.jar app.jar
ARG GIT_SHA=unknown
ENV GIT_SHA=${GIT_SHA}
EXPOSE 8080
USER 1001:1001
ENTRYPOINT ["java", "-jar", "app.jar"]