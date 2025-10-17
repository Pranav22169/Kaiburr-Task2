# ---------------------------------------------------
# Kaiburr Task 2 - Spring Boot + MongoDB + Kubernetes
# ---------------------------------------------------

# ======== Build Stage ========
FROM eclipse-temurin:17-alpine as builder
WORKDIR /app

# Copy Maven wrapper and config
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (cached)
RUN ["sh", "-c", "chmod +x mvnw || true"]
RUN ./mvnw dependency:go-offline -B

# Copy source and build the application
COPY src src
RUN ./mvnw -DskipTests package

# ======== Runtime Stage ========
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy built jar from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose app port
EXPOSE 8080

# Default command
ENTRYPOINT ["java","-jar","/app/app.jar"]
