FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8280

ENTRYPOINT ["java","-jar","/app/app.jar"]