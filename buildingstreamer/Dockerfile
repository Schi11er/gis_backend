# Alternative: Direkter Maven Build ohne Wrapper
FROM eclipse-temurin:21-jdk-alpine AS build

# Maven installieren
RUN apk add --no-cache maven

# Arbeitsverzeichnis setzen
WORKDIR /app

# pom.xml kopieren für besseres Caching
COPY pom.xml ./

# Dependencies downloaden
RUN mvn dependency:go-offline -B

# Quellcode kopieren
COPY src ./src

# Anwendung kompilieren
RUN mvn clean package -DskipTests

# Production image
FROM eclipse-temurin:21-jre-alpine AS production

# Alpine Linux Tools für Health Check installieren
RUN apk add --no-cache curl

# Nicht-root User für Security
RUN addgroup -g 1001 -S appuser && adduser -u 1001 -S appuser -G appuser

# Arbeitsverzeichnis setzen
WORKDIR /app

# JAR aus Build-Stage kopieren
COPY --from=build /app/target/*.jar app.jar

# Ownership ändern
RUN chown appuser:appuser app.jar

# User wechseln
USER appuser

# Port exposieren
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Anwendung starten
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
