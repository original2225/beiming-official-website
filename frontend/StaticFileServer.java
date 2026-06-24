import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public class StaticFileServer {
  private static final Path ROOT = Path.of("/usr/share/beiming-frontend").toAbsolutePath().normalize();
  private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  public static void main(String[] args) throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 5173), 0);
    server.createContext("/", StaticFileServer::handle);
    server.start();
  }

  private static void handle(HttpExchange exchange) throws IOException {
    if (exchange.getRequestURI().getPath().startsWith("/api/")) {
      proxy(exchange);
      return;
    }
    serveStatic(exchange);
  }

  private static void proxy(HttpExchange exchange) throws IOException {
    try {
      HttpRequest.Builder builder = HttpRequest.newBuilder()
          .uri(URI.create("http://backend:8135" + exchange.getRequestURI()))
          .timeout(Duration.ofSeconds(15))
          .method(exchange.getRequestMethod(), HttpRequest.BodyPublishers.ofByteArray(exchange.getRequestBody().readAllBytes()));
      String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
      if (contentType != null) {
        builder.header("Content-Type", contentType);
      }
      String authorization = exchange.getRequestHeaders().getFirst("Authorization");
      if (authorization != null) {
        builder.header("Authorization", authorization);
      }
      HttpRequest request = builder.build();
      HttpResponse<byte[]> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
      response.headers().firstValue("content-type").ifPresent(value -> exchange.getResponseHeaders().set("Content-Type", value));
      send(exchange, response.statusCode(), response.body());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      send(exchange, 502, "backend proxy interrupted".getBytes());
    } catch (Exception e) {
      send(exchange, 502, "backend proxy failed".getBytes());
    }
  }

  private static void serveStatic(HttpExchange exchange) throws IOException {
    String requestPath = exchange.getRequestURI().getPath();
    Path file = ROOT.resolve(requestPath.substring(1)).normalize();
    if (!file.startsWith(ROOT) || !Files.exists(file) || Files.isDirectory(file)) {
      file = ROOT.resolve("index.html");
    }
    exchange.getResponseHeaders().set("Content-Type", contentType(file));
    send(exchange, 200, Files.readAllBytes(file));
  }

  private static String contentType(Path file) {
    String name = file.getFileName().toString();
    if (name.endsWith(".html")) return "text/html; charset=utf-8";
    if (name.endsWith(".js")) return "application/javascript; charset=utf-8";
    if (name.endsWith(".css")) return "text/css; charset=utf-8";
    if (name.endsWith(".png")) return "image/png";
    if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
    if (name.endsWith(".webp")) return "image/webp";
    if (name.endsWith(".woff")) return "font/woff";
    return "application/octet-stream";
  }

  private static void send(HttpExchange exchange, int status, byte[] body) throws IOException {
    exchange.sendResponseHeaders(status, body.length);
    try (OutputStream output = exchange.getResponseBody()) {
      output.write(body);
    }
  }
}
