FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8380

ENTRYPOINT ["java","-jar","/app/app.jar"]