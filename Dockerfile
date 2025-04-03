# Stage 1: Build the application
FROM maven:3.9.4-eclipse-temurin-17 as build

WORKDIR /app

# Copy pom and download dependencies first (for caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the rest and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy JAR from builder
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8099

# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]
