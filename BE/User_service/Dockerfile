
FROM maven:3.9-eclipse-temurin-22 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

FROM openjdk:22-slim

WORKDIR /app

COPY --from=build /app/target/*.jar user-module.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "user-module.jar"]
