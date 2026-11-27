package com.rentproof;

import com.rentproof.dashboard.DashboardService;
import com.rentproof.dashboard.DashboardStats;
import com.rentproof.payment.Payment;
import com.rentproof.payment.PaymentService;
import com.rentproof.reminder.Reminder;
import com.rentproof.reminder.ReminderDAO;
import com.rentproof.reminder.ReminderService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * RentProofServer - Central Core Java HTTP Server on Port 8080
 * 
 * Unifies all RentProof modules:
 * - Reminder Management Module (Gungun)
 * - Dashboard Analytics Module (Gungun)
 * - Property & Room Inventory (Aditi)
 * - Payment Management Module (Aanvi)
 * - Tenant Directory (Shweta)
 * 
 * Built with standard JDK HttpServer (com.sun.net.httpserver).
 * No Spring Boot, No external server dependencies.
 */
public class RentProofServer {

    private static final int PORT = 8080;
    private static final ReminderService reminderService = new ReminderService();
    private static final DashboardService dashboardService = new DashboardService();
    private static final PaymentService paymentService = new PaymentService();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // ==========================================
        // 1. REMINDERS MODULE ENDPOINTS (Gungun)
        // ==========================================
        server.createContext("/api/reminders", new RemindersHandler());
        server.createContext("/api/reminders/stats", new ReminderStatsHandler());
        server.createContext("/api/reminders/get", new GetReminderByIdHandler());
        server.createContext("/api/reminders/add", new AddReminderHandler());
        server.createContext("/api/reminders/update", new UpdateReminderHandler());
        server.createContext("/api/reminders/update-status", new UpdateReminderStatusHandler());
        server.createContext("/api/reminders/delete", new DeleteReminderHandler());

        // ==========================================
        // 2. DASHBOARD MODULE ENDPOINTS (Gungun)
        // ==========================================
        server.createContext("/api/dashboard/stats", new DashboardStatsHandler());
        server.createContext("/api/dashboard/recent-activity", new DashboardActivityHandler());
        server.createContext("/api/dashboard/urgent-reminders", new DashboardUrgentRemindersHandler());

        // ==========================================
        // 3. PAYMENTS & TENANTS MODULE ENDPOINTS (Aanvi & Shared)
        // ==========================================
        server.createContext("/api/payments", new PaymentsHandler());
        server.createContext("/api/payments/stats", new PaymentStatsHandler());
        server.createContext("/api/payments/add", new AddPaymentHandler());
        server.createContext("/api/payments/update-status", new UpdatePaymentStatusHandler());
        server.createContext("/api/payments/delete", new DeletePaymentHandler());
        server.createContext("/api/tenants", new TenantsHandler());
        server.createContext("/api/properties", new PropertiesHandler());
        server.createContext("/api/rooms", new RoomsHandler());

        // ==========================================
        // 4. STATIC FILE SERVING
        // ==========================================
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null); // default executor
        System.out.println("============================================================");
        System.out.println("   RENTPROOF CORE HTTP SERVER RUNNING ON PORT " + PORT);
        System.out.println("   Dashboard Portal : http://localhost:" + PORT + "/frontend/dashboard/dashboard.html");
        System.out.println("   Reminders Module : http://localhost:" + PORT + "/frontend/reminders/reminders.html");
        System.out.println("   Payments Module  : http://localhost:" + PORT + "/frontend/payments/payments.html");
        System.out.println("   Properties Portal: http://localhost:" + PORT + "/frontend/properties/properties.html");
        System.out.println("============================================================");
        server.start();
    }

    // =========================================================================
    // REMINDERS API HANDLERS
    // =========================================================================

    static class RemindersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> queryParams = parseQueryParams(query);

                String search = queryParams.get("search");
                String type = queryParams.get("type");
                String status = queryParams.get("status");
                String tenantStr = queryParams.get("tenantId");

                ArrayList<Reminder> list;
                if (search != null && !search.trim().isEmpty()) {
                    list = reminderService.searchReminders(1, search.trim());
                } else if (tenantStr != null && !tenantStr.trim().isEmpty() && 
                           !"ALL".equalsIgnoreCase(tenantStr.trim()) && 
                           !"null".equalsIgnoreCase(tenantStr.trim())) {
                    if ("GENERAL".equalsIgnoreCase(tenantStr.trim())) {
                        list = new ArrayList<>();
                        for (Reminder r : reminderService.getRemindersByLandlord(1)) {
                            if (r.getTenantId() == null) list.add(r);
                        }
                    } else {
                        try {
                            list = reminderService.getRemindersByTenant(Integer.parseInt(tenantStr.trim()));
                        } catch (NumberFormatException e) {
                            list = reminderService.getRemindersByLandlord(1);
                        }
                    }
                } else {
                    list = reminderService.getRemindersByLandlord(1);
                }

                // Apply type and status filter if specified
                if (type != null && !type.trim().isEmpty() && !"ALL".equalsIgnoreCase(type.trim())) {
                    ArrayList<Reminder> filtered = new ArrayList<>();
                    for (Reminder r : list) {
                        if (type.equalsIgnoreCase(r.getReminderType())) filtered.add(r);
                    }
                    list = filtered;
                }
                if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status.trim())) {
                    ArrayList<Reminder> filtered = new ArrayList<>();
                    for (Reminder r : list) {
                        if (status.equalsIgnoreCase(r.getReminderStatus())) filtered.add(r);
                    }
                    list = filtered;
                }

                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) json.append(",");
                    json.append(reminderToJson(list.get(i)));
                }
                json.append("]");

                sendJsonResponse(exchange, 200, json.toString());
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    static class ReminderStatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                Map<String, Integer> stats = reminderService.calculateReminderStats(1);
                String json = String.format(
                    "{\"total\":%d,\"pending\":%d,\"completed\":%d,\"upcoming\":%d,\"overdue\":%d}",
                    stats.getOrDefault("total", 0),
                    stats.getOrDefault("pending", 0),
                    stats.getOrDefault("completed", 0),
                    stats.getOrDefault("upcoming", 0),
                    stats.getOrDefault("overdue", 0)
                );
                sendJsonResponse(exchange, 200, json);
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    static class GetReminderByIdHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> queryParams = parseQueryParams(query);
                int id = Integer.parseInt(queryParams.getOrDefault("id", "0"));

                Reminder r = reminderService.getReminderById(id);
                if (r != null) {
                    sendJsonResponse(exchange, 200, reminderToJson(r));
                } else {
                    sendJsonResponse(exchange, 404, "{\"error\":\"Reminder not found\"}");
                }
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    static class AddReminderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> data = parseFormDataOrJson(body);

                int landlordId = Integer.parseInt(data.getOrDefault("landlordId", "1"));
                String tenantVal = data.get("tenantId");
                Integer tenantId = (tenantVal != null && !tenantVal.trim().isEmpty() && 
                                    !"null".equalsIgnoreCase(tenantVal.trim()) && 
                                    !"GENERAL".equalsIgnoreCase(tenantVal.trim()) && 
                                    !"0".equals(tenantVal.trim()))
                    ? Integer.parseInt(tenantVal.trim())
                    : null;

                String title = data.getOrDefault("title", "");
                String message = data.getOrDefault("message", "");
                String dueDateStr = data.getOrDefault("dueDate", "");
                String reminderType = data.getOrDefault("reminderType", "Rent Due");
                String reminderStatus = data.getOrDefault("reminderStatus", "Pending");

                Date dueDate = reminderService.parseDate(dueDateStr);
                Reminder reminder = new Reminder(landlordId, tenantId, title, message, dueDate, reminderType, reminderStatus);

                int newId = reminderService.createReminder(reminder);
                sendJsonResponse(exchange, 201, "{\"success\":true,\"reminderId\":" + newId + ",\"message\":\"Reminder created successfully\"}");
            } catch (IllegalArgumentException e) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    static class UpdateReminderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> data = parseFormDataOrJson(body);

                int reminderId = Integer.parseInt(data.getOrDefault("reminderId", "0"));
                int landlordId = Integer.parseInt(data.getOrDefault("landlordId", "1"));
                String tenantVal = data.get("tenantId");
                Integer tenantId = (tenantVal != null && !tenantVal.trim().isEmpty() && 
                                    !"null".equalsIgnoreCase(tenantVal.trim()) && 
                                    !"GENERAL".equalsIgnoreCase(tenantVal.trim()) && 
                                    !"0".equals(tenantVal.trim()))
                    ? Integer.parseInt(tenantVal.trim())
                    : null;

                String title = data.getOrDefault("title", "");
                String message = data.getOrDefault("message", "");
                String dueDateStr = data.getOrDefault("dueDate", "");
                String reminderType = data.getOrDefault("reminderType", "Rent Due");
                String reminderStatus = data.getOrDefault("reminderStatus", "Pending");

                Date dueDate = reminderService.parseDate(dueDateStr);
                Reminder reminder = new Reminder(reminderId, landlordId, tenantId, title, message, dueDate, reminderType, reminderStatus, null);

                boolean updated = reminderService.updateReminder(reminder);
                sendJsonResponse(exchange, 200, "{\"success\":" + updated + ",\"message\":\"Reminder updated successfully\"}");
            } catch (IllegalArgumentException e) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    static class UpdateReminderStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> data = parseFormDataOrJson(body);

                int reminderId = Integer.parseInt(data.getOrDefault("reminderId", "0"));
                String newStatus = data.getOrDefault("reminderStatus", "Pending");

                boolean ok = reminderService.updateStatus(reminderId, newStatus);
                sendJsonResponse(exchange, 200, "{\"success\":" + ok + ",\"message\":\"Reminder status updated\"}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    static class DeleteReminderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> data = parseFormDataOrJson(body);

                int reminderId = Integer.parseInt(data.getOrDefault("reminderId", "0"));
                boolean ok = reminderService.deleteReminder(reminderId);
                sendJsonResponse(exchange, 200, "{\"success\":" + ok + ",\"message\":\"Reminder deleted successfully\"}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // =========================================================================
    // DASHBOARD API HANDLERS
    // =========================================================================

    static class DashboardStatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                DashboardStats stats = dashboardService.getDashboardStats(1);
                sendJsonResponse(exchange, 200, stats.toJson());
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    static class DashboardActivityHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                ArrayList<Map<String, String>> acts = dashboardService.getRecentActivities(1, 10);
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < acts.size(); i++) {
                    if (i > 0) json.append(",");
                    Map<String, String> a = acts.get(i);
                    json.append(String.format(
                        "{\"source\":\"%s\",\"description\":\"%s\",\"createdAt\":\"%s\",\"icon\":\"%s\"}",
                        escapeJson(a.get("source")),
                        escapeJson(a.get("description")),
                        escapeJson(a.get("createdAt")),
                        escapeJson(a.get("icon"))
                    ));
                }
                json.append("]");
                sendJsonResponse(exchange, 200, json.toString());
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    static class DashboardUrgentRemindersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                ArrayList<Map<String, Object>> list = dashboardService.getUrgentReminders(1, 6);
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) json.append(",");
                    Map<String, Object> m = list.get(i);
                    json.append(String.format(
                        "{\"reminderId\":%d,\"title\":\"%s\",\"dueDate\":\"%s\",\"reminderType\":\"%s\",\"reminderStatus\":\"%s\",\"tenantName\":\"%s\",\"daysLeft\":%d}",
                        m.get("reminderId"),
                        escapeJson((String) m.get("title")),
                        escapeJson((String) m.get("dueDate")),
                        escapeJson((String) m.get("reminderType")),
                        escapeJson((String) m.get("reminderStatus")),
                        escapeJson((String) m.get("tenantName")),
                        m.get("daysLeft")
                    ));
                }
                json.append("]");
                sendJsonResponse(exchange, 200, json.toString());
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // =========================================================================
    // PAYMENTS & SHARED HANDLERS (Aanvi & Shweta Integration)
    // =========================================================================

    static class PaymentsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> queryParams = parseQueryParams(query);

                String keyword = queryParams.get("search");
                String tenantStr = queryParams.get("tenantId");
                Integer tenantId = (tenantStr != null && !tenantStr.isEmpty()) ? Integer.parseInt(tenantStr) : null;
                String month = queryParams.get("month");
                String status = queryParams.get("status");

                ArrayList<Payment> payments;
                if ((keyword != null && !keyword.isEmpty()) || tenantId != null || 
                    (month != null && !month.isEmpty()) || (status != null && !status.isEmpty())) {
                    payments = paymentService.searchAndFilterPayments(keyword, tenantId, month, status);
                } else {
                    payments = paymentService.getAllPayments();
                }

                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < payments.size(); i++) {
                    if (i > 0) json.append(",");
                    json.append(paymentToJson(payments.get(i)));
                }
                json.append("]");

                sendJsonResponse(exchange, 200, json.toString());
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    static class PaymentStatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                Map<String, Object> stats = paymentService.getDashboardStats();
                String json = String.format(
                    "{\"totalRentCollected\":%.2f,\"successfulPaymentCount\":%d,\"pendingPaymentCount\":%d,\"overduePaymentCount\":%d}",
                    ((Number) stats.get("totalRentCollected")).doubleValue(),
                    ((Number) stats.get("successfulPaymentCount")).intValue(),
                    ((Number) stats.get("pendingPaymentCount")).intValue(),
                    ((Number) stats.get("overduePaymentCount")).intValue()
                );
                sendJsonResponse(exchange, 200, json);
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    static class AddPaymentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> data = parseFormDataOrJson(body);

                int tenantId = Integer.parseInt(data.getOrDefault("tenantId", "0"));
                int propertyId = Integer.parseInt(data.getOrDefault("propertyId", "0"));
                int roomId = Integer.parseInt(data.getOrDefault("roomId", "0"));
                double amount = Double.parseDouble(data.getOrDefault("amount", "0"));
                String paymentDate = data.getOrDefault("paymentDate", "");
                String paymentMonth = data.getOrDefault("paymentMonth", "");
                String paymentMode = data.getOrDefault("paymentMode", "Cash");
                String transactionRef = data.getOrDefault("transactionRef", "");
                String paymentStatus = data.getOrDefault("paymentStatus", "Paid");
                String remarks = data.getOrDefault("remarks", "");

                Payment payment = new Payment(tenantId, propertyId, roomId, amount, 
                                              paymentDate, paymentMonth, paymentMode, 
                                              transactionRef, paymentStatus, remarks);

                int newId = paymentService.recordPayment(payment);
                sendJsonResponse(exchange, 201, "{\"success\":true,\"paymentId\":" + newId + ",\"message\":\"Payment recorded successfully\"}");
            } catch (IllegalArgumentException e) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    static class UpdatePaymentStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> data = parseFormDataOrJson(body);

                int paymentId = Integer.parseInt(data.getOrDefault("paymentId", "0"));
                String status = data.getOrDefault("paymentStatus", "");

                boolean ok = paymentService.updatePaymentStatus(paymentId, status);
                sendJsonResponse(exchange, 200, "{\"success\":" + ok + ",\"message\":\"Payment status updated\"}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    static class DeletePaymentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                return;
            }

            try {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> data = parseFormDataOrJson(body);

                int paymentId = Integer.parseInt(data.getOrDefault("paymentId", "0"));
                boolean ok = paymentService.deletePayment(paymentId);
                sendJsonResponse(exchange, 200, "{\"success\":" + ok + ",\"message\":\"Payment deleted successfully\"}");
            } catch (Exception e) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    static class TenantsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                ArrayList<Map<String, Object>> tenants = paymentService.getAvailableTenants();
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < tenants.size(); i++) {
                    if (i > 0) json.append(",");
                    Map<String, Object> t = tenants.get(i);
                    json.append(String.format(
                        "{\"tenantId\":%d,\"fullName\":\"%s\",\"phone\":\"%s\",\"propertyId\":%d,\"propertyName\":\"%s\",\"roomId\":%d,\"roomNumber\":\"%s\",\"monthlyRent\":%.2f}",
                        t.get("tenantId"), escapeJson((String) t.get("fullName")),
                        escapeJson((String) t.get("phone")), t.get("propertyId"),
                        escapeJson((String) t.get("propertyName")), t.get("roomId"),
                        escapeJson((String) t.get("roomNumber")), ((Number) t.get("monthlyRent")).doubleValue()
                    ));
                }
                json.append("]");
                sendJsonResponse(exchange, 200, json.toString());
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    static class PropertiesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                ArrayList<Map<String, Object>> properties = paymentService.getAvailableProperties();
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < properties.size(); i++) {
                    if (i > 0) json.append(",");
                    Map<String, Object> p = properties.get(i);
                    json.append(String.format(
                        "{\"propertyId\":%d,\"propertyName\":\"%s\",\"propertyType\":\"%s\",\"city\":\"%s\"}",
                        p.get("propertyId"), escapeJson((String) p.get("propertyName")),
                        escapeJson((String) p.get("propertyType")), escapeJson((String) p.get("city"))
                    ));
                }
                json.append("]");
                sendJsonResponse(exchange, 200, json.toString());
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    static class RoomsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> queryParams = parseQueryParams(query);
                int propertyId = Integer.parseInt(queryParams.getOrDefault("propertyId", "0"));

                ArrayList<Map<String, Object>> rooms = paymentService.getRoomsByProperty(propertyId);
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < rooms.size(); i++) {
                    if (i > 0) json.append(",");
                    Map<String, Object> r = rooms.get(i);
                    json.append(String.format(
                        "{\"roomId\":%d,\"propertyId\":%d,\"roomNumber\":\"%s\",\"monthlyRent\":%.2f,\"roomStatus\":\"%s\"}",
                        r.get("roomId"), r.get("propertyId"),
                        escapeJson((String) r.get("roomNumber")),
                        ((Number) r.get("monthlyRent")).doubleValue(),
                        escapeJson((String) r.get("roomStatus"))
                    ));
                }
                json.append("]");
                sendJsonResponse(exchange, 200, json.toString());
            } catch (Exception e) {
                sendJsonResponse(exchange, 500, "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // =========================================================================
    // STATIC FILE HANDLER
    // =========================================================================

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.equals("/index.html") || path.equals("/dashboard")) {
                path = "/frontend/dashboard/dashboard.html";
            }

            File file = new File("." + path);
            if (!file.exists() || file.isDirectory()) {
                String notFound = "<h1>404 Not Found</h1><p>File not found: " + path + "</p>";
                exchange.sendResponseHeaders(404, notFound.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound.getBytes());
                }
                return;
            }

            String contentType = getContentType(file.getName());
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, file.length());

            try (InputStream is = new FileInputStream(file);
                 OutputStream os = exchange.getResponseBody()) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = is.read(buffer)) > 0) {
                    os.write(buffer, 0, count);
                }
            }
        }
    }

    // =========================================================================
    // UTILITY METHODS
    // =========================================================================

    private static String reminderToJson(Reminder r) {
        return String.format(
            "{" +
            "\"reminderId\":%d," +
            "\"landlordId\":%d," +
            "\"tenantId\":%s," +
            "\"tenantName\":\"%s\"," +
            "\"title\":\"%s\"," +
            "\"message\":\"%s\"," +
            "\"dueDate\":\"%s\"," +
            "\"reminderType\":\"%s\"," +
            "\"reminderStatus\":\"%s\"," +
            "\"createdAt\":\"%s\"," +
            "\"isOverdue\":%b," +
            "\"isUpcoming\":%b" +
            "}",
            r.getReminderId(),
            r.getLandlordId(),
            (r.getTenantId() != null ? r.getTenantId().toString() : "null"),
            escapeJson(r.getTenantName() != null ? r.getTenantName() : "General Property"),
            escapeJson(r.getTitle()),
            escapeJson(r.getMessage()),
            (r.getDueDate() != null ? r.getDueDate().toString() : ""),
            escapeJson(r.getReminderType()),
            escapeJson(r.getReminderStatus()),
            (r.getCreatedAt() != null ? r.getCreatedAt().toString() : ""),
            r.isOverdue(),
            r.isUpcoming(7)
        );
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

    private static void sendJsonResponse(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
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

    private static Map<String, String> parseFormDataOrJson(String body) {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.trim().isEmpty()) return map;

        String trimmed = body.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            String inner = trimmed.substring(1, trimmed.length() - 1);
            String[] tokens = inner.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            for (String token : tokens) {
                String[] kv = token.split(":", 2);
                if (kv.length == 2) {
                    String k = kv[0].trim().replace("\"", "");
                    String v = kv[1].trim();
                    if (v.startsWith("\"") && v.endsWith("\"")) {
                        v = v.substring(1, v.length() - 1);
                    } else if ("null".equalsIgnoreCase(v)) {
                        v = null;
                    }
                    map.put(k, v);
                }
            }
        } else {
            map = parseQueryParams(trimmed);
        }
        return map;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String getContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        return "text/plain; charset=UTF-8";
    }
}
