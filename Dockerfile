# Build stage
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# Run stage
FROM eclipse-temurin:21
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Folder for H2 file database
RUN mkdir -p /app/data

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]