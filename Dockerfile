FROM maven:3.9-eclipse-temurin-11 AS build
WORKDIR /build
COPY pom.xml .
COPY config ./config
COPY eligibility-domain/pom.xml eligibility-domain/
COPY eligibility-application/pom.xml eligibility-application/
COPY eligibility-infrastructure/pom.xml eligibility-infrastructure/
COPY eligibility-presentation/pom.xml eligibility-presentation/
COPY eligibility-architecture-tests/pom.xml eligibility-architecture-tests/
RUN mvn -B -q dependency:go-offline
COPY . .
RUN mvn -B -DskipTests package

FROM eclipse-temurin:11-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 app
WORKDIR /app
COPY --from=build /build/eligibility-presentation/target/eligibility-presentation-*-boot.jar app.jar
USER 10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
