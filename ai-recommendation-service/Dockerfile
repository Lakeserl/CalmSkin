FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8096
ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=50.0", \
  "-XX:+UseG1GC", \
  "-XX:MaxGCPauseMillis=100", \
  "-jar", "app.jar"]
