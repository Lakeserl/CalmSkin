# syntax=docker/dockerfile:1.7
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,id=calmskin-maven,target=/root/.m2,sharing=locked \
		for attempt in 1 2 3; do \
			if mvn -B -ntp package -DskipTests; then break; fi; \
			if [ "$attempt" -eq 3 ]; then exit 1; fi; \
			echo "Maven package failed; retrying in $((attempt * 5)) seconds"; \
			sleep "$((attempt * 5))"; \
		done

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]
