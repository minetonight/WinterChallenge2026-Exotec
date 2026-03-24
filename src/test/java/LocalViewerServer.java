import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * Minimal local web server for Codingame match replays.
 *
 * <p>The stock runner only serves files that exist under this project's
 * {@code src/main/resources/view} folder. This repository contains only the
 * custom game resources, while the generic viewer shell lives in dependency
 * JARs. Serving static assets from the full classpath restores the local match
 * viewer without copying generated files into the repository.</p>
 */
public final class LocalViewerServer {
    private static final String DEFAULT_ENTRYPOINT = "test.html";
    private static final String GAME_JSON_PATH = "game.json";
    private static final String ASSETS_JS_PATH = "assets.js";
    private static final Map<String, String> CONTENT_TYPES = Map.ofEntries(
        Map.entry("css", "text/css; charset=utf-8"),
        Map.entry("gif", "image/gif"),
        Map.entry("html", "text/html; charset=utf-8"),
        Map.entry("jpeg", "image/jpeg"),
        Map.entry("jpg", "image/jpeg"),
        Map.entry("js", "text/javascript; charset=utf-8"),
        Map.entry("json", "application/json; charset=utf-8"),
        Map.entry("png", "image/png"),
        Map.entry("svg", "image/svg+xml"),
        Map.entry("txt", "text/plain; charset=utf-8")
    );

    private final HttpServer server;

    private LocalViewerServer(HttpServer server) {
        this.server = server;
    }

    public static void start(String gameJson, int port) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
            server.createContext("/", new ViewerHttpHandler(gameJson));
            server.setExecutor(Executors.newCachedThreadPool());
            LocalViewerServer localViewerServer = new LocalViewerServer(server);
            Runtime.getRuntime().addShutdownHook(new Thread(localViewerServer::stop));

            server.start();
            System.out.println("Viewer available at http://127.0.0.1:" + port + "/");

            new CountDownLatch(1).await();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to start local viewer server", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Local viewer server interrupted", exception);
        }
    }

    private void stop() {
        server.stop(0);
    }

    private static final class ViewerHttpHandler implements HttpHandler {
        private static final String GET = "GET";
        private static final String HEAD = "HEAD";

        private final byte[] gameJsonBytes;
        private final byte[] assetsJsBytes;
        private final ClassLoader classLoader;

        private ViewerHttpHandler(String gameJson) {
            this.gameJsonBytes = gameJson.getBytes(StandardCharsets.UTF_8);
            this.classLoader = LocalViewerServer.class.getClassLoader();
            this.assetsJsBytes = buildAssetsJs(classLoader).getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String method = exchange.getRequestMethod();
                if (!GET.equals(method) && !HEAD.equals(method)) {
                    sendStatus(exchange, 405);
                    return;
                }

                String normalizedPath = normalizePath(exchange.getRequestURI().getPath());
                if (normalizedPath == null) {
                    sendStatus(exchange, 400);
                    return;
                }

                if (normalizedPath.isEmpty()) {
                    normalizedPath = DEFAULT_ENTRYPOINT;
                }

                if (GAME_JSON_PATH.equals(normalizedPath) || "services/gameResult".equals(normalizedPath)) {
                    sendBytes(exchange, 200, "application/json; charset=utf-8", gameJsonBytes, HEAD.equals(method));
                    return;
                }

                if (ASSETS_JS_PATH.equals(normalizedPath)) {
                    sendBytes(exchange, 200, "text/javascript; charset=utf-8", assetsJsBytes, HEAD.equals(method));
                    return;
                }

                if ("services/save-replay".equals(normalizedPath)) {
                    sendStatus(exchange, 204);
                    return;
                }

                String resourcePath = "view/" + normalizedPath;
                try (InputStream inputStream = classLoader.getResourceAsStream(resourcePath)) {
                    if (inputStream == null) {
                        sendStatus(exchange, 404);
                        return;
                    }

                    byte[] content = inputStream.readAllBytes();
                    sendBytes(exchange, 200, contentTypeFor(normalizedPath), content, HEAD.equals(method));
                }
            } finally {
                exchange.close();
            }
        }

        private static String normalizePath(String rawPath) {
            String trimmedPath = rawPath == null ? "" : rawPath.trim();
            String pathWithoutLeadingSlash = trimmedPath.startsWith("/")
                ? trimmedPath.substring(1)
                : trimmedPath;

            if (pathWithoutLeadingSlash.contains("..") || pathWithoutLeadingSlash.contains("\\")) {
                return null;
            }

            return pathWithoutLeadingSlash;
        }

        private static String contentTypeFor(String path) {
            int extensionSeparator = path.lastIndexOf('.');
            if (extensionSeparator < 0 || extensionSeparator == path.length() - 1) {
                return "application/octet-stream";
            }

            String extension = path.substring(extensionSeparator + 1).toLowerCase(Locale.ROOT);
            return CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");
        }

        private static void sendStatus(HttpExchange exchange, int statusCode) throws IOException {
            exchange.sendResponseHeaders(statusCode, -1);
        }

        private static String buildAssetsJs(ClassLoader classLoader) {
            JsonObject assets = new JsonObject();
            JsonObject images = new JsonObject();
            JsonArray sprites = new JsonArray();
            JsonArray fonts = new JsonArray();

            assets.add("images", images);
            assets.add("sprites", sprites);
            assets.add("fonts", fonts);

            Path assetsDirectory = resolveAssetsDirectory(classLoader);
            if (assetsDirectory != null && Files.isDirectory(assetsDirectory)) {
                try (Stream<Path> files = Files.list(assetsDirectory).sorted()) {
                    files
                        .filter(Files::isRegularFile)
                        .forEach(path -> addAssetEntry(path, images, sprites, fonts));
                } catch (IOException exception) {
                    System.out.println("Unable to generate assets.js: " + exception.getMessage());
                }
            }

            return "export const assets = " + assets + ";\n";
        }

        private static Path resolveAssetsDirectory(ClassLoader classLoader) {
            URL assetsUrl = classLoader.getResource("view/assets");
            if (assetsUrl == null || !"file".equalsIgnoreCase(assetsUrl.getProtocol())) {
                return null;
            }

            try {
                return Path.of(assetsUrl.toURI());
            } catch (URISyntaxException exception) {
                System.out.println("Unable to resolve assets directory: " + exception.getMessage());
                return null;
            }
        }

        private static void addAssetEntry(Path path, JsonObject images, JsonArray sprites, JsonArray fonts) {
            String fileName = path.getFileName().toString();
            String lowerCaseName = fileName.toLowerCase(Locale.ROOT);
            String resourcePath = "./assets/" + fileName;

            if (lowerCaseName.endsWith(".json")) {
                sprites.add(resourcePath);
                return;
            }

            if (lowerCaseName.endsWith(".fnt")) {
                fonts.add(resourcePath);
                return;
            }

            images.addProperty(fileName, resourcePath);
        }

        private static void sendBytes(
            HttpExchange exchange,
            int statusCode,
            String contentType,
            byte[] content,
            boolean headersOnly
        ) throws IOException {
            Headers headers = exchange.getResponseHeaders();
            headers.set("Access-Control-Allow-Origin", "*");
            headers.set("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.set("Content-Type", contentType);

            if (headersOnly) {
                exchange.sendResponseHeaders(statusCode, -1);
                return;
            }

            exchange.sendResponseHeaders(statusCode, content.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(content);
            }
        }
    }
}