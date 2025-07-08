# 1) Etapa de compilación: construye el JAR con Maven
FROM eclipse-temurin:21-jdk AS build

# Directorio de trabajo
WORKDIR /app

# Copia sólo lo necesario para cachear dependencias
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

# Copia el código fuente y construye el paquete
COPY src src
RUN ./mvnw clean package -DskipTests -B

# 2) Etapa de runtime: empaqueta sólo el JAR resultante
FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copia el JAR de la etapa de build
COPY --from=build /app/target/ms-imagenes-dyc-0.0.1-SNAPSHOT.jar app.jar

# Exponer el puerto que usa tu aplicación
EXPOSE 8082

# Lanzar la aplicación
ENTRYPOINT ["java","-jar","/app/app.jar"]
