FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -q
COPY src ./src
RUN ./mvnw package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY --from=builder /app/target/*.jar anvesha-core.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "anvesha-core.jar", "--spring.profiles.active=docker"]
