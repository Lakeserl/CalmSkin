FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Download haarcascade XML needed for OpenCV face detection
RUN apk add --no-cache wget && \
    mkdir -p src/main/resources/opencv && \
    wget -q -O src/main/resources/opencv/haarcascade_frontalface_default.xml \
        https://raw.githubusercontent.com/opencv/opencv/4.x/data/haarcascades/haarcascade_frontalface_default.xml \
    || (echo "Warning: haarcascade download failed — face detection will run in pass-all mode" && \
        touch src/main/resources/opencv/haarcascade_frontalface_default.xml)

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8095
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=50.0", \
  "-XX:+UseG1GC", \
  "-XX:MaxGCPauseMillis=100", \
  "-jar", "app.jar"]
