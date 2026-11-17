package com.rentproof.dashboard;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * DashboardServer - Lightweight Built-In HTTP Server for Landlord Dashboard
 * 
 * Strict compliance with Core Java SE:
 * - Uses standard library com.sun.net.httpserver (no external frameworks, no Spring Boot, no Servlets)
 * - Exposes /api/dashboard/summary endpoint for dynamic MySQL/JDBC data retrieval
 * - Serves static HTML/CSS/JS frontend files
 */
public class DashboardServer {

    private static final int PORT = 8080;
    private static final DashboardService dashboardService = new DashboardService();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // API Endpoint for Dashboard Analytics
        server.createContext("/api/dashboard/summary", new DashboardSummaryHandler());

        // Static Web Files Server
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null); // default executor
        System.out.println("============================================================");
        System.out.println("   RENTPROOF LANDLORD DASHBOARD SERVER RUNNING ON PORT " + PORT);
        System.out.println("   Open in Browser: http://localhost:" + PORT + "/frontend/dashboard/dashboard.html");
        System.out.println("============================================================");
        server.start();
    }

    /**
     * Handles /api/dashboard/summary GET requests.
     * Returns JSON aggregated metrics from MySQL via DashboardService and JDBC.
     */
    static class DashboardSummaryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Enable Cross-Origin Resource Sharing (CORS)
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            int landlordId = 1; // Default landlord ID for active session
            String query = exchange.getRequestURI().getQuery();
            if (query != null && query.contains("landlordId=")) {
                try {
                    String[] parts = query.split("landlordId=");
                    if (parts.length > 1) {
                        String idStr = parts[1].split("&")[0];
                        landlordId = Integer.parseInt(idStr);
                    }
                } catch (Exception ignored) {
                }
            }

            // Fetch live metrics from MySQL via JDBC
            DashboardStats stats = dashboardService.getStats(landlordId);
            ArrayList<PropertyOverviewDTO> properties = dashboardService.getPropertyOverview(landlordId);
            ArrayList<PaymentRecordDTO> recentPayments = dashboardService.getRecentPayments(landlordId, 5);
            ArrayList<PaymentRecordDTO> pendingRent = dashboardService.getPendingRent(landlordId);
            ArrayList<ReminderDTO> reminders = dashboardService.getUpcomingReminders(landlordId, 5);

            // Construct manual JSON without third-party libraries (Lecture 1 to 42 compliance)
            StringBuilder json = new StringBuilder();
            json.append("{");
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
                json.append("{");
                json.append("\"propertyId\":").append(p.getPropertyId()).append(",");
                json.append("\"propertyName\":\"").append(escapeJson(p.getPropertyName())).append("\",");
                json.append("\"propertyType\":\"").append(escapeJson(p.getPropertyType())).append("\",");
                json.append("\"location\":\"").append(escapeJson(p.getLocation())).append("\",");
                json.append("\"totalRooms\":").append(p.getTotalRooms()).append(",");
                json.append("\"occupiedRooms\":").append(p.getOccupiedRooms()).append(",");
                json.append("\"vacantRooms\":").append(p.getVacantRooms());
                json.append("}");
            }
            json.append("],");

            // Recent payments array
            json.append("\"recentPayments\":[");
            for (int i = 0; i < recentPayments.size(); i++) {
                PaymentRecordDTO pr = recentPayments.get(i);
                if (i > 0) json.append(",");
                json.append("{");
                json.append("\"paymentId\":").append(pr.getPaymentId()).append(",");
                json.append("\"tenantName\":\"").append(escapeJson(pr.getTenantName())).append("\",");
                json.append("\"propertyName\":\"").append(escapeJson(pr.getPropertyName())).append("\",");
                json.append("\"amount\":").append(pr.getAmount()).append(",");
                json.append("\"paymentDate\":\"").append(escapeJson(pr.getPaymentDate())).append("\",");
                json.append("\"paymentMonth\":\"").append(escapeJson(pr.getPaymentMonth())).append("\",");
                json.append("\"paymentStatus\":\"").append(escapeJson(pr.getPaymentStatus())).append("\"");
                json.append("}");
            }
            json.append("],");

            // Pending rent array
            json.append("\"pendingRent\":[");
            for (int i = 0; i < pendingRent.size(); i++) {
                PaymentRecordDTO pr = pendingRent.get(i);
                if (i > 0) json.append(",");
                json.append("{");
                json.append("\"paymentId\":").append(pr.getPaymentId()).append(",");
                json.append("\"tenantName\":\"").append(escapeJson(pr.getTenantName())).append("\",");
                json.append("\"propertyName\":\"").append(escapeJson(pr.getPropertyName())).append("\",");
                json.append("\"amount\":").append(pr.getAmount()).append(",");
                json.append("\"paymentDate\":\"").append(escapeJson(pr.getPaymentDate())).append("\",");
                json.append("\"paymentMonth\":\"").append(escapeJson(pr.getPaymentMonth())).append("\",");
                json.append("\"paymentStatus\":\"").append(escapeJson(pr.getPaymentStatus())).append("\"");
                json.append("}");
            }
            json.append("],");

            // Reminders array
            json.append("\"reminders\":[");
            for (int i = 0; i < reminders.size(); i++) {
                ReminderDTO r = reminders.get(i);
                if (i > 0) json.append(",");
                json.append("{");
                json.append("\"reminderId\":").append(r.getReminderId()).append(",");
                json.append("\"title\":\"").append(escapeJson(r.getTitle())).append("\",");
                json.append("\"relatedTo\":\"").append(escapeJson(r.getRelatedTo())).append("\",");
                json.append("\"dueDate\":\"").append(escapeJson(r.getDueDate())).append("\",");
                json.append("\"reminderType\":\"").append(escapeJson(r.getReminderType())).append("\",");
                json.append("\"reminderStatus\":\"").append(escapeJson(r.getReminderStatus())).append("\"");
                json.append("}");
            }
            json.append("]");

            json.append("}");

            byte[] responseBytes = json.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }

        private static String escapeJson(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        }
    }

    /**
     * Static file handler serving workspace HTML, CSS, JS, and image assets.
     */
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if ("/".equals(path) || path.isEmpty()) {
                path = "/index.html";
            }

            // Remove leading slash for local file lookup
            String relativePath = path.startsWith("/") ? path.substring(1) : path;
            File file = new File(relativePath);

            if (!file.exists() || file.isDirectory()) {
                String notFound = "<h1>404 Not Found</h1><p>File not found: " + path + "</p>";
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
                int count;
                while ((count = is.read(buffer)) != -1) {
                    os.write(buffer, 0, count);
                }
            }
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
            return "text/plain; charset=UTF-8";
        }
    }
}
