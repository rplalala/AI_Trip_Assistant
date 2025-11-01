FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn install -DskipTests -q

FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=build /app/target/*.jar /app/app.jar
ENV PORT=8092
EXPOSE 8092
ENTRYPOINT ["java","-jar","/app/app.jar"]