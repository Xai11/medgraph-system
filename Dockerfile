FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
ARG SERVICE

COPY pom.xml ./
COPY fact-service/pom.xml fact-service/pom.xml
COPY graph-service/pom.xml graph-service/pom.xml
COPY ${SERVICE}/src/ ${SERVICE}/src/
RUN mvn -B -ntp -pl "${SERVICE}" -am package

FROM eclipse-temurin:21-jre
WORKDIR /app
ARG SERVICE
COPY --from=build /workspace/${SERVICE}/target/${SERVICE}.jar app.jar
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
