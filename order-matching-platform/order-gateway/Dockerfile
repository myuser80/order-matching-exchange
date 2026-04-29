FROM maven:3.9.5-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY common-lib/pom.xml common-lib/
COPY order-gateway/pom.xml order-gateway/
COPY matching-engine/pom.xml matching-engine/
COPY execution-service/pom.xml execution-service/
RUN mvn dependency:go-offline -pl common-lib,order-gateway -am -q

COPY common-lib/src common-lib/src
COPY order-gateway/src order-gateway/src
RUN mvn package -pl common-lib,order-gateway -am -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/order-gateway/target/order-gateway-1.0.0.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
