FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn -B -ntp -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -DskipTests package && cp target/*.jar /app/avies.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system avies && useradd --system --gid avies avies && mkdir -p /app/storage && chown avies:avies /app/storage
COPY --from=build /app/avies.jar /app/avies.jar
USER avies
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65.0", "-jar", "/app/avies.jar"]
