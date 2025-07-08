FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY mvnw pom.xml .mvn/ ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src src
RUN ./mvnw clean package -DskipTests -B

FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=build /app/target/ms-imagenes-dyc-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app/app.jar"]
