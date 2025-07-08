# 1) Etapa de compilación: usa la imagen oficial de Maven con Temurin 21
FROM maven:3.9.8-eclipse-temurin-21 AS build

WORKDIR /app

# Copia solo el POM para cachear dependencias
COPY pom.xml .

# Descarga todas las dependencias sin compilar aún
RUN mvn dependency:go-offline -B

# Copia el código fuente y compila el JAR sin tests
COPY src ./src
RUN mvn clean package -DskipTests -B

# 2) Etapa de runtime: empaqueta solamente el JAR resultante
FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copia el JAR generado
COPY --from=build /app/target/ms-imagenes-dyc-0.0.1-SNAPSHOT.jar app.jar

# Exponer el puerto de la app
EXPOSE 8082

# Arrancar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
