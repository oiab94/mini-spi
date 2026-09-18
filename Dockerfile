FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

COPY src ./src
RUN ./mvnw package -DskipTests -q

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

COPY --from=build /workspace/target/mini-spi-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
