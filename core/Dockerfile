# syntax=docker/dockerfile:1
# Build a partir da raiz do back-java: docker build -f core/Dockerfile .

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY core/pom.xml core/
RUN --mount=type=cache,target=/root/.m2/repository \
    --mount=type=secret,id=maven-settings,target=/root/.m2/settings.xml \
    mvn -B -ntp -pl core -am dependency:go-offline
COPY core/src core/src
RUN --mount=type=cache,target=/root/.m2/repository \
    --mount=type=secret,id=maven-settings,target=/root/.m2/settings.xml \
    mvn -B -ntp -pl core -am package -DskipTests

FROM eclipse-temurin:25-jre AS extract
WORKDIR /extract
COPY --from=build /workspace/core/target/quickorder-core.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:25-jre
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /application
COPY --from=extract /extract/extracted/dependencies/ ./
COPY --from=extract /extract/extracted/spring-boot-loader/ ./
COPY --from=extract /extract/extracted/snapshot-dependencies/ ./
COPY --from=extract /extract/extracted/application/ ./
USER app
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC"
ENTRYPOINT ["java", "-jar", "application.jar"]
