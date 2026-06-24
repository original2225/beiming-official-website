FROM eclipse-temurin:21-jre-alpine
WORKDIR /usr/share/beiming-frontend
COPY dist ./
COPY container/StaticFileServer.class ./StaticFileServer.class
EXPOSE 5173
HEALTHCHECK --interval=20s --timeout=5s --start-period=10s --retries=5 CMD ["wget", "-q", "-O", "-", "http://127.0.0.1:5173/"]
CMD ["java", "-cp", "/usr/share/beiming-frontend", "StaticFileServer"]
