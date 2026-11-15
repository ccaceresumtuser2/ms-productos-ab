# ---------- Etapa 1: build ----------
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Descarga dependencias primero (capa cacheable)
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Compila (los tests ya corrieron en GitHub Actions)
COPY src src
RUN ./mvnw -B -q package -DskipTests

# ---------- Etapa 2: runtime ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=build /app/target/ms-productos-ab-*.jar app.jar

# Render inyecta PORT; la app lo lee con server.port=${PORT:8080}
ENV PORT=8080
EXPOSE 8080

# Ajusta la heap al límite de memoria del contenedor (plan free = 512 MB)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
