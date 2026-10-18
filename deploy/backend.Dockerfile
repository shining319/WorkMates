# Build context: backend/WorkMates (set in the deploy workflow)

# --- build stage: compile and package the jar (tests already ran in CI) ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Copy pom.xml first so the dependency layer is cached until the pom changes
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -DskipTests package

# --- run stage: JRE only ---
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home app
COPY --from=build /app/target/*.jar app.jar
USER app
EXPOSE 8080
# Size the heap from the container's memory limit, so a small VPS is not overrun
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
