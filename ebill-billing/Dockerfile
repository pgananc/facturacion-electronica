# Imagen para ebill-billing (producción).
# El JAR se inyecta desde el job 'build' (artifacts) en la etapa docker del pipeline.
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# El pipeline copia source/target/*.jar en el contexto de build
COPY target/*.jar app.jar

EXPOSE 8480

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
