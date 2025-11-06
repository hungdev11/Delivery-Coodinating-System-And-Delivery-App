FROM maven:3.9-eclipse-temurin-22 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:22-jre-jammy

WORKDIR /app

COPY --from=build /app/target/app.jar session_service.jar

EXPOSE 21505

ENTRYPOINT ["java", "-jar", "session_service.jar"]