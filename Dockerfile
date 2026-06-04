# ── Etapa 1: Build con Maven oficial (más estable que mvnw en Docker) ──
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Descarga dependencias primero (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

# Compila el proyecto
COPY src src
RUN mvn package -DskipTests -q

# ── Etapa 2: Runtime ligero ─────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -Xms256m -Xmx400m -jar app.jar"]
