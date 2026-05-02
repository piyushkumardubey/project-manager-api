# Stage 1: Build
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /app

# Copy Maven Wrapper
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Give Execute Permission
RUN chmod +x mvnw

# Download Dependencies
RUN ./mvnw dependency:go-offline -B

# Copy Source Code
COPY src ./src

# Build Application
RUN ./mvnw clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy Generated JAR
COPY --from=build /app/target/*.jar app.jar

# Expose Spring Boot Port
EXPOSE 8080

# Run Application
ENTRYPOINT ["java", "-jar", "app.jar"]
