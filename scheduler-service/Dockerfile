FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY shared/pom.xml shared/pom.xml
COPY scheduler-service/pom.xml scheduler-service/pom.xml
COPY worker-service/pom.xml worker-service/pom.xml
COPY shared/src shared/src
COPY scheduler-service/src scheduler-service/src

RUN mvn -pl scheduler-service -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --create-home --uid 10001 appuser
COPY --from=build /workspace/scheduler-service/target/scheduler-service-*.jar app.jar

USER appuser
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
