package com.cinephile.web;

import com.cinephile.dao.BookingDAO;
import com.cinephile.dao.DBConnection;
import com.cinephile.dao.MovieShowDAO;
import com.cinephile.dao.UserDAO;
import com.cinephile.exception.CinephileException;
import com.cinephile.model.*;
import com.cinephile.service.BookingCalculator;
import com.cinephile.service.PaymentFactory;
import com.cinephile.service.PaymentProcessor;
import com.cinephile.service.TicketFileService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Lightweight Core Java Web Server using built-in com.sun.net.httpserver.HttpServer.
 * Connects our aesthetic HTML/CSS/JS frontend directly to plain JDBC + MySQL 8
 * with zero heavy frameworks (no Spring Boot, no Hibernate, no Tomcat needed).
 */
public class CinephileWebServer {

    private static final int PORT = 8080;
    private static final Path WEB_ROOT = Paths.get("web").toAbsolutePath().normalize();

    private static final UserDAO userDAO = new UserDAO();
    private static final MovieShowDAO movieShowDAO = new MovieShowDAO();
    private static final BookingDAO bookingDAO = new BookingDAO();

    // Simple session store mapping session token -> authenticated UserAccount
    private static final Map<String, UserAccount> SESSIONS = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(Executors.newFixedThreadPool(8));

        // API Routes
        server.createContext("/api/health", ex -> handleSafe(ex, CinephileWebServer::handleHealth));
        server.createContext("/api/auth/login", ex -> handleSafe(ex, CinephileWebServer::handleLogin));
        server.createContext("/api/auth/register", ex -> handleSafe(ex, CinephileWebServer::handleRegister));
        server.createContext("/api/movies", ex -> handleSafe(ex, CinephileWebServer::handleMovies));
        server.createContext("/api/shows", ex -> handleSafe(ex, CinephileWebServer::handleShows));
        server.createContext("/api/bookings", ex -> handleSafe(ex, CinephileWebServer::handleBookings));
        server.createContext("/api/admin/", ex -> handleSafe(ex, CinephileWebServer::handleAdmin));

        // Static Web Pages (HTML, CSS, JS)
        server.createContext("/", ex -> handleSafe(ex, CinephileWebServer::handleStatic));

        server.start();
        String url = "http://localhost:" + PORT;
        System.out.println("==============================================================");
        System.out.println(" CINEPHILE Web Server started at: " + url);
        System.out.println(" MySQL Status: " + (DBConnection.testConnection() ? "CONNECTED" : "CHECK db.properties"));
        System.out.println(" Press Ctrl+C in this window to stop.");
        System.out.println("==============================================================");

        openBrowser(url);
    }

    @FunctionalInterface
    interface RouteHandler {
        void handle(HttpExchange ex) throws Exception;
    }

    private static void handleSafe(HttpExchange ex, RouteHandler handler) throws IOException {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return;
        }
        try {
            handler.handle(ex);
        } catch (CinephileException e) {
            sendJson(ex, 400, "{\"ok\":false,\"error\":" + JsonHelper.q(e.getMessage()) + "}");
        } catch (Exception e) {
            sendJson(ex, 500, "{\"ok\":false,\"error\":" + JsonHelper.q("Server error: " + e.getMessage()) + "}");
        }
    }

    // ------------------------------------------------------------------
    // Health Check
    // ------------------------------------------------------------------
    private static void handleHealth(HttpExchange ex) throws IOException {
        boolean connected = DBConnection.testConnection();
        sendJson(ex, 200, "{\"ok\":true,\"connected\":" + connected + ",\"user\":" + JsonHelper.q(DBConnection.getUser()) + "}");
    }

    // ------------------------------------------------------------------
    // Auth: Login & Register
    // ------------------------------------------------------------------
    private static void handleLogin(HttpExchange ex) throws Exception {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            sendJson(ex, 405, "{\"ok\":false,\"error\":\"Method not allowed\"}");
            return;
        }
        Map<String, String> body = JsonHelper.parseObject(readBody(ex));
        String role = body.getOrDefault("role", "Customer");
        String email = body.getOrDefault("email", "");
        String password = body.getOrDefault("password", "");

        UserAccount account = "Admin".equalsIgnoreCase(role)
                ? userDAO.authenticateAdmin(email, password)
                : userDAO.authenticateCustomer(email, password);

        String token = UUID.randomUUID().toString();
        SESSIONS.put(token, account);
        sendJson(ex, 200, "{\"ok\":true,\"data\":" + JsonHelper.userJson(account, token) + "}");
    }

    private static void handleRegister(HttpExchange ex) throws Exception {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            sendJson(ex, 405, "{\"ok\":false,\"error\":\"Method not allowed\"}");
            return;
        }
        Map<String, String> body = JsonHelper.parseObject(readBody(ex));
        String name = body.getOrDefault("name", "");
        String email = body.getOrDefault("email", "");
        String phone = body.getOrDefault("phone", "");
        String password = body.getOrDefault("password", "");
        if (name.trim().isEmpty() || email.trim().isEmpty() || password.trim().isEmpty()) {
            throw new CinephileException("Name, email, and password are required.");
        }
        Customer customer = userDAO.registerCustomer(name, email, password, phone);
        String token = UUID.randomUUID().toString();
        SESSIONS.put(token, customer);
        sendJson(ex, 201, "{\"ok\":true,\"data\":" + JsonHelper.userJson(customer, token) + "}");
    }

    // ------------------------------------------------------------------
    // Public Movies & Shows
    // ------------------------------------------------------------------
    private static void handleMovies(HttpExchange ex) throws Exception {
        Map<String, String> query = parseQuery(ex.getRequestURI().getRawQuery());
        List<Movie> all = movieShowDAO.getAllMovies();
        List<Movie> filtered = BookingCalculator.filterMovies(
                all, query.get("search"), query.get("genre"), query.get("language"));
        sendJson(ex, 200, "{\"ok\":true,\"data\":" + JsonHelper.moviesJson(filtered) + "}");
    }

    private static void handleShows(HttpExchange ex) throws Exception {
        String path = ex.getRequestURI().getPath();
        // /api/shows/{id}/seats
        if (path.matches("/api/shows/\\d+/seats")) {
            long showId = Long.parseLong(path.split("/")[3]);
            List<Seat> seats = movieShowDAO.getSeatsForShow(showId);
            sendJson(ex, 200, "{\"ok\":true,\"data\":" + JsonHelper.seatsJson(seats) + "}");
            return;
        }
        // /api/shows/{id}
        if (path.matches("/api/shows/\\d+")) {
            long showId = Long.parseLong(path.split("/")[3]);
            Showtime found = movieShowDAO.getShowtimesByMovie(null).stream()
                    .filter(s -> s.getShowId() == showId)
                    .findFirst()
                    .orElseThrow(() -> new CinephileException("Showtime not found: " + showId));
            sendJson(ex, 200, "{\"ok\":true,\"data\":" + JsonHelper.showsJson(Collections.singletonList(found)) + "}");
            return;
        }
        // /api/shows?movieId=...
        Map<String, String> query = parseQuery(ex.getRequestURI().getRawQuery());
        Long movieId = query.containsKey("movieId") && !query.get("movieId").isEmpty()
                ? Long.parseLong(query.get("movieId")) : null;
        List<Showtime> shows = movieShowDAO.getShowtimesByMovie(movieId);
        sendJson(ex, 200, "{\"ok\":true,\"data\":" + JsonHelper.showsJson(shows) + "}");
    }

    // ------------------------------------------------------------------
    // Customer Bookings
    // ------------------------------------------------------------------
    private static void handleBookings(HttpExchange ex) throws Exception {
        UserAccount user = requireSession(ex);
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();

        if ("GET".equalsIgnoreCase(method) && "/api/bookings/my".equals(path)) {
            List<BookingSummary> list = bookingDAO.getCustomerBookings(user.getId());
            sendJson(ex, 200, "{\"ok\":true,\"data\":" + bookingsArrayJson(list) + "}");
            return;
        }

        if ("GET".equalsIgnoreCase(method) && path.matches("/api/bookings/\\d+/ticket")) {
            long bookingId = Long.parseLong(path.split("/")[3]);
            BookingSummary b = bookingDAO.getBookingById(bookingId);
            if (!(user instanceof Admin) && b.getCustomerId() != user.getId()) {
                throw new CinephileException("Not authorized to access this ticket.");
            }
            List<String> labels = bookingDAO.getSeatLabelsForBooking(bookingId);
            File file = TicketFileService.exportTicket(b, labels);
            byte[] bytes = Files.readAllBytes(file.toPath());
            ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            ex.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
            ex.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes);
            }
            return;
        }

        if ("GET".equalsIgnoreCase(method) && path.matches("/api/bookings/\\d+")) {
            long bookingId = Long.parseLong(path.split("/")[3]);
            BookingSummary b = bookingDAO.getBookingById(bookingId);
            if (!(user instanceof Admin) && b.getCustomerId() != user.getId()) {
                throw new CinephileException("Not authorized to view this booking.");
            }
            List<String> labels = bookingDAO.getSeatLabelsForBooking(bookingId);
            sendJson(ex, 200, "{\"ok\":true,\"data\":" + JsonHelper.bookingJson(b, labels) + "}");
            return;
        }

        if ("POST".equalsIgnoreCase(method) && path.matches("/api/bookings/\\d+/cancel")) {
            long bookingId = Long.parseLong(path.split("/")[3]);
            BookingSummary b = bookingDAO.getBookingById(bookingId);
            if (!(user instanceof Admin) && b.getCustomerId() != user.getId()) {
                throw new CinephileException("Not authorized to cancel this booking.");
            }
            bookingDAO.cancelBooking(bookingId);
            sendJson(ex, 200, "{\"ok\":true,\"message\":\"Booking cancelled and seats released.\"}");
            return;
        }

        if ("POST".equalsIgnoreCase(method) && "/api/bookings".equals(path)) {
            if (!(user instanceof Customer)) {
                throw new CinephileException("Please sign in as a Customer to book seats.");
            }
            Map<String, String> body = JsonHelper.parseObject(readBody(ex));
            long showId = Long.parseLong(body.get("showId"));
            List<Long> requestedSeatIds = JsonHelper.parseLongArray(body.get("seatIds"));
            String payMethod = body.getOrDefault("paymentMethod", "UPI");

            List<Seat> allSeats = movieShowDAO.getSeatsForShow(showId);
            List<Seat> chosen = allSeats.stream()
                    .filter(s -> requestedSeatIds.contains(s.getSeatId()))
                    .collect(Collectors.toList());

            PaymentProcessor processor = PaymentFactory.getProcessor(payMethod);
            BookingSummary created = bookingDAO.createBooking(user.getId(), showId, chosen, processor);
            List<String> labels = bookingDAO.getSeatLabelsForBooking(created.getBookingId());
            TicketFileService.exportTicket(created, labels);

            sendJson(ex, 201, "{\"ok\":true,\"data\":" + JsonHelper.bookingJson(created, labels) + "}");
            return;
        }

        sendJson(ex, 404, "{\"ok\":false,\"error\":\"Unknown booking endpoint\"}");
    }

    private static String bookingsArrayJson(List<BookingSummary> list) throws CinephileException {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            BookingSummary b = list.get(i);
            sb.append(JsonHelper.bookingJson(b, bookingDAO.getSeatLabelsForBooking(b.getBookingId())));
        }
        return sb.append("]").toString();
    }

    // ------------------------------------------------------------------
    // Admin Operations (Requires Admin session)
    // ------------------------------------------------------------------
    private static void handleAdmin(HttpExchange ex) throws Exception {
        UserAccount user = requireSession(ex);
        if (!(user instanceof Admin)) {
            sendJson(ex, 403, "{\"ok\":false,\"error\":\"Administrator login required.\"}");
            return;
        }
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();

        if ("GET".equalsIgnoreCase(method) && "/api/admin/overview".equals(path)) {
            String json = "{"
                    + "\"movies\":" + JsonHelper.moviesJson(movieShowDAO.getAllMovies()) + ","
                    + "\"theatres\":" + JsonHelper.theatresJson(movieShowDAO.getAllTheatres()) + ","
                    + "\"screens\":" + JsonHelper.screensJson(movieShowDAO.getAllScreens()) + ","
                    + "\"shows\":" + JsonHelper.showsJson(movieShowDAO.getShowtimesByMovie(null)) + ","
                    + "\"bookings\":" + bookingsArrayJson(bookingDAO.getAllBookings())
                    + "}";
            sendJson(ex, 200, "{\"ok\":true,\"data\":" + json + "}");
            return;
        }

        if ("GET".equalsIgnoreCase(method) && "/api/admin/reports".equals(path)) {
            List<MovieReportRow> rows = bookingDAO.getAllMovieReportsViaProcedure();
            sendJson(ex, 200, "{\"ok\":true,\"data\":" + JsonHelper.reportsJson(rows) + "}");
            return;
        }

        if ("GET".equalsIgnoreCase(method) && path.matches("/api/admin/reports/\\d+")) {
            long movieId = Long.parseLong(path.split("/")[4]);
            String title = movieShowDAO.getAllMovies().stream()
                    .filter(m -> m.getMovieId() == movieId)
                    .map(Movie::getTitle).findFirst().orElse("Movie #" + movieId);
            String msg = bookingDAO.getMovieRevenueViaProcedure(movieId, title);
            sendJson(ex, 200, "{\"ok\":true,\"message\":" + JsonHelper.q(msg) + "}");
            return;
        }

        if ("GET".equalsIgnoreCase(method) && "/api/admin/audit-logs".equals(path)) {
            sendJson(ex, 200, "{\"ok\":true,\"data\":" + JsonHelper.auditLogsJson(bookingDAO.getAuditLogs()) + "}");
            return;
        }

        if ("POST".equalsIgnoreCase(method) && "/api/admin/movies".equals(path)) {
            Map<String, String> b = JsonHelper.parseObject(readBody(ex));
            movieShowDAO.addMovie(
                    b.get("title"), b.get("genre"), b.get("language"),
                    Integer.parseInt(b.get("durationMinutes")),
                    b.getOrDefault("description", "Added via Admin Console"),
                    LocalDate.parse(b.get("releaseDate"))
            );
            sendJson(ex, 201, "{\"ok\":true,\"message\":\"Movie added.\"}");
            return;
        }

        if ("DELETE".equalsIgnoreCase(method) && path.matches("/api/admin/movies/\\d+")) {
            long movieId = Long.parseLong(path.split("/")[4]);
            movieShowDAO.deleteMovie(movieId);
            sendJson(ex, 200, "{\"ok\":true,\"message\":\"Movie deleted.\"}");
            return;
        }

        if ("POST".equalsIgnoreCase(method) && "/api/admin/theatres".equals(path)) {
            Map<String, String> b = JsonHelper.parseObject(readBody(ex));
            movieShowDAO.addTheatre(b.get("name"), b.get("city"), b.get("address"));
            sendJson(ex, 201, "{\"ok\":true,\"message\":\"Theatre created.\"}");
            return;
        }

        if ("POST".equalsIgnoreCase(method) && "/api/admin/screens".equals(path)) {
            Map<String, String> b = JsonHelper.parseObject(readBody(ex));
            movieShowDAO.addScreenWithSeats(
                    Long.parseLong(b.get("theatreId")), b.get("name"),
                    Integer.parseInt(b.get("totalRows")), Integer.parseInt(b.get("totalCols")),
                    Double.parseDouble(b.getOrDefault("regularPrice", "150")),
                    Double.parseDouble(b.getOrDefault("premiumPrice", "220")),
                    Double.parseDouble(b.getOrDefault("vipPrice", "320"))
            );
            sendJson(ex, 201, "{\"ok\":true,\"message\":\"Screen created and seats generated.\"}");
            return;
        }

        if ("POST".equalsIgnoreCase(method) && "/api/admin/shows".equals(path)) {
            Map<String, String> b = JsonHelper.parseObject(readBody(ex));
            String timeStr = b.get("showTime");
            if (timeStr.length() == 5) timeStr += ":00";
            movieShowDAO.addShowtime(
                    Long.parseLong(b.get("movieId")),
                    Long.parseLong(b.get("screenId")),
                    LocalDate.parse(b.get("showDate")),
                    LocalTime.parse(timeStr)
            );
            sendJson(ex, 201, "{\"ok\":true,\"message\":\"Showtime scheduled.\"}");
            return;
        }

        if ("POST".equalsIgnoreCase(method) && path.matches("/api/admin/bookings/\\d+/cancel")) {
            long bookingId = Long.parseLong(path.split("/")[4]);
            bookingDAO.cancelBooking(bookingId);
            sendJson(ex, 200, "{\"ok\":true,\"message\":\"Booking cancelled by Admin.\"}");
            return;
        }

        sendJson(ex, 404, "{\"ok\":false,\"error\":\"Unknown admin endpoint\"}");
    }

    // ------------------------------------------------------------------
    // Static File Server (web/) with Byte-Range support for MP4 video
    // ------------------------------------------------------------------
    private static void handleStatic(HttpExchange ex) throws IOException {
        String rawPath = ex.getRequestURI().getPath();
        if ("/".equals(rawPath)) rawPath = "/index.html";
        Path target = WEB_ROOT.resolve(rawPath.substring(1)).normalize();
        if (!target.startsWith(WEB_ROOT) || !Files.exists(target) || Files.isDirectory(target)) {
            byte[] notFound = "<h1>404 Not Found</h1>".getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            ex.sendResponseHeaders(404, notFound.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(notFound); }
            return;
        }
        String contentType = guessContentType(target.toString());
        byte[] bytes = Files.readAllBytes(target);
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.getResponseHeaders().set("Accept-Ranges", "bytes");

        String rangeHeader = ex.getRequestHeaders().getFirst("Range");
        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String[] parts = rangeHeader.substring(6).split("-", 2);
            int start = parts[0].isEmpty() ? 0 : Integer.parseInt(parts[0]);
            int end = (parts.length > 1 && !parts[1].isEmpty()) ? Math.min(Integer.parseInt(parts[1]), bytes.length - 1) : bytes.length - 1;
            if (start < 0 || start > end || start >= bytes.length) {
                ex.getResponseHeaders().set("Content-Range", "bytes */" + bytes.length);
                ex.sendResponseHeaders(416, -1);
                return;
            }
            int len = end - start + 1;
            ex.getResponseHeaders().set("Content-Range", "bytes " + start + "-" + end + "/" + bytes.length);
            ex.sendResponseHeaders(206, len);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(bytes, start, len);
            }
            return;
        }

        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String guessContentType(String file) {
        if (file.endsWith(".html")) return "text/html; charset=utf-8";
        if (file.endsWith(".css")) return "text/css; charset=utf-8";
        if (file.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (file.endsWith(".mp4")) return "video/mp4";
        if (file.endsWith(".jpg") || file.endsWith(".jpeg")) return "image/jpeg";
        if (file.endsWith(".webp")) return "image/webp";
        if (file.endsWith(".svg")) return "image/svg+xml";
        if (file.endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }

    private static UserAccount requireSession(HttpExchange ex) throws CinephileException {
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7).trim();
            UserAccount acc = SESSIONS.get(token);
            if (acc != null) return acc;
        }
        throw new CinephileException("Please sign in to continue.");
    }

    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody();
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[4096];
            int n;
            while ((n = is.read(chunk)) != -1) {
                buffer.write(chunk, 0, n);
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static void sendJson(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null || raw.isEmpty()) return map;
        for (String pair : raw.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                try {
                    String k = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                    String v = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
                    map.put(k, v);
                } catch (UnsupportedEncodingException ignored) {
                }
            }
        }
        return map;
    }

    private static void openBrowser(String url) {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                new ProcessBuilder("cmd", "/c", "start", url).start();
            } else if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(url));
            }
        } catch (Exception ignored) {
        }
    }
}
