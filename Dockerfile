# Multi-stage Docker build for Unified NeuroSys Monorepo (Spring Boot + React Frontend)

# Stage 1: Build React Frontend
FROM node:18-alpine AS frontend-build
WORKDIR /app/neurosys-frontend
COPY neurosys-frontend/package*.json ./
RUN npm install
COPY neurosys-frontend/ .
ENV VITE_API_BASE_URL=/api/v1
RUN npm run build

# Stage 2: Build Maven Backend with Embedded Frontend Static Resources
FROM maven:3.9.6-eclipse-temurin-17 AS backend-build
WORKDIR /app
COPY . .
# Copy built frontend dist files into Spring Boot static resources
COPY --from=frontend-build /app/neurosys-frontend/dist /app/neurosys-backend/src/main/resources/static
RUN mvn clean package -DskipTests -f neurosys-backend/pom.xml

# Stage 3: Production Runtime Container
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=backend-build /app/neurosys-backend/target/neurosys-backend-1.0.0-SNAPSHOT.jar app.jar

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

# Restrict JVM Heap to fit within Railway 512MB RAM Limit
ENTRYPOINT ["java", "-Xms128m", "-Xmx384m", "-XX:+UseG1GC", "-jar", "app.jar"]
