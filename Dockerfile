# Use an official Maven image with OpenJDK 17
FROM maven:3.9-eclipse-temurin-17 AS build

# Set the working directory inside the container
WORKDIR /app

# Copy the pom.xml and source code
COPY pom.xml /app
COPY src /app/src

# Run Maven to build the project (this will create the target directory with the JAR)
RUN mvn clean package -DskipTests

# Use OpenJDK 17 to run the application
FROM eclipse-temurin:17-jre

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file from the build container to the runtime container
COPY --from=build /app/target/thelookingglass-1.0-SNAPSHOT.jar /app/thelookingglass-1.0-SNAPSHOT.jar

# Ensure the JAR file is executable
RUN chmod +x /app/thelookingglass-1.0-SNAPSHOT.jar

# Set the entrypoint to run the JAR file
ENTRYPOINT ["java", "-cp", "/app/thelookingglass-1.0-SNAPSHOT.jar", "com.example.TheLookingGlass"]


