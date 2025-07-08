# 1) Etapa de compilación: usa la imagen oficial de Maven + Temurin 21
FROM maven:3.9.2-eclipse-temurin-21 AS build

# Directorio de trabajo
WORKDIR /app

# Copia el POM y descarga dependencias
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia el código fuente y construye el JAR sin tests
COPY src ./src
RUN mvn clean package -DskipTests -B

# 2) Etapa de runtime: empaqueta solo el JAR resultante
FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copia el JAR generado en la etapa de build
COPY --from=build /app/target/ms-imagenes-dyc-0.0.1-SNAPSHOT.jar app.jar

# Expone el puerto de tu aplicación
EXPOSE 8082

# Comando de arranque
ENTRYPOINT ["java", "-jar", "app.jar"]
