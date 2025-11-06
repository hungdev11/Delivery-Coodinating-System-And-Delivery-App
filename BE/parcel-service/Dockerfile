
FROM maven:3.9-eclipse-temurin-22 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:22-jre-jammy

WORKDIR /app

COPY --from=build /app/target/app.jar parcel_service.jar

EXPOSE 21506

ENTRYPOINT ["java", "-jar", "parcel_service.jar"]
