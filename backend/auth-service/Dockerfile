# ---------- Etapa 1: compilar ----------
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Primero solo el pom: así Docker guarda en caché las dependencias
COPY pom.xml .
RUN mvn -q dependency:go-offline

# Después el código, y se empaqueta (sin pruebas: necesitan la base levantada)
COPY src ./src
RUN mvn -q package -DskipTests

# ---------- Etapa 2: ejecutar ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]