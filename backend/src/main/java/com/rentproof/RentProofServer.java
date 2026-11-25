package com.rentproof;

import com.rentproof.dashboard.DashboardService;
import com.rentproof.dashboard.DashboardStats;
import com.rentproof.dashboard.PaymentRecordDTO;
import com.rentproof.dashboard.PropertyOverviewDTO;
import com.rentproof.dashboard.ReminderDTO;
import com.rentproof.database.DBConnection;
import com.rentproof.database.DatabaseInitializer;
import com.rentproof.auth.SessionManager;
import com.rentproof.landlord.AuthService;
import com.rentproof.landlord.Landlord;
import com.rentproof.payment.Payment;
import com.rentproof.payment.PaymentService;
import com.rentproof.property.Property;
import com.rentproof.property.PropertyService;
import com.rentproof.property.Room;
import com.rentproof.reminder.Reminder;
import com.rentproof.reminder.ReminderService;
import com.rentproof.tenant.Tenant;
import com.rentproof.tenant.TenantService;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RentProofServer - Unified Application & API Server for RentProof
 * 
 * Strict compliance with Core Java (Syllabus up to Lecture 42):
 * - Built using JDK standard com.sun.net.httpserver (No Spring Boot, No Servlets, No third-party frameworks)
 * - Single port (8080) for all modules (Auth, Property, Tenant, Payment, Reminder, Dashboard)
 * - Directly coordinates with JDBC DAOs and MySQL database (NO localStorage fake storage)
 * - Serves frontend static web assets (HTML, CSS, Vanilla JS, images)
 * - Comprehensive CORS support for local development
 * 
 * Team Integrations:
 * - Aditi Sharma : Landlord Authentication, Property & Room Management, Landlord Dashboard
 * - Shweta       : Tenant Management & Property/Room Allocation
 * - Aanvi        : Payment Recording, Tracking & Statistics
 * - Gungun       : Rent Due & Maintenance Reminders
 */
public class RentProofServer {

    public static final int PORT = 8080;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final ConcurrentHashMap<String, LoginAttempt> LOGIN_ATTEMPTS = new ConcurrentHashMap<>();

    // Service Layer Singletons
    private static final AuthService authService = new AuthService();
    private static final PropertyService propertyService = new PropertyService();
    private static final TenantService tenantService = new TenantService();
    private static final PaymentService paymentService = new PaymentService();
    private static final ReminderService reminderService = new ReminderService();
    private static final DashboardService dashboardService = new DashboardService();

    public static void main(String[] args) throws IOException {
        try {
            DatabaseInitializer.initializeDatabase();
        } catch (SQLException e) {
            throw new IOException("Failed to initialize the RentProof database.", e);
        }
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // ==========================================
        // 1. AUTHENTICATION ENDPOINTS
        // ==========================================
        createApiContext(server, "/api/auth/login", new AuthLoginHandler());
        createApiContext(server, "/api/auth/register", new AuthRegisterHandler());
        createApiContext(server, "/api/auth/logout", new AuthLogoutHandler());
        createApiContext(server, "/api/auth/session", new AuthSessionHandler());
        createApiContext(server, "/api/auth/csrf", new CsrfTokenHandler());

        // ==========================================
        // 2. PROPERTY & ROOM ENDPOINTS
        // ==========================================
        createApiContext(server, "/api/properties", new PropertiesHandler());
        createApiContext(server, "/api/properties/add", new AddPropertyHandler());
        createApiContext(server, "/api/properties/update", new UpdatePropertyHandler());
        createApiContext(server, "/api/properties/delete", new DeletePropertyHandler());

        createApiContext(server, "/api/rooms", new RoomsHandler());
        createApiContext(server, "/api/rooms/add", new AddRoomHandler());
        createApiContext(server, "/api/rooms/update", new UpdateRoomHandler());
        createApiContext(server, "/api/rooms/delete", new DeleteRoomHandler());

        // ==========================================
        // 3. TENANT ENDPOINTS
        // ==========================================
        createApiContext(server, "/api/tenants", new TenantsHandler());
        createApiContext(server, "/api/tenants/add", new AddTenantHandler());
        createApiContext(server, "/api/tenants/update", new UpdateTenantHandler());
        createApiContext(server, "/api/tenants/delete", new DeleteTenantHandler());
        createApiContext(server, "/api/tenants/assign", new AssignTenantHandler());

        // ==========================================
        // 4. PAYMENT ENDPOINTS
        // ==========================================
        createApiContext(server, "/api/payments", new PaymentsHandler());
        createApiContext(server, "/api/payments/stats", new PaymentStatsHandler());
        createApiContext(server, "/api/payments/add", new AddPaymentHandler());
        createApiContext(server, "/api/payments/update-status", new UpdatePaymentStatusHandler());
        createApiContext(server, "/api/payments/delete", new DeletePaymentHandler());

        // ==========================================
        // 5. REMINDER ENDPOINTS
        // ==========================================
        createApiContext(server, "/api/reminders", new RemindersHandler());
        createApiContext(server, "/api/reminders/stats", new ReminderStatsHandler());
        createApiContext(server, "/api/reminders/add", new AddReminderHandler());
        createApiContext(server, "/api/reminders/update", new UpdateReminderHandler());
        createApiContext(server, "/api/reminders/delete", new DeleteReminderHandler());
        createApiContext(server, "/api/reminders/toggle-status", new ToggleReminderStatusHandler());

        // ==========================================
        // 6. DASHBOARD SUMMARY ENDPOINT
        // ==========================================
        createApiContext(server, "/api/dashboard/summary", new DashboardSummaryHandler());

        // ==========================================
        // 7. STATIC WEB FILES (HTML, CSS, JS, IMAGES)
        // ==========================================
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null); // default executor
        boolean databaseConnected = DBConnection.testConnection();
        System.out.println("============================================================");
        System.out.println("   RENTPROOF UNIFIED CORE JAVA SERVER RUNNING ON PORT " + PORT);
        System.out.println("   MySQL Database Connection: "
                + (databaseConnected ? "OK" : "UNAVAILABLE - check protected DB configuration"));
        System.out.println("   Open in Browser: http://localhost:" + PORT + "/frontend/auth/login.html");
        System.out.println("   Dashboard URL  : http://localhost:" + PORT + "/frontend/dashboard/dashboard.html");
        System.out.println("============================================================");
        server.start();
    }

    private static void createApiContext(HttpServer server, String path, HttpHandler handler) {
        HttpContext context = server.createContext(path, handler);
        context.getFilters().add(new CsrfFilter());
    }

    // =========================================================================
    // 1. AUTHENTICATION HANDLERS
    // =========================================================================

    static class AuthLoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"success\":false,\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String body = readRequestBody(exchange);
                Map<String, String> data = parseBody(body);

                String email = data.getOrDefault("email", "").trim();
                String password = data.getOrDefault("password", "");

                LoginAttempt attempt = LOGIN_ATTEMPTS.computeIfAbsent(email.toLowerCase(), key -> new LoginAttempt());
                if (attempt.isLocked()) {
                    sendJsonResponse(exchange, 429, "{\"success\":false,\"error\":\"Too many attempts. Please try again later.\"}");
                    return;
                }

                Landlord landlord = authService.login(email, password);
                if (landlord != null) {
                    LOGIN_ATTEMPTS.remove(email.toLowerCase());
                    String token = SessionManager.createSession(landlord.getLandlordId(), landlord.getEmail());
                    exchange.getResponseHeaders().add("Set-Cookie",
                            "rentproof_session=" + token + "; Path=/; HttpOnly; SameSite=Strict; Max-Age=1800"
                                    + secureCookieAttribute());
                    String json = String.format(
                            "{\"success\":true,\"message\":\"Login successful\",\"landlord\":{\"landlordId\":%d,\"fullName\":\"%s\",\"email\":\"%s\",\"phone\":\"%s\"}}",
                            landlord.getLandlordId(), escapeJson(landlord.getFullName()),
                            escapeJson(landlord.getEmail()), escapeJson(landlord.getPhone())
                    );
                    sendJsonResponse(exchange, 200, json);
                } else {
                    attempt.recordFailure();
                    sendJsonResponse(exchange, 401, "{\"success\":false,\"error\":\"Invalid email or password.\"}");
                }
            } catch (Exception e) {
                System.err.println("[AuthLoginHandler] Login request failed: " + e.getClass().getSimpleName());
                sendJsonResponse(exchange, 500, "{\"success\":false,\"error\":\"Unable to complete login.\"}");
            }
        }
    }

    static class AuthRegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"success\":false,\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String body = readRequestBody(exchange);
                Map<String, String> data = parseBody(body);

                String fullName = data.getOrDefault("fullName", "").trim();
                String email = data.getOrDefault("email", "").trim();
                String phone = data.getOrDefault("phone", "").trim();
                String password = data.getOrDefault("password", "");
                String confirmPassword = data.getOrDefault("confirmPassword", "");

                Landlord landlord = authService.register(fullName, email, phone, password, confirmPassword);
                String json = String.format(
                        "{\"success\":true,\"message\":\"Account registered successfully\",\"landlord\":{\"landlordId\":%d,\"fullName\":\"%s\",\"email\":\"%s\",\"phone\":\"%s\"}}",
                        landlord.getLandlordId(), escapeJson(landlord.getFullName()),
                        escapeJson(landlord.getEmail()), escapeJson(landlord.getPhone())
                );
                sendJsonResponse(exchange, 200, json);
            } catch (IllegalArgumentException e) {
                sendSafeError(exchange, 400, e);
            } catch (SQLException e) {
                System.err.println("[AuthRegisterHandler] Registration database operation failed.");
                sendJsonResponse(exchange, 500, "{\"success\":false,\"error\":\"Unable to complete registration.\"}");
            } catch (Exception e) {
                System.err.println("[AuthRegisterHandler] Registration request failed: " + e.getClass().getSimpleName());
                sendJsonResponse(exchange, 500, "{\"success\":false,\"error\":\"Unable to complete registration.\"}");
            }
        }
    }

    static class AuthLogoutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"success\":false,\"error\":\"Method Not Allowed\"}");
                return;
            }
            SessionManager.invalidate(getSessionToken(exchange));
            exchange.getResponseHeaders().add("Set-Cookie",
                    "rentproof_session=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0"
                            + secureCookieAttribute());
            exchange.getResponseHeaders().add("Set-Cookie",
                    "rentproof_csrf=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0"
                            + secureCookieAttribute());
            sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Logged out\"}");
        }
    }

    static class CsrfTokenHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"success\":false,\"error\":\"Method Not Allowed\"}");
                return;
            }

            byte[] tokenBytes = new byte[32];
            SECURE_RANDOM.nextBytes(tokenBytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
            exchange.getResponseHeaders().add("Set-Cookie",
                    "rentproof_csrf=" + token + "; Path=/; HttpOnly; SameSite=Strict; Max-Age=1800"
                            + secureCookieAttribute());
            sendJsonResponse(exchange, 200, "{\"csrfToken\":\"" + token + "\"}");
        }
    }

    static class AuthSessionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"success\":false,\"error\":\"Method Not Allowed\"}");
                return;
            }
            Integer landlordId = getAuthenticatedLandlordId(exchange);
            if (landlordId == null) {
                sendJsonResponse(exchange, 401, "{\"success\":false,\"error\":\"Authentication required.\"}");
                return;
            }
            Landlord landlord = new com.rentproof.landlord.LandlordDAO().getLandlordById(landlordId);
            if (landlord == null) {
                sendJsonResponse(exchange, 401, "{\"success\":false,\"error\":\"Authentication required.\"}");
                return;
            }
            String json = String.format(
                    "{\"success\":true,\"landlord\":{\"landlordId\":%d,\"fullName\":\"%s\",\"email\":\"%s\",\"phone\":\"%s\"}}",
                    landlord.getLandlordId(), escapeJson(landlord.getFullName()),
                    escapeJson(landlord.getEmail()), escapeJson(landlord.getPhone()));
            sendJsonResponse(exchange, 200, json);
        }
    }

    // =========================================================================
    // 2. PROPERTY & ROOM HANDLERS
    // =========================================================================

    static class PropertiesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;
            if (!requireAuthentication(exchange)) return;

            try {
                String method = exchange.getRequestMethod();
                if ("GET".equalsIgnoreCase(method)) {
                    int landlordId = getAuthenticatedLandlordId(exchange);
                    ArrayList<Property> list = propertyService.getPropertiesByLandlord(landlordId);

                    // Build JSON with room occupancy calculation
                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < list.size(); i++) {
                        Property p = list.get(i);
                        if (i > 0) json.append(",");

                        // Query real room statistics for this property
                        int occupied = 0;
                        int vacant = 0;
                        ArrayList<Room> rooms = propertyService.getRoomsByProperty(p.getPropertyId());
                        for (Room r : rooms) {
                            if ("Occupied".equalsIgnoreCase(r.getRoomStatus())) occupied++;
                            else if ("Vacant".equalsIgnoreCase(r.getRoomStatus())) vacant++;
                        }
                        int total = (rooms.size() > 0) ? rooms.size() : p.getTotalRooms();

                        json.append(String.format(
                                "{\"propertyId\":%d,\"landlordId\":%d,\"propertyName\":\"%s\",\"propertyType\":\"%s\"," +
                                "\"address\":\"%s\",\"city\":\"%s\",\"state\":\"%s\",\"pincode\":\"%s\"," +
                                "\"totalRooms\":%d,\"occupiedRooms\":%d,\"vacantRooms\":%d,\"monthlyRent\":%.2f,\"description\":\"%s\"}",
                                p.getPropertyId(), p.getLandlordId(), escapeJson(p.getPropertyName()),
                                escapeJson(p.getPropertyType()), escapeJson(p.getAddress()),
                                escapeJson(p.getCity()), escapeJson(p.getState()), escapeJson(p.getPincode()),
                                total, occupied, vacant, p.getMonthlyRent(), escapeJson(p.getDescription())
                        ));
                    }
                    json.append("]");
                    sendJsonResponse(exchange, 200, json.toString());
                } else if ("POST".equalsIgnoreCase(method)) {
                    new AddPropertyHandler().handle(exchange);
                } else if ("PUT".equalsIgnoreCase(method)) {
                    new UpdatePropertyHandler().handle(exchange);
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    new DeletePropertyHandler().handle(exchange);
                } else {
                    sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                }
            } catch (Exception e) {
                sendSafeError(exchange, 500, e);
            }
        }
    }

    static class AddPropertyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;
            if (!requireAuthentication(exchange)) return;

            try {
                String body = readRequestBody(exchange);
                Map<String, String> data = parseBody(body);

                Property p = new Property();
                int landlordId = getAuthenticatedLandlordId(exchange);
                p.setLandlordId(landlordId);
                p.setPropertyName(data.getOrDefault("propertyName", "").trim());
                p.setPropertyType(data.getOrDefault("propertyType", "Apartment").trim());
                p.setAddress(data.getOrDefault("address", "").trim());
                p.setCity(data.getOrDefault("city", "").trim());
                p.setState(data.getOrDefault("state", "").trim());
                p.setPincode(data.getOrDefault("pincode", "").trim());
                p.setTotalRooms(parseIntSafe(data.get("totalRooms"), 1));
                p.setMonthlyRent(parseDoubleSafe(data.get("monthlyRent"), 0.0));
                p.setDescription(data.getOrDefault("description", ""));

                int id = propertyService.addProperty(p, landlordId);

                // Auto-create initial vacant rooms if specified
                if (id > 0 && p.getTotalRooms() > 0) {
                    for (int r = 1; r <= p.getTotalRooms(); r++) {
                        String rNum = "Room " + (r < 10 ? "0" + r : r);
                        Room newRoom = new Room(id, rNum, p.getMonthlyRent(), "Vacant");
                        try {
                            propertyService.addRoom(newRoom);
                        } catch (Exception ignored) {}
                    }
                }

                sendJsonResponse(exchange, 200, "{\"success\":true,\"propertyId\":" + id + ",\"message\":\"Property added successfully to MySQL\"}");
            } catch (IllegalArgumentException e) {
                sendSafeError(exchange, 400, e);
            } catch (SQLException e) {
                sendSafeError(exchange, 500, e);
            } catch (Exception e) {
                sendSafeError(exchange, 500, e);
            }
        }
    }

    static class UpdatePropertyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;
            if (!requireAuthentication(exchange)) return;

            try {
                String body = readRequestBody(exchange);
                Map<String, String> data = parseBody(body);

                int propId = parseIntSafe(data.get("propertyId"), 0);
                if (!landlordOwnsProperty(getAuthenticatedLandlordId(exchange), propId)) {
                    sendJsonResponse(exchange, 403, "{\"success\":false,\"error\":\"You are not authorized to modify this resource.\"}");
                    return;
                }
                if (propId <= 0) {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Valid propertyId is required.\"}");
                    return;
                }

                Property p = propertyService.getPropertyById(propId);
                if (p == null) {
                    sendJsonResponse(exchange, 404, "{\"success\":false,\"error\":\"Property not found in MySQL.\"}");
                    return;
                }
                int landlordId = getAuthenticatedLandlordId(exchange);
                if (!landlordOwnsProperty(landlordId, propId)) {
                    sendJsonResponse(exchange, 403, "{\"success\":false,\"error\":\"You are not authorized to modify this resource.\"}");
                    return;
                }
                p.setLandlordId(landlordId);
                if (data.containsKey("propertyName")) p.setPropertyName(data.get("propertyName").trim());
                if (data.containsKey("propertyType")) p.setPropertyType(data.get("propertyType").trim());
                if (data.containsKey("address")) p.setAddress(data.get("address").trim());
                if (data.containsKey("city")) p.setCity(data.get("city").trim());
                if (data.containsKey("state")) p.setState(data.get("state").trim());
                if (data.containsKey("pincode")) p.setPincode(data.get("pincode").trim());
                if (data.containsKey("totalRooms")) p.setTotalRooms(parseIntSafe(data.get("totalRooms"), p.getTotalRooms()));
                if (data.containsKey("monthlyRent")) p.setMonthlyRent(parseDoubleSafe(data.get("monthlyRent"), p.getMonthlyRent()));
                if (data.containsKey("description")) p.setDescription(data.get("description"));

                boolean ok = propertyService.updateProperty(p, landlordId);
                if (ok) {
                    sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Property updated successfully in MySQL\"}");
                } else {
                    sendJsonResponse(exchange, 500, "{\"success\":false,\"error\":\"Failed to update property in database.\"}");
                }
            } catch (Exception e) {
                sendSafeError(exchange, 400, e);
            }
        }
    }

    static class DeletePropertyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;
            if (!requireAuthentication(exchange)) return;

            try {
                String body = readRequestBody(exchange);
                Map<String, String> data = parseBody(body);

                int propId = parseIntSafe(data.get("propertyId"), 0);
                if (propId <= 0) {
                    String query = exchange.getRequestURI().getQuery();
                    Map<String, String> q = parseQueryParams(query);
                    propId = parseIntSafe(q.get("id"), 0);
                }

                if (propId <= 0) {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"propertyId is required for deletion.\"}");
                    return;
                }

                Property p = propertyService.getPropertyById(propId);
                if (p == null) {
                    sendJsonResponse(exchange, 404, "{\"success\":false,\"error\":\"Property not found.\"}");
                    return;
                }

                int landlordId = getAuthenticatedLandlordId(exchange);
                if (!landlordOwnsProperty(landlordId, propId)) {
                    sendJsonResponse(exchange, 403, "{\"success\":false,\"error\":\"You are not authorized to modify this resource.\"}");
                    return;
                }
                boolean ok = propertyService.deleteProperty(propId, landlordId);
                if (ok) {
                    sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Property deleted successfully from MySQL\"}");
                } else {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Cannot delete property: active rooms, tenants or payment records are linked to it.\"}");
                }
            } catch (Exception e) {
                sendSafeError(exchange, 500, e);
            }
        }
    }

    static class RoomsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;
            if (!requireAuthentication(exchange)) return;

            try {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> queryParams = parseQueryParams(query);
                int propertyId = parseIntSafe(queryParams.get("propertyId"), 0);
                boolean vacantOnly = "true".equalsIgnoreCase(queryParams.get("vacantOnly"));

                ArrayList<Room> rooms;
                if (propertyId > 0) {
                    if (!landlordOwnsProperty(getAuthenticatedLandlordId(exchange), propertyId)) {
                        sendJsonResponse(exchange, 403, "{\"success\":false,\"error\":\"You are not authorized to access this resource.\"}");
                        return;
                    }
                    rooms = vacantOnly ? propertyService.getVacantRoomsByProperty(propertyId)
                                       : propertyService.getRoomsByProperty(propertyId);
                } else {
                    rooms = propertyService.getAllRooms();
                    ArrayList<Room> ownedRooms = new ArrayList<>();
                    int landlordId = getAuthenticatedLandlordId(exchange);
                    for (Room room : rooms) {
                        if (landlordOwnsProperty(landlordId, room.getPropertyId())) {
                            ownedRooms.add(room);
                        }
                    }
                    rooms = ownedRooms;
                }

                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < rooms.size(); i++) {
                    if (i > 0) json.append(",");
                    Room r = rooms.get(i);
                    json.append(String.format(
                            "{\"roomId\":%d,\"propertyId\":%d,\"roomNumber\":\"%s\",\"monthlyRent\":%.2f,\"roomStatus\":\"%s\",\"propertyName\":\"%s\"}",
                            r.getRoomId(), r.getPropertyId(), escapeJson(r.getRoomNumber()),
                            r.getMonthlyRent(), escapeJson(r.getRoomStatus()),
                            escapeJson(r.getPropertyName() != null ? r.getPropertyName() : "")
                    ));
                }
                json.append("]");
                sendJsonResponse(exchange, 200, json.toString());
            } catch (Exception e) {
                sendSafeError(exchange, 500, e);
            }
        }
    }

    static class AddRoomHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;
            if (!requireAuthentication(exchange)) return;

            try {
                String body = readRequestBody(exchange);
                Map<String, String> data = parseBody(body);

                int propId = parseIntSafe(data.get("propertyId"), 0);
                int landlordId = getAuthenticatedLandlordId(exchange);
                if (propId <= 0 || !landlordOwnsProperty(landlordId, propId)) {
                    sendJsonResponse(exchange, 403, "{\"success\":false,\"error\":\"You are not authorized to modify this resource.\"}");
                    return;
                }
                String roomNum = data.getOrDefault("roomNumber", "").trim();
                double rent = parseDoubleSafe(data.get("monthlyRent"), 0.0);
                String status = data.getOrDefault("roomStatus", "Vacant").trim();

                Room r = new Room(propId, roomNum, rent, status);
                int id = propertyService.addRoom(r);
                sendJsonResponse(exchange, 200, "{\"success\":true,\"roomId\":" + id + ",\"message\":\"Room added successfully\"}");
            } catch (IllegalArgumentException e) {
                sendSafeError(exchange, 400, e);
            } catch (SQLException e) {
                sendSafeError(exchange, e.getErrorCode() == 1062 ? 409 : 500, e);
            } catch (Exception e) {
                sendSafeError(exchange, 500, e);
            }
        }
    }

    static class UpdateRoomHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;
            if (!requireAuthentication(exchange)) return;

            try {
                String body = readRequestBody(exchange);
                Map<String, String> data = parseBody(body);

                int roomId = parseIntSafe(data.get("roomId"), 0);
                Room r = propertyService.getRoomById(roomId);
                if (r == null) {
                    sendJsonResponse(exchange, 404, "{\"success\":false,\"error\":\"Room not found\"}");
                    return;
                }
                if (!landlordOwnsRoom(getAuthenticatedLandlordId(exchange), roomId)) {
                    sendJsonResponse(exchange, 403, "{\"success\":false,\"error\":\"You are not authorized to modify this resource.\"}");
                    return;
                }

                if (data.containsKey("roomNumber")) r.setRoomNumber(data.get("roomNumber").trim());
                if (data.containsKey("monthlyRent")) r.setMonthlyRent(parseDoubleSafe(data.get("monthlyRent"), r.getMonthlyRent()));
                if (data.containsKey("roomStatus")) r.setRoomStatus(data.get("roomStatus").trim());

                boolean ok = propertyService.updateRoom(r);
                if (ok) {
                    sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Room updated successfully\"}");
                } else {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Room update did not modify a record.\"}");
                }
            } catch (IllegalArgumentException e) {
                sendSafeError(exchange, 400, e);
            } catch (SQLException e) {
                sendSafeError(exchange, 500, e);
            } catch (Exception e) {
                sendSafeError(exchange, 500, e);
            }
        }
    }

    static class DeleteRoomHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;
            if (!requireAuthentication(exchange)) return;

            try {
                String body = readRequestBody(exchange);
                Map<String, String> data = parseBody(body);
                int roomId = parseIntSafe(data.get("roomId"), 0);
                if (!landlordOwnsRoom(getAuthenticatedLandlordId(exchange), roomId)) {
                    sendJsonResponse(exchange, 403, "{\"success\":false,\"error\":\"You are not authorized to modify this resource.\"}");
                    return;
                }

                boolean ok = propertyService.deleteRoom(roomId);
                if (ok) {
                    sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Room deleted successfully\"}");
                } else {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Cannot delete room: tenant or payment records are linked, or the room ID is invalid.\"}");
                }
            } catch (Exception e) {
                sendSafeError(exchange, 500, e);
            }
        }
    }

    // =========================================================================
    // 3. TENANT HANDLERS
    // =========================================================================

    static class TenantsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;
            if (!requireAuthentication(exchange)) return;

            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            // Extract path parameter if any, e.g. /api/tenants/3
            String[] segments = path.split("/");
            int pathId = 0;
            if (segments.length > 3) {
                pathId = parseIntSafe(segments[3], 0);
            }

            try {
                if ("GET".equalsIgnoreCase(method)) {
                    if (pathId > 0) {
                        Tenant t = tenantService.getTenantById(pathId);
                        if (t != null) {
                            if (!landlordOwnsTenant(getAuthenticatedLandlordId(exchange), pathId)) {
                                sendJsonResponse(exchange, 403, "{\"success\":false,\"error\":\"You are not authorized to access this resource.\"}");
                                return;
                            }
                            sendJsonResponse(exchange, 200, tenantToJson(t));
                        } else {
                            sendJsonResponse(exchange, 404, "{\"success\":false,\"error\":\"Tenant not found\"}");
                        }
                        return;
                    }

                    Map<String, String> q = parseQueryParams(exchange.getRequestURI().getQuery());
                    int propertyId = parseIntSafe(q.get("propertyId"), 0);
                    String status = q.get("status");
                    String search = q.get("search");

                    ArrayList<Tenant> list;
                    if (propertyId > 0) {
                        if (!landlordOwnsProperty(getAuthenticatedLandlordId(exchange), propertyId)) {
                            sendJsonResponse(exchange, 403, "{\"success\":false,\"error\":\"You are not authorized to access this resource.\"}");
                            return;
                        }
                        list = tenantService.getTenantsByProperty(propertyId);
                    } else if (status != null && !status.isEmpty() && !"All".equalsIgnoreCase(status)) {
                        list = tenantService.getTenantsByStatus(status);
                    } else if (search != null && !search.isEmpty()) {
                        list = tenantService.searchTenants(search);
                    } else {
                        list = tenantService.getAllTenants();
                    }

                    int authenticatedLandlordId = getAuthenticatedLandlordId(exchange);
                    ArrayList<Tenant> ownedTenants = new ArrayList<>();
                    for (Tenant tenant : list) {
                        if (landlordOwnsTenant(authenticatedLandlordId, tenant.getTenantId())) {
                            ownedTenants.add(tenant);
                        }
                    }
                    list = ownedTenants;

                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < list.size(); i++) {
                        if (i > 0) json.append(",");
                        json.append(tenantToJson(list.get(i)));
                    }
                    json.append("]");
                    sendJsonResponse(exchange, 200, json.toString());
                } else if ("POST".equalsIgnoreCase(method)) {
                    new AddTenantHandler().handle(exchange);
                } else if ("PUT".equalsIgnoreCase(method)) {
                    new UpdateTenantHandler().handle(exchange);
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    new DeleteTenantHandler().handle(exchange);
                } else {
                    sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                }
            } catch (Exception e) {
                sendSafeError(exchange, 500, e);
            }
        }
    }

    static class AddTenantHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;
            if (!requireAuthentication(exchange)) return;

            try {
                String body = readRequestBody(exchange);
                Map<String, String> data = parseBody(body);

                Tenant t = new Tenant();
                t.setPropertyId(parseIntSafe(data.get("propertyId"), 0));
                int landlordId = getAuthenticatedLandlordId(exchange);
                if (!landlordOwnsProperty(landlordId, t.getPropertyId())) {
                    sendJsonResponse(exchange, 403, "{\"success\":false,\"error\":\"You are not authorized to use this property.\"}");
                    return;
                }
                t.setRoomId(parseIntSafe(data.get("roomId"), 0));
                if (!roomBelongsToProperty(landlordId, t.getRoomId(), t.getPropertyId())) {
                    sendJsonResponse(exchange, 403, "{\"success\":false,\"error\":\"The selected room does not belong to this property.\"}");
                    return;
                }
                t.setFullName(data.getOrDefault("fullName", "").trim());
                t.setEmail(data.getOrDefault("email", "").trim());
                t.setPhone(data.getOrDefault("phone", "").trim());
                t.setEmergencyContact(data.getOrDefault("emergencyContact", "").trim());
                t.setIdProofNumber(data.getOrDefault("idProofNumber", data.getOrDefault("permanentAddress", "")).trim());
                t.setSecurityDeposit(parseDoubleSafe(data.get("securityDeposit"), 0.0));
                t.setTenantStatus(data.getOrDefault("tenantStatus", "Active").trim());

                String moveInStr = data.get("moveInDate");
                if (moveInStr != null && !moveInStr.isEmpty()) {
                    try {
                        t.setMoveInDate(Date.valueOf(moveInStr.trim()));
                    } catch (Exception ignored) {
                        t.setMoveInDate(new Date(System.currentTimeMillis()));
                    }
                } else {
                    t.setMoveInDate(new Date(System.currentTimeMillis()));
                }

                int generatedId = tenantService.registerTenant(t);
                t.setTenantId(generatedId);

                sendJsonResponse(exchange, 200, "{\"success\":true,\"tenantId\":" + generatedId + ",\"tenant\":" + tenantToJson(t) + ",\"message\":\"Tenant registered successfully in MySQL\"}");
            } catch (IllegalArgumentException e) {
                sendSafeError(exchange, 400, e);
            } catch (SQLException e) {
                sendSafeError(exchange, 500, e);
            } catch (Exception e) {
                sendSafeError(exchange, 500, e);
            }
        }
    }

    static class UpdateTenantHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;
            if (!requireAuthentication(exchange)) return;

            try {
                String body = readRequestBody(exchange);
                Map<String, String> data = parseBody(body);

                int tenantId = parseIntSafe(data.get("tenantId"), 0);
                if (tenantId <= 0) {
                    String path = exchange.getRequestURI().getPath();
                    String[] segs = path.split("/");
                    if (segs.length > 3) tenantId = parseIntSafe(segs[3], 0);
                }

                Tenant t = tenantService.getTenantById(tenantId);
                if (t == null) {
                    sendJsonResponse(exchange, 404, "{\"success\":false,\"error\":\"Tenant not found in MySQL\"}");
                    return;
                }
                if (!landlordOwnsTenant(getAuthenticatedLandlordId(exchange), tenantId)) {
                    sendJsonResponse(exchange, 403, "{\"success\":false,\"error\":\"You are not authorized to modify this resource.\"}");
                    return;
                }

                int landlordId = getAuthenticatedLandlordId(exchange);
                int targetPropertyId = data.containsKey("propertyId")
                        ? parseIntSafe(data.get("propertyId"), 0) : t.getPropertyId();
                int targetRoomId = data.containsKey("roomId")
                        ? parseIntSafe(data.get("roomId"), 0) : t.getRoomId();
                if ((data.containsKey("propertyId") || data.containsKey("roomId"))
                        && (!landlordOwnsProperty(landlordId, targetPropertyId)
                        || (targetRoomId > 0 && !roomBelongsToProperty(landlordId, targetRoomId, targetPropertyId)))) {
                    sendJsonResponse(exchange, 403, "{\"success\":false,\"error\":\"You are not authorized to assign this resource.\"}");
                    return;
                }

                if (data.containsKey("fullName")) t.setFullName(data.get("fullName").trim());
                if (data.containsKey("email")) t.setEmail(data.get("email").trim());
                if (data.containsKey("phone")) t.setPhone(data.get("phone").trim());
                if (data.containsKey("emergencyContact")) t.setEmergencyContact(data.get("emergencyContact").trim());
                if (data.containsKey("idProofNumber")) t.setIdProofNumber(data.get("idProofNumber").trim());
                if (data.containsKey("securityDeposit")) t.setSecurityDeposit(parseDoubleSafe(data.get("securityDeposit"), t.getSecurityDeposit()));
                if (data.containsKey("tenantStatus")) t.setTenantStatus(data.get("tenantStatus").trim());
                if (data.containsKey("propertyId")) t.setPropertyId(parseIntSafe(data.get("propertyId"), t.getPropertyId()));
                if (data.containsKey("roomId")) t.setRoomId(parseIntSafe(data.get("roomId"), t.getRoomId()));

                boolean ok = tenantService.updateTenant(t);
                sendJsonResponse(exchange, 200, "{\"success\":" + ok + ",\"message\":\"Tenant updated successfully in MySQL\",\"tenant\":" + tenantToJson(t) + "}");
            } catch (Exception e) {
                sendSafeError(exchange, 400, e);
            }
        }
    }

    static class DeleteTenantHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;
            if (!requireAuthentication(exchange)) return;

            try {
                String body = readRequestBody(exchange);
                Map<String, String> data = parseBody(body);

                int tenantId = parseIntSafe(data.get("tenantId"), 0);
                if (tenantId <= 0) {
                    String path = exchange.getRequestURI().getPath();
                    String[] segs = path.split("/");
                    if (segs.length > 3) tenantId = parseIntSafe(segs[3], 0);
                }
                if (!landlordOwnsTenant(getAuthenticatedLandlordId(exchange), tenantId)) {
                    sendJsonResponse(exchange, 403, "{\"success\":false,\"error\":\"You are not authorized to modify this resource.\"}");
                    return;
                }

                if (tenantId <= 0) {
                    String query = exchange.getRequestURI().getQuery();
                    Map<String, String> q = parseQueryParams(query);
                    tenantId = parseIntSafe(q.get("id"), 0);
                }

                boolean ok = tenantService.deleteTenant(tenantId);
                sendJsonResponse(exchange, 200, "{\"success\":" + ok + ",\"message\":\"Tenant removed successfully from MySQL. Assigned room is now vacant.\"}");
            } catch (Exception e) {
                sendSafeError(exchange, 500, e);
            }
        }
    }

    static class AssignTenantHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;
            if (!requireAuthentication(exchange)) return;

            try {
                String body = readRequestBody(exchange);
                Map<String, String> data = parseBody(body);

                int tenantId = parseIntSafe(data.get("tenantId"), 0);
                int propertyId = parseIntSafe(data.get("propertyId"), 0);
                int roomId = parseIntSafe(data.get("roomId"), 0);
                int landlordId = getAuthenticatedLandlordId(exchange);
                if (!landlordOwnsTenant(landlordId, tenantId)
                        || !landlordOwnsProperty(landlordId, propertyId)
                        || !landlordOwnsRoom(landlordId, roomId)
                        || !roomBelongsToProperty(landlordId, roomId, propertyId)) {
                    sendJsonResponse(exchange, 403, "{\"success\":false,\"error\":\"You are not authorized to access these resources.\"}");
                    return;
                }
                double monthlyRent = parseDoubleSafe(data.get("monthlyRent"), 0.0);

                Date moveInDate = new Date(System.currentTimeMillis());
                if (data.containsKey("moveInDate") && !data.get("moveInDate").isEmpty()) {
                    try {
                        moveInDate = Date.valueOf(data.get("moveInDate").trim());
                    } catch (Exception ignored) {}
                }

                boolean ok = tenantService.assignProperty(tenantId, propertyId, roomId, monthlyRent, moveInDate);
                if (ok) {
                    sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Property assigned successfully to tenant in MySQL\"}");
                } else {
                    sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Failed to assign property/room in database.\"}");
                }
            } catch (Exception e) {
                sendSafeError(exchange, 500, e);
            }
        }
    }

    // =========================================================================
    // 4. PAYMENT HANDLERS
    // =========================================================================

    static class PaymentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;
            if (!requireAuthentication(exchange)) return;

            try {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> queryParams = parseQueryParams(query);

                String keyword = queryParams.get("search");
                String tenantStr = queryParams.get("tenantId");
                Integer tenantId = (tenantStr != null && !tenantStr.isEmpty()) ? parseIntSafe(tenantStr, 0) : null;
                if (tenantId != null && tenantId <= 0) tenantId = null;
                String month = queryParams.get("month");
                String status = queryParams.get("status");

                ArrayList<Payment> payments = paymentService.searchAndFilterPayments(keyword, tenantId, month, status);

                int landlordId = getAuthenticatedLandlordId(exchange);
                ArrayList<Payment> ownedPayments = new ArrayList<>();
                for (Payment payment : payments) {
                    if (landlordOwnsPayment(landlordId, payment.getPaymentId())) {
                        ownedPayments.add(payment);
                    }
                }
                payments = ownedPayments;

                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < payments.size(); i++) {
                    if (i > 0) json.append(",");
                    json.append(paymentToJson(payments.get(i)));
                }
                json.append("]");
                sendJsonResponse(exchange, 200, json.toString());
            } catch (Exception e) {
                sendSafeError(exchange, 500, e);
            }
        }
    }

    static class PaymentStatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;
            if (!requireAuthentication(exchange)) return;

            try {
                Map<String, Object> stats = paymentService.getDashboardStats(getAuthenticatedLandlordId(exchange));
                String json = String.format(
                        "{\"totalCollected\":%.2f,\"successful\":%d,\"pending\":%d,\"overdue\":%d,\"overdueAmount\":%.2f}",
                        ((Number) stats.get("totalRentCollected")).doubleValue(),
                        ((Number) stats.get("successfulPaymentCount")).intValue(),
                        ((Number) stats.get("pendingPaymentCount")).intValue(),
                        ((Number) stats.get("overduePaymentCount")).intValue(),
                        ((Number) stats.get("overdueAmount")).doubleValue()
                );
                sendJsonResponse(exchange, 200, json);
            } catch (Exception e) {
                sendSafeError(exchange, 500, e);
            }
        }
    }

    static class AddPaymentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;
            if (!requireAuthentication(exchange)) return;

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String body = readRequestBody(exchange);
                Map<String, String> data = parseBody(body);

                Payment p = new Payment();
                p.setTenantId(parseIntSafe(data.get("tenantId"), 0));
                p.setPropertyId(parseIntSafe(data.get("propertyId"), 0));
                p.setRoomId(parseIntSafe(data.get("roomId"), 0));
                p.setAmount(parseDoubleSafe(data.get("amount"), 0.0));
                p.setPaymentDate(data.getOrDefault("paymentDate", ""));
                p.setPaymentMonth(data.getOrDefault("paymentMonth", ""));
                p.setPaymentMode(data.getOrDefault("paymentMode", "Cash"));
                p.setTransactionRef(data.getOrDefault("transactionRef", ""));
                p.setPaymentStatus(data.getOrDefault("paymentStatus", "Paid"));
                p.setRemarks(data.getOrDefault("remarks", ""));

                int landlordId = getAuthenticatedLandlordId(exchange);
                if (!landlordOwnsTenant(landlordId, p.getTenantId())
                        || !landlordOwnsProperty(landlordId, p.getPropertyId())
                        || !landlordOwnsRoom(landlordId, p.getRoomId())) {
                    sendJsonResponse(exchange, 403, "{\"success\":false,\"error\":\"You are not authorized to use these resources.\"}");
                    return;
                }

                int id = paymentService.recordPayment(p);
                sendJsonResponse(exchange, 200, "{\"success\":true,\"paymentId\":" + id + ",\"message\":\"Payment recorded successfully in MySQL\"}");
            } catch (Exception e) {
                sendSafeError(exchange, 400, e);
            }
        }
    }

    static class UpdatePaymentStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;
            if (!requireAuthentication(exchange)) return;

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String body = readRequestBody(exchange);
                Map<String, String> data = parseBody(body);

                int paymentId = parseIntSafe(data.get("paymentId"), 0);
                if (!landlordOwnsPayment(getAuthenticatedLandlordId(exchange), paymentId)) {
                    sendJsonResponse(exchange, 403, "{\"success\":false,\"error\":\"You are not authorized to modify this resource.\"}");
                    return;
                }
                String newStatus = data.getOrDefault("status", "").trim();

                boolean ok = paymentService.updatePaymentStatus(paymentId, newStatus);
                sendJsonResponse(exchange, 200, "{\"success\":" + ok + ",\"message\":\"Payment status updated in MySQL\"}");
            } catch (Exception e) {
                sendSafeError(exchange, 400, e);
            }
        }
    }

    static class DeletePaymentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;
            if (!requireAuthentication(exchange)) return;

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String body = readRequestBody(exchange);
                Map<String, String> data = parseBody(body);

                int paymentId = parseIntSafe(data.get("paymentId"), 0);
                if (!landlordOwnsPayment(getAuthenticatedLandlordId(exchange), paymentId)) {
                    sendJsonResponse(exchange, 403, "{\"success\":false,\"error\":\"You are not authorized to modify this resource.\"}");
                    return;
                }
                boolean ok = paymentService.deletePayment(paymentId);
                sendJsonResponse(exchange, 200, "{\"success\":" + ok + ",\"message\":\"Payment deleted successfully from MySQL\"}");
            } catch (Exception e) {
                sendSafeError(exchange, 400, e);
            }
        }
    }

    // =========================================================================
    // 5. REMINDER HANDLERS
    // =========================================================================

    static class RemindersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;
            if (!requireAuthentication(exchange)) return;

            try {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> q = parseQueryParams(query);

                int landlordId = getAuthenticatedLandlordId(exchange);
                String status = q.get("status");
                String type = q.get("type");
                String keyword = q.get("search");

                ArrayList<Reminder> list;
                if (keyword != null && !keyword.trim().isEmpty()) {
                    list = reminderService.searchReminders(landlordId, keyword.trim());
                } else if ("Pending".equalsIgnoreCase(status)) {
                    list = reminderService.getPendingReminders(landlordId);
                } else if ("Completed".equalsIgnoreCase(status)) {
                    list = reminderService.getCompletedReminders(landlordId);
                } else if ("Overdue".equalsIgnoreCase(status)) {
                    list = reminderService.getOverdueReminders(landlordId);
                } else if ("Maintenance".equalsIgnoreCase(type)) {
                    list = reminderService.getMaintenanceReminders(landlordId);
                } else {
                    list = reminderService.getRemindersByLandlord(landlordId);
                }

                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) json.append(",");
                    json.append(reminderToJson(list.get(i)));
                }
                json.append("]");
                sendJsonResponse(exchange, 200, json.toString());
            } catch (Exception e) {
                sendSafeError(exchange, 500, e);
            }
        }
    }

    static class ReminderStatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;
            if (!requireAuthentication(exchange)) return;

            try {
                int landlordId = getAuthenticatedLandlordId(exchange);

                Map<String, Integer> stats = reminderService.calculateReminderStats(landlordId);
                StringBuilder json = new StringBuilder("{");
                json.append("\"total\":").append(stats.getOrDefault("total", 0)).append(",");
                json.append("\"pending\":").append(stats.getOrDefault("pending", 0)).append(",");
                json.append("\"completed\":").append(stats.getOrDefault("completed", 0)).append(",");
                json.append("\"upcoming\":").append(stats.getOrDefault("upcoming", 0)).append(",");
                json.append("\"overdue\":").append(stats.getOrDefault("overdue", 0));
                json.append("}");
                sendJsonResponse(exchange, 200, json.toString());
            } catch (Exception e) {
                sendSafeError(exchange, 500, e);
            }
        }
    }

    static class AddReminderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;
            if (!requireAuthentication(exchange)) return;

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String body = readRequestBody(exchange);
                Map<String, String> data = parseBody(body);

                Reminder r = new Reminder();
                r.setLandlordId(getAuthenticatedLandlordId(exchange));
                int tenantId = parseIntSafe(data.get("tenantId"), 0);
                int landlordId = getAuthenticatedLandlordId(exchange);
                if (tenantId > 0 && !landlordOwnsTenant(landlordId, tenantId)) {
                    sendJsonResponse(exchange, 403, "{\"success\":false,\"error\":\"You are not authorized to use this tenant.\"}");
                    return;
                }
                r.setTenantId(tenantId > 0 ? tenantId : null);
                r.setTitle(data.getOrDefault("title", "").trim());
                r.setMessage(data.getOrDefault("message", "").trim());
                r.setReminderType(data.getOrDefault("reminderType", "General").trim());
                r.setReminderStatus(data.getOrDefault("reminderStatus", "Pending").trim());

                String dueStr = data.get("dueDate");
                if (dueStr != null && !dueStr.trim().isEmpty()) {
                    r.setDueDate(Date.valueOf(dueStr.trim()));
                } else {
                    r.setDueDate(new Date(System.currentTimeMillis()));
                }

                int id = reminderService.createReminder(r);
                sendJsonResponse(exchange, 200, "{\"success\":true,\"reminderId\":" + id + ",\"message\":\"Reminder saved successfully in MySQL\"}");
            } catch (Exception e) {
                sendSafeError(exchange, 400, e);
            }
        }
    }

    static class UpdateReminderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;
            if (!requireAuthentication(exchange)) return;

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String body = readRequestBody(exchange);
                Map<String, String> data = parseBody(body);

                int reminderId = parseIntSafe(data.get("reminderId"), 0);
                Reminder r = reminderService.getReminderById(reminderId);
                if (r == null) {
                    sendJsonResponse(exchange, 404, "{\"success\":false,\"error\":\"Reminder not found in MySQL\"}");
                    return;
                }
                if (!landlordOwnsReminder(getAuthenticatedLandlordId(exchange), reminderId)) {
                    sendJsonResponse(exchange, 403, "{\"success\":false,\"error\":\"You are not authorized to modify this resource.\"}");
                    return;
                }

                if (data.containsKey("title")) r.setTitle(data.get("title").trim());
                if (data.containsKey("message")) r.setMessage(data.get("message").trim());
                if (data.containsKey("reminderType")) r.setReminderType(data.get("reminderType").trim());
                if (data.containsKey("reminderStatus")) r.setReminderStatus(data.get("reminderStatus").trim());
                if (data.containsKey("dueDate") && !data.get("dueDate").trim().isEmpty()) {
                    r.setDueDate(Date.valueOf(data.get("dueDate").trim()));
                }
                if (data.containsKey("tenantId")) {
                    int tid = parseIntSafe(data.get("tenantId"), 0);
                    r.setTenantId(tid > 0 ? tid : null);
                }

                boolean ok = reminderService.updateReminder(r);
                sendJsonResponse(exchange, 200, "{\"success\":" + ok + ",\"message\":\"Reminder updated successfully in MySQL\"}");
            } catch (Exception e) {
                sendSafeError(exchange, 400, e);
            }
        }
    }

    static class DeleteReminderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;
            if (!requireAuthentication(exchange)) return;

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String body = readRequestBody(exchange);
                Map<String, String> data = parseBody(body);

                int reminderId = parseIntSafe(data.get("reminderId"), 0);
                if (!landlordOwnsReminder(getAuthenticatedLandlordId(exchange), reminderId)) {
                    sendJsonResponse(exchange, 403, "{\"success\":false,\"error\":\"You are not authorized to modify this resource.\"}");
                    return;
                }
                boolean ok = reminderService.deleteReminder(reminderId);
                sendJsonResponse(exchange, 200, "{\"success\":" + ok + ",\"message\":\"Reminder deleted successfully from MySQL\"}");
            } catch (Exception e) {
                sendSafeError(exchange, 400, e);
            }
        }
    }

    static class ToggleReminderStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;
            if (!requireAuthentication(exchange)) return;

            try {
                String body = readRequestBody(exchange);
                Map<String, String> data = parseBody(body);

                int reminderId = parseIntSafe(data.get("reminderId"), 0);
                if (!landlordOwnsReminder(getAuthenticatedLandlordId(exchange), reminderId)) {
                    sendJsonResponse(exchange, 403, "{\"success\":false,\"error\":\"You are not authorized to modify this resource.\"}");
                    return;
                }
                Reminder r = reminderService.getReminderById(reminderId);
                if (r == null) {
                    sendJsonResponse(exchange, 404, "{\"success\":false,\"error\":\"Reminder not found\"}");
                    return;
                }

                String newStatus = "Completed".equalsIgnoreCase(r.getReminderStatus()) ? "Pending" : "Completed";
                boolean ok = reminderService.updateStatus(reminderId, newStatus);
                sendJsonResponse(exchange, 200, "{\"success\":" + ok + ",\"newStatus\":\"" + newStatus + "\",\"message\":\"Status toggled successfully in MySQL\"}");
            } catch (Exception e) {
                sendSafeError(exchange, 400, e);
            }
        }
    }

    // =========================================================================
    // 6. DASHBOARD SUMMARY HANDLER
    // =========================================================================

    static class DashboardSummaryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (handleOptions(exchange)) return;
            if (!requireAuthentication(exchange)) return;

            try {
                int landlordId = getAuthenticatedLandlordId(exchange);

                DashboardStats stats = dashboardService.getStats(landlordId);
                ArrayList<PropertyOverviewDTO> properties = dashboardService.getPropertyOverview(landlordId);
                ArrayList<PaymentRecordDTO> recentPayments = dashboardService.getRecentPayments(landlordId, 5);
                ArrayList<PaymentRecordDTO> pendingRent = dashboardService.getPendingRent(landlordId);
                ArrayList<ReminderDTO> reminders = dashboardService.getUpcomingReminders(landlordId, 5);

                StringBuilder json = new StringBuilder("{");
                json.append("\"success\":true,");
                json.append("\"landlordId\":").append(landlordId).append(",");

                // Stats object
                json.append("\"stats\":{");
                json.append("\"totalProperties\":").append(stats.getTotalProperties()).append(",");
                json.append("\"totalRooms\":").append(stats.getTotalRooms()).append(",");
                json.append("\"occupiedRooms\":").append(stats.getOccupiedRooms()).append(",");
                json.append("\"vacantRooms\":").append(stats.getVacantRooms()).append(",");
                json.append("\"totalTenants\":").append(stats.getTotalTenants()).append(",");
                json.append("\"rentCollected\":").append(stats.getRentCollected()).append(",");
                json.append("\"pendingRent\":").append(stats.getPendingRent());
                json.append("},");

                // Properties array
                json.append("\"properties\":[");
                for (int i = 0; i < properties.size(); i++) {
                    PropertyOverviewDTO p = properties.get(i);
                    if (i > 0) json.append(",");
                    json.append(String.format(
                            "{\"propertyId\":%d,\"propertyName\":\"%s\",\"propertyType\":\"%s\",\"location\":\"%s\"," +
                            "\"totalRooms\":%d,\"occupiedRooms\":%d,\"vacantRooms\":%d}",
                            p.getPropertyId(), escapeJson(p.getPropertyName()), escapeJson(p.getPropertyType()),
                            escapeJson(p.getLocation()), p.getTotalRooms(), p.getOccupiedRooms(), p.getVacantRooms()
                    ));
                }
                json.append("],");

                // Recent payments
                json.append("\"recentPayments\":[");
                for (int i = 0; i < recentPayments.size(); i++) {
                    PaymentRecordDTO pr = recentPayments.get(i);
                    if (i > 0) json.append(",");
                    json.append(String.format(
                            "{\"paymentId\":%d,\"tenantName\":\"%s\",\"propertyName\":\"%s\",\"amount\":%.2f," +
                            "\"paymentDate\":\"%s\",\"paymentMonth\":\"%s\",\"paymentStatus\":\"%s\"}",
                            pr.getPaymentId(), escapeJson(pr.getTenantName()), escapeJson(pr.getPropertyName()),
                            pr.getAmount(), escapeJson(pr.getPaymentDate()), escapeJson(pr.getPaymentMonth()),
                            escapeJson(pr.getPaymentStatus())
                    ));
                }
                json.append("],");

                // Pending rent
                json.append("\"pendingRent\":[");
                for (int i = 0; i < pendingRent.size(); i++) {
                    PaymentRecordDTO pr = pendingRent.get(i);
                    if (i > 0) json.append(",");
                    json.append(String.format(
                            "{\"paymentId\":%d,\"tenantName\":\"%s\",\"propertyName\":\"%s\",\"amount\":%.2f," +
                            "\"paymentDate\":\"%s\",\"paymentMonth\":\"%s\",\"paymentStatus\":\"%s\"}",
                            pr.getPaymentId(), escapeJson(pr.getTenantName()), escapeJson(pr.getPropertyName()),
                            pr.getAmount(), escapeJson(pr.getPaymentDate()), escapeJson(pr.getPaymentMonth()),
                            escapeJson(pr.getPaymentStatus())
                    ));
                }
                json.append("],");

                // Reminders
                json.append("\"reminders\":[");
                for (int i = 0; i < reminders.size(); i++) {
                    ReminderDTO r = reminders.get(i);
                    if (i > 0) json.append(",");
                    json.append(String.format(
                            "{\"reminderId\":%d,\"title\":\"%s\",\"relatedTo\":\"%s\",\"dueDate\":\"%s\"," +
                            "\"reminderType\":\"%s\",\"reminderStatus\":\"%s\"}",
                            r.getReminderId(), escapeJson(r.getTitle()), escapeJson(r.getRelatedTo()),
                            escapeJson(r.getDueDate()), escapeJson(r.getReminderType()),
                            escapeJson(r.getReminderStatus())
                    ));
                }
                json.append("]");

                json.append("}");
                sendJsonResponse(exchange, 200, json.toString());
            } catch (Exception e) {
                sendSafeError(exchange, 500, e);
            }
        }
    }

    // =========================================================================
    // 7. STATIC WEB ASSETS HANDLER
    // =========================================================================

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            // Clean leading slashes
            String relativePath = path.startsWith("/") ? path.substring(1) : path;
            if (isProtectedFrontendPage(relativePath) && getAuthenticatedLandlordId(exchange) == null) {
                exchange.getResponseHeaders().set("Location", "/frontend/auth/login.html");
                exchange.getResponseHeaders().set("Cache-Control", "no-store");
                exchange.sendResponseHeaders(302, -1);
                return;
            }
            File file = resolveWorkspaceFile(relativePath);

            if (file == null || !file.exists() || file.isDirectory()) {
                String notFound = "<h1>404 Not Found</h1><p>Resource not found: " + escapeJson(path) + "</p>";
                byte[] bytes = notFound.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(404, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
                return;
            }

            String mimeType = getMimeType(file.getName());
            exchange.getResponseHeaders().set("Content-Type", mimeType);
            exchange.sendResponseHeaders(200, file.length());

            try (InputStream is = new FileInputStream(file);
                 OutputStream os = exchange.getResponseBody()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
            }
        }

        private File resolveWorkspaceFile(String relativePath) {
            File f = new File(relativePath);
            if (f.exists()) return f;

            // Try relative to parent if process started inside bin/ or backend/
            f = new File("..", relativePath);
            if (f.exists()) return f;

            f = new File("../..", relativePath);
            if (f.exists()) return f;

            return null;
        }

        private String getMimeType(String fileName) {
            String lower = fileName.toLowerCase();
            if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
            if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
            if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
            if (lower.endsWith(".png")) return "image/png";
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
            if (lower.endsWith(".svg")) return "image/svg+xml";
            if (lower.endsWith(".ico")) return "image/x-icon";
            if (lower.endsWith(".woff")) return "font/woff";
            if (lower.endsWith(".woff2")) return "font/woff2";
            return "text/plain; charset=UTF-8";
        }
    }

    // =========================================================================
    // UTILITY METHODS & JSON FORMATTERS
    // =========================================================================

    private static final class LoginAttempt {
        private int failures;
        private long lockedUntilMs;

        private boolean isLocked() {
            if (lockedUntilMs == 0) return false;
            if (System.currentTimeMillis() >= lockedUntilMs) {
                lockedUntilMs = 0;
                failures = 0;
                return false;
            }
            return true;
        }

        private void recordFailure() {
            failures++;
            if (failures >= 5) {
                lockedUntilMs = System.currentTimeMillis() + (5 * 60 * 1000L);
            }
        }
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void sendJsonResponse(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendSafeError(HttpExchange exchange, int status, Exception error) throws IOException {
        System.err.println("[RentProofServer] Request failed: " + error.getClass().getSimpleName());
        String message = status == 400 ? "Invalid or incomplete request." : "Internal server error.";
        sendJsonResponse(exchange, status,
                "{\"success\":false,\"error\":\"" + message + "\"}");
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if (isAllowedOrigin(origin)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", origin);
            exchange.getResponseHeaders().set("Vary", "Origin");
            exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
        }
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, X-CSRF-Token");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
        exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");
        exchange.getResponseHeaders().set("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        if ("true".equalsIgnoreCase(System.getenv("RENTPROOF_HTTPS"))) {
            exchange.getResponseHeaders().set("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
        exchange.getResponseHeaders().set("Content-Security-Policy",
                "default-src 'self'; img-src 'self' data: https://fonts.gstatic.com https://fonts.googleapis.com; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; script-src 'self'");
    }

    private static boolean isAllowedOrigin(String origin) {
        if (origin == null || origin.trim().isEmpty()) return false;
        if ("http://localhost:8080".equals(origin) || "http://127.0.0.1:8080".equals(origin)) return true;

        String configuredOrigins = System.getenv("RENTPROOF_ALLOWED_ORIGINS");
        if (configuredOrigins == null || configuredOrigins.trim().isEmpty()) return false;
        for (String allowedOrigin : configuredOrigins.split(",")) {
            if (origin.equals(allowedOrigin.trim())) return true;
        }
        return false;
    }

    private static String secureCookieAttribute() {
        return "true".equalsIgnoreCase(System.getenv("RENTPROOF_HTTPS")) ? "; Secure" : "";
    }

    private static String getCookie(HttpExchange exchange, String name) {
        java.util.List<String> cookies = exchange.getRequestHeaders().get("Cookie");
        if (cookies == null) return null;
        String prefix = name + "=";
        for (String cookieHeader : cookies) {
            for (String cookie : cookieHeader.split(";")) {
                String trimmed = cookie.trim();
                if (trimmed.startsWith(prefix)) return trimmed.substring(prefix.length());
            }
        }
        return null;
    }

    private static boolean validCsrfRequest(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        if (origin != null && !isAllowedOrigin(origin)) return false;

        String cookieToken = getCookie(exchange, "rentproof_csrf");
        String headerToken = exchange.getRequestHeaders().getFirst("X-CSRF-Token");
        if (cookieToken == null || headerToken == null) return false;
        return MessageDigest.isEqual(
                cookieToken.getBytes(StandardCharsets.UTF_8),
                headerToken.getBytes(StandardCharsets.UTF_8));
    }

    static class CsrfFilter extends Filter {
        @Override
        public String description() {
            return "Validates CSRF tokens on state-changing API requests";
        }

        @Override
        public void doFilter(HttpExchange exchange, Chain chain) throws IOException {
            String method = exchange.getRequestMethod();
            if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)
                    || "DELETE".equalsIgnoreCase(method)) {
                addCorsHeaders(exchange);
                if (!validCsrfRequest(exchange)) {
                    sendJsonResponse(exchange, 403, "{\"success\":false,\"error\":\"Invalid or missing CSRF token.\"}");
                    return;
                }
            }
            chain.doFilter(exchange);
        }
    }

    private static String getSessionToken(HttpExchange exchange) {
        java.util.List<String> cookies = exchange.getRequestHeaders().get("Cookie");
        if (cookies == null) return null;
        for (String cookieHeader : cookies) {
            for (String cookie : cookieHeader.split(";")) {
                String trimmed = cookie.trim();
                if (trimmed.startsWith("rentproof_session=")) {
                    return trimmed.substring("rentproof_session=".length());
                }
            }
        }
        return null;
    }

    private static Integer getAuthenticatedLandlordId(HttpExchange exchange) {
        return SessionManager.getLandlordId(getSessionToken(exchange));
    }

    private static boolean requireAuthentication(HttpExchange exchange) throws IOException {
        if (getAuthenticatedLandlordId(exchange) != null) {
            return true;
        }
        sendJsonResponse(exchange, 401, "{\"success\":false,\"error\":\"Authentication required.\"}");
        return false;
    }

    private static boolean landlordOwnsProperty(int landlordId, int propertyId) {
        return resourceOwnedByLandlord(
                "SELECT 1 FROM properties WHERE property_id = ? AND landlord_id = ?",
                propertyId, landlordId);
    }

    private static boolean roomBelongsToProperty(int landlordId, int roomId, int propertyId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT 1 FROM rooms r JOIN properties p ON p.property_id = r.property_id "
                             + "WHERE r.room_id = ? AND r.property_id = ? AND p.landlord_id = ?")) {
            pstmt.setInt(1, roomId);
            pstmt.setInt(2, propertyId);
            pstmt.setInt(3, landlordId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private static boolean landlordOwnsRoom(int landlordId, int roomId) {
        return resourceOwnedByLandlord(
                "SELECT 1 FROM rooms r JOIN properties p ON p.property_id = r.property_id "
              + "WHERE r.room_id = ? AND p.landlord_id = ?",
                roomId, landlordId);
    }

    private static boolean landlordOwnsTenant(int landlordId, int tenantId) {
        return resourceOwnedByLandlord(
                "SELECT 1 FROM tenants t JOIN properties p ON p.property_id = t.property_id "
              + "WHERE t.tenant_id = ? AND p.landlord_id = ?",
                tenantId, landlordId);
    }

    private static boolean landlordOwnsPayment(int landlordId, int paymentId) {
        return resourceOwnedByLandlord(
                "SELECT 1 FROM payments py JOIN properties p ON p.property_id = py.property_id "
              + "WHERE py.payment_id = ? AND p.landlord_id = ?",
                paymentId, landlordId);
    }

    private static boolean landlordOwnsReminder(int landlordId, int reminderId) {
        return resourceOwnedByLandlord(
                "SELECT 1 FROM reminders WHERE reminder_id = ? AND landlord_id = ?",
                reminderId, landlordId);
    }

    private static boolean resourceOwnedByLandlord(String sql, int resourceId, int landlordId) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, resourceId);
            pstmt.setInt(2, landlordId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private static boolean handleOptions(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    private static boolean isProtectedFrontendPage(String path) {
        String normalized = path.replace('\\', '/');
        if (!normalized.endsWith(".html")) return false;
        return normalized.startsWith("frontend/dashboard/")
                || normalized.startsWith("frontend/properties/")
                || normalized.startsWith("frontend/rooms/")
                || normalized.startsWith("frontend/tenants/")
                || normalized.startsWith("frontend/payments/")
                || normalized.startsWith("frontend/reminders/")
                || normalized.startsWith("frontend/reports/")
                || normalized.startsWith("frontend/settings/");
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2) {
                map.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
            } else if (pair.length == 1) {
                map.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8), "");
            }
        }
        return map;
    }

    /**
     * Robust body parser handling JSON objects and URL-encoded form data
     * without third-party JSON dependencies.
     */
    public static Map<String, String> parseBody(String body) {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.trim().isEmpty()) return map;

        String trimmed = body.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            // Character-level JSON key-value parser supporting commas in strings
            int i = 1;
            int len = trimmed.length() - 1;
            while (i < len) {
                while (i < len && Character.isWhitespace(trimmed.charAt(i))) i++;
                if (i >= len) break;
                if (trimmed.charAt(i) == ',') { i++; continue; }
                if (trimmed.charAt(i) != '"') { i++; continue; }

                // Read key
                i++; // skip open quote
                int keyStart = i;
                while (i < len && trimmed.charAt(i) != '"') i++;
                String key = trimmed.substring(keyStart, i);
                if (i < len) i++; // skip close quote

                // Find colon
                while (i < len && trimmed.charAt(i) != ':') i++;
                if (i < len) i++; // skip colon
                while (i < len && Character.isWhitespace(trimmed.charAt(i))) i++;
                if (i >= len) break;

                // Read value
                String val = "";
                if (trimmed.charAt(i) == '"') {
                    i++; // skip open quote
                    StringBuilder sb = new StringBuilder();
                    while (i < len) {
                        char c = trimmed.charAt(i);
                        if (c == '\\' && i + 1 < len) {
                            char next = trimmed.charAt(i + 1);
                            if (next == '"' || next == '\\' || next == '/') { sb.append(next); i += 2; continue; }
                            else if (next == 'n') { sb.append('\n'); i += 2; continue; }
                            else if (next == 'r') { sb.append('\r'); i += 2; continue; }
                            else if (next == 't') { sb.append('\t'); i += 2; continue; }
                        }
                        if (c == '"') { i++; break; }
                        sb.append(c);
                        i++;
                    }
                    val = sb.toString();
                } else {
                    int valStart = i;
                    while (i < len && trimmed.charAt(i) != ',' && trimmed.charAt(i) != '}' && !Character.isWhitespace(trimmed.charAt(i))) {
                        i++;
                    }
                    val = trimmed.substring(valStart, i).trim();
                    if ("null".equalsIgnoreCase(val)) val = "";
                }
                map.put(key, val);
            }
        } else {
            // URL-encoded form data
            map = parseQueryParams(trimmed);
        }
        return map;
    }

    private static String tenantToJson(Tenant t) {
        return String.format(
                "{\"tenantId\":%d,\"propertyId\":%d,\"propertyName\":\"%s\",\"roomId\":%d,\"roomNumber\":\"%s\"," +
                "\"fullName\":\"%s\",\"email\":\"%s\",\"phone\":\"%s\",\"emergencyContact\":\"%s\"," +
                "\"permanentAddress\":\"%s\",\"idProofNumber\":\"%s\",\"moveInDate\":\"%s\",\"moveOutDate\":\"%s\"," +
                "\"monthlyRent\":%.2f,\"securityDeposit\":%.2f,\"tenantStatus\":\"%s\",\"createdAt\":\"%s\"}",
                t.getTenantId(), t.getPropertyId(), escapeJson(t.getPropertyName() != null ? t.getPropertyName() : ""),
                t.getRoomId(), escapeJson(t.getRoomNumber() != null ? t.getRoomNumber() : ""),
                escapeJson(t.getFullName()), escapeJson(t.getEmail()), escapeJson(t.getPhone()),
                escapeJson(t.getEmergencyContact() != null ? t.getEmergencyContact() : ""),
                escapeJson(t.getPermanentAddress() != null ? t.getPermanentAddress() : ""),
                escapeJson(maskSensitiveIdentifier(t.getIdProofNumber())),
                t.getMoveInDate() != null ? t.getMoveInDate().toString() : "",
                t.getMoveOutDate() != null ? t.getMoveOutDate().toString() : "",
                t.getMonthlyRent(), t.getSecurityDeposit(), escapeJson(t.getTenantStatus()),
                t.getCreatedAt() != null ? t.getCreatedAt().toString() : ""
        );
    }

    private static String maskSensitiveIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) return "";
        String normalized = identifier.replaceAll("[^A-Za-z0-9]", "");
        if (normalized.length() <= 4) return "****";
        return "****" + normalized.substring(normalized.length() - 4);
    }

    private static String paymentToJson(Payment p) {
        return String.format(
                "{\"paymentId\":%d,\"tenantId\":%d,\"propertyId\":%d,\"roomId\":%d," +
                "\"amount\":%.2f,\"paymentDate\":\"%s\",\"paymentMonth\":\"%s\"," +
                "\"paymentMode\":\"%s\",\"transactionRef\":\"%s\",\"paymentStatus\":\"%s\"," +
                "\"remarks\":\"%s\",\"tenantName\":\"%s\",\"propertyName\":\"%s\",\"roomNumber\":\"%s\"}",
                p.getPaymentId(), p.getTenantId(), p.getPropertyId(), p.getRoomId(),
                p.getAmount(), escapeJson(p.getPaymentDate()), escapeJson(p.getPaymentMonth()),
                escapeJson(p.getPaymentMode()), escapeJson(p.getTransactionRef()),
                escapeJson(p.getPaymentStatus()), escapeJson(p.getRemarks()),
                escapeJson(p.getTenantName()), escapeJson(p.getPropertyName()),
                escapeJson(p.getRoomNumber())
        );
    }

    private static String reminderToJson(Reminder r) {
        return String.format(
                "{\"reminderId\":%d,\"landlordId\":%d,\"tenantId\":%s,\"tenantName\":\"%s\"," +
                "\"title\":\"%s\",\"message\":\"%s\",\"dueDate\":\"%s\",\"reminderType\":\"%s\"," +
                "\"reminderStatus\":\"%s\",\"createdAt\":\"%s\"}",
                r.getReminderId(), r.getLandlordId(),
                r.getTenantId() != null ? r.getTenantId() : "null",
                escapeJson(r.getTenantName() != null ? r.getTenantName() : ""),
                escapeJson(r.getTitle()), escapeJson(r.getMessage()),
                r.getDueDate() != null ? r.getDueDate().toString() : "",
                escapeJson(r.getReminderType()), escapeJson(r.getReminderStatus()),
                r.getCreatedAt() != null ? r.getCreatedAt().toString() : ""
        );
    }

    private static int parseIntSafe(String val, int defaultVal) {
        if (val == null || val.trim().isEmpty()) return defaultVal;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private static double parseDoubleSafe(String val, double defaultVal) {
        if (val == null || val.trim().isEmpty()) return defaultVal;
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
