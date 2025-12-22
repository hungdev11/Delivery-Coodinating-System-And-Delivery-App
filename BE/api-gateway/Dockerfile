# Build argument for repository source (auto-link package to repo)
ARG GITHUB_REPOSITORY=""

FROM maven:3.9-eclipse-temurin-22 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:22-jre-jammy

# Label for GitHub Container Registry auto-linking
ARG GITHUB_REPOSITORY=""
LABEL org.opencontainers.image.source="https://github.com/${GITHUB_REPOSITORY}"

# Install wget for health checks
# Some environments block plain HTTP access to Ubuntu mirrors, which breaks apt
# with "does not have a Release file". Switch sources to HTTPS before updating.
RUN sed -i 's|http://|https://|g' /etc/apt/sources.list && \
    apt-get update -qq && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends wget && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /app/target/*.jar api-gateway.jar

EXPOSE 21500

ENTRYPOINT ["java", "-jar", "api-gateway.jar"]
