# Build stage
FROM maven:3.8.5-eclipse-temurin-17 AS build
WORKDIR /app
# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B
# Build application
COPY . .
RUN mvn package -DskipTests -B

# Run stage
FROM eclipse-temurin:17-jre-jammy
# Install native dependencies for audio decryption (DAVE)
RUN apt-get update && apt-get install -y \
    libopus0 \
    libsodium23 \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/opexy-1.0.0.jar /app/opexy-bot.jar
WORKDIR /app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "opexy-bot.jar"]
