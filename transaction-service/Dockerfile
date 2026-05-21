# Stage 1: Build the application(a jar file)
FROM maven:3.9.6-eclipse-temurin-17 AS build
#base image

WORKDIR /app
# working directory

COPY pom.xml .
# copy package.json to app in different layer ass it does not change a lot

COPY src ./src
#copy from src from currecnt diretory to src

RUN mvn clean package -DskipTests
#manage dependency

# Stage 2: Run the application(create an image)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]