# ─────────────────────────────────────────────────────────────────────────────
# NixSpace API — Multi-Stage Dockerfile
# Stage 1: Build with Maven
# Stage 2: Lean runtime image (JRE only)
# ─────────────────────────────────────────────────────────────────────────────

# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

# Cache Maven dependencies separately from source code
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B 2>/dev/null || true

COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -B \
    && mkdir -p target/extracted \
    && java -Djarmode=layertools -jar target/*.jar extract --destination target/extracted


# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

# Non-root user for security
RUN addgroup -S nixspace && adduser -S nixspace -G nixspace

WORKDIR /app

# Copy layered JAR (fastest Docker layer cache reuse)
COPY --from=builder --chown=nixspace:nixspace /workspace/target/extracted/dependencies/          ./
COPY --from=builder --chown=nixspace:nixspace /workspace/target/extracted/spring-boot-loader/   ./
COPY --from=builder --chown=nixspace:nixspace /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=nixspace:nixspace /workspace/target/extracted/application/          ./

USER nixspace

EXPOSE 8080

# Spring Boot layered JAR launcher
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "org.springframework.boot.loader.launch.JarLauncher"]
