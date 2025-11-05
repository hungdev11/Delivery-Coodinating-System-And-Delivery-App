
FROM maven:3.9-eclipse-temurin-22 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:22-jre-jammy

WORKDIR /app

COPY --from=build /app/target/*.jar api-gateway.jar

EXPOSE 21500

ENTRYPOINT ["java", "-jar", "api-gateway.jar"]
