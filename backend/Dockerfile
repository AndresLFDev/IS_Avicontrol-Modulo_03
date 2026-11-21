# Imagen de producción del backend (Spring Boot 4, Java 21).
# Construir desde la carpeta backend/:  docker build -t avicontrol-m3-backend .

# --- Etapa 1: compilar el JAR ejecutable ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src

# Se copia primero lo que cambia poco para aprovechar la caché de capas de Docker.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew

ENV GRADLE_OPTS="-Xmx768m"
RUN ./gradlew --no-daemon dependencies > /dev/null

COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# --- Etapa 2: imagen final, solo con el JRE ---
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app

COPY --from=build /src/build/libs/*.jar app.jar

# La configuración (base de datos, Kafka, CORS) llega por variables de entorno: ver application-prod.yml.
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
