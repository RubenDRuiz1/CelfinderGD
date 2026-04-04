# Etapa 1: Compilar la aplicación
FROM maven:3.9.6-eclipse-temurin-17 AS builder

# Establecer el directorio de trabajo
WORKDIR /app

# Copiar los archivos de configuración y fuente
COPY pom.xml ./
COPY src ./src

# Compilar la aplicación (sin tests para producción)
RUN mvn clean package -DskipTests

# Etapa 2: Crear la imagen final
FROM eclipse-temurin:17-jdk-jammy

# Establecer el directorio de trabajo
WORKDIR /app

# Copiar el JAR generado desde la etapa de compilación
COPY --from=builder /app/target/*.jar app.jar

# Exponer el puerto de la aplicación
EXPOSE 8080

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
