FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 10001 --no-create-home underart
COPY --from=build /app/target/*.jar app.jar
USER underart
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"
CMD ["java","-jar","app.jar"]
