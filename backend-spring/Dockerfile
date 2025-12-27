# Build stage
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY backend-spring/pom.xml ./
COPY backend-spring/maven-settings.xml /root/.m2/settings.xml
RUN mvn -q -e -s /root/.m2/settings.xml -DskipTests dependency:go-offline
COPY backend-spring/src ./src
RUN mvn -q -e -s /root/.m2/settings.xml -DskipTests package

# SkyWalking agent
FROM eclipse-temurin:21-jre AS agent
WORKDIR /
COPY packages/opentelemetry-javaagent.jar /opentelemetry-javaagent.jar

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV JAVA_OPTS=""
COPY --from=build /app/target/backend-0.1.0.jar /app/app.jar
COPY --from=agent /opentelemetry-javaagent.jar /opentelemetry-javaagent.jar
EXPOSE 3001
ENTRYPOINT ["bash","-lc","java -javaagent:/opentelemetry-javaagent.jar -Dotel.service.name=flashsale -Dotel.exporter.otlp.endpoint=http://tempo:4317 -Dotel.exporter.otlp.protocol=grpc ${JAVA_OPTS} -jar /app/app.jar"]

