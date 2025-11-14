FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copy root pom.xml
COPY pom.xml .

# Copy all module pom.xml files
COPY personal-portal-core/pom.xml ./personal-portal-core/
COPY personal-portal-users/pom.xml ./personal-portal-users/
COPY personal-portal-content/pom.xml ./personal-portal-content/
COPY personal-portal-booking/pom.xml ./personal-portal-booking/
COPY personal-portal-application/pom.xml ./personal-portal-application/

# Copy Maven wrapper (if exists)
COPY mvnw* ./

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN apt-get update && apt-get install -y maven && \
    mvn dependency:go-offline -B || true

# Copy source code for all modules
COPY personal-portal-core/src ./personal-portal-core/src
COPY personal-portal-users/src ./personal-portal-users/src
COPY personal-portal-content/src ./personal-portal-content/src
COPY personal-portal-booking/src ./personal-portal-booking/src
COPY personal-portal-application/src ./personal-portal-application/src

# Build the application
RUN mvn clean package -DskipTests -B -pl personal-portal-application -am

# Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the built executable JAR from the application module
# Spring Boot plugin repackages the JAR and replaces the original with executable version
COPY --from=build /app/personal-portal-application/target/personal-portal-application-0.0.1-SNAPSHOT.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
