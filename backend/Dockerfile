# --- build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# --- runtime stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/mytaman-backend.jar app.jar
ENV VAULT_DIR=/vault
ENV PORT=7000
EXPOSE 7000
ENTRYPOINT ["java", "-jar", "app.jar"]
