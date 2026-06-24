FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/unified-backend-service-0.1.0-SNAPSHOT.jar /app/unified-backend-service.jar
EXPOSE 8135
HEALTHCHECK --interval=20s --timeout=5s --start-period=40s --retries=5 CMD ["sh", "-c", "wget -q -O - http://127.0.0.1:8135/api/v1/unified-backend/health | grep -q '\"code\":0'"]
ENTRYPOINT ["java", "-jar", "/app/unified-backend-service.jar"]
