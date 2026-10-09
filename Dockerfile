# Etapa 1: Construccion (Builder)
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copiamos los archivos de configuracion de Gradle
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN chmod +x ./gradlew

# Copiamos el codigo fuente
COPY src ./src

# Construimos el proyecto omitiendo las pruebas
RUN ./gradlew build -x test

# Etapa 2: Ejecucion (Runtime)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copiamos el JAR generado desde la etapa 1
COPY --from=builder /app/build/libs/prueba-1.0.jar app.jar

EXPOSE 8080

# Limite de 300MB de RAM para el plan gratuito de Render
ENTRYPOINT ["java", "-Xmx300m", "-jar", "app.jar"]
