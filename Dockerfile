# Build stage
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B
# Build application
COPY . .
RUN mvn package -DskipTests -B

# Run stage
FROM eclipse-temurin:25-jre
# Install native dependencies for audio decryption (DAVE) and compression (ffmpeg)
RUN apt-get update && apt-get install -y \
    libopus0 \
    libsodium23 \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/opexy-1.0.0.jar /app/opexy-bot.jar
WORKDIR /app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:+ZGenerational", "-jar", "opexy-bot.jar"]
