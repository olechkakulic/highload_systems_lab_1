FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S shelter && adduser -S shelter -G shelter
WORKDIR /app
COPY --from=build /build/target/animal-shelter-1.0.0.jar app.jar
USER shelter
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
