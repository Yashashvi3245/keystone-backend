# =====================================================================
# KEYSTONE Backend — Multi-stage Dockerfile
#
# Stage 1 (builder):  Maven + JDK 21 — compiles and packages the JAR
# Stage 2 (runtime):  JRE 21 slim — runs the fat JAR as a non-root user
# =====================================================================

# ── Stage 1: build ───────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Download dependencies before copying source (layer-cache friendly)
COPY .mvn/           .mvn/
COPY mvnw            ./
RUN  chmod +x mvnw

COPY pom.xml         ./
RUN  ./mvnw dependency:go-offline -q

# Now copy source and build
COPY src             ./src
RUN  ./mvnw package -DskipTests -q

# ── Stage 2: runtime ─────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Run as non-root for security
RUN  addgroup -S keystone && adduser -S keystone -G keystone
USER keystone

# Copy the fat JAR produced by Maven
COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
            "-XX:MaxRAMPercentage=75.0", \
            "-XX:+UseContainerSupport", \
            "-jar", "app.jar"]
