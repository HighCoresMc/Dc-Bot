FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY . .
RUN mvn package -DskipTests -B

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y \
    libopus0 \
    libsodium23 \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/opexy-1.0.0.jar /app/opexy-bot.jar
WORKDIR /app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:+ZGenerational", "-jar", "opexy-bot.jar"]
