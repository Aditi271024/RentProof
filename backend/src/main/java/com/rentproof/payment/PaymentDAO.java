package com.rentproof.payment;

import com.rentproof.database.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * PaymentDAO handles all Data Access Object (DAO) database operations for the 'payments' table.
 * Module: Payment Management
 * Author: Aanvi
 * 
 * Concepts used (Syllabus up to Lecture 42):
 * - JDBC API (Connection, PreparedStatement, ResultSet, Statement.RETURN_GENERATED_KEYS)
 * - Java Collections Framework (ArrayList, Map, HashMap)
 * - try-with-resources for automatic resource closing
 * - CRUD operations (Create, Read, Update, Delete)
 * - SQL Queries with Multi-Table Relational JOINs (tenants, properties, rooms)
 * - Aggregation functions (SUM, COUNT)
 * - Exception Handling (SQLException)
 */
public class PaymentDAO {

    /**
     * Inserts a new payment record into the MySQL database.
     * 
     * @param payment Payment object containing transaction details
     * @return generated payment_id if successful, or -1 on failure
     * @throws SQLException if a database error occurs
     */
    public int addPayment(Payment payment) throws SQLException {
        String sql = "INSERT INTO payments (tenant_id, property_id, room_id, amount, " +
                     "payment_date, payment_month, payment_mode, transaction_ref, " +
                     "payment_status, remarks) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, payment.getTenantId());
            pstmt.setInt(2, payment.getPropertyId());
            pstmt.setInt(3, payment.getRoomId());
            pstmt.setDouble(4, payment.getAmount());

            // Convert string date to java.sql.Date
            Date sqlDate = (payment.getPaymentDate() != null && !payment.getPaymentDate().trim().isEmpty())
                    ? Date.valueOf(payment.getPaymentDate().trim())
                    : new Date(System.currentTimeMillis());
            pstmt.setDate(5, sqlDate);

            pstmt.setString(6, payment.getPaymentMonth().trim());
            pstmt.setString(7, payment.getPaymentMode().trim());
            pstmt.setString(8, payment.getTransactionRef() != null ? payment.getTransactionRef().trim() : null);
            pstmt.setString(9, (payment.getPaymentStatus() != null) ? payment.getPaymentStatus().trim() : "Paid");
            pstmt.setString(10, payment.getRemarks() != null ? payment.getRemarks().trim() : null);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int generatedId = generatedKeys.getInt(1);
                        payment.setPaymentId(generatedId);
                        return generatedId;
                    }
                }
            }
            return -1;
        } catch (SQLException e) {
            System.err.println("[PaymentDAO] addPayment failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Retrieves all payment records ordered chronologically (newest first).
     * Performs SQL LEFT JOINs with tenants, properties, and rooms tables to fetch human-readable names.
     * 
     * @return ArrayList of Payment objects
     */
    public ArrayList<Payment> getAllPayments() {
        ArrayList<Payment> list = new ArrayList<>();
        String sql = "SELECT p.payment_id, p.tenant_id, p.property_id, p.room_id, p.amount, " +
                     "p.payment_date, p.payment_month, p.payment_mode, p.transaction_ref, " +
                     "p.payment_status, p.remarks, p.created_at, " +
                     "t.full_name AS tenant_name, pr.property_name, r.room_number " +
                     "FROM payments p " +
                     "LEFT JOIN tenants t ON p.tenant_id = t.tenant_id " +
                     "LEFT JOIN properties pr ON p.property_id = pr.property_id " +
                     "LEFT JOIN rooms r ON p.room_id = r.room_id " +
                     "ORDER BY p.payment_date DESC, p.payment_id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToPaymentWithDetails(rs));
            }
        } catch (SQLException e) {
            System.err.println("[PaymentDAO] getAllPayments failed: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves a single payment record by its primary key ID.
     * 
     * @param paymentId the ID of the payment to retrieve
     * @return Payment object if found, or null
     */
    public Payment getPaymentById(int paymentId) {
        String sql = "SELECT p.payment_id, p.tenant_id, p.property_id, p.room_id, p.amount, " +
                     "p.payment_date, p.payment_month, p.payment_mode, p.transaction_ref, " +
                     "p.payment_status, p.remarks, p.created_at, " +
                     "t.full_name AS tenant_name, pr.property_name, r.room_number " +
                     "FROM payments p " +
                     "LEFT JOIN tenants t ON p.tenant_id = t.tenant_id " +
                     "LEFT JOIN properties pr ON p.property_id = pr.property_id " +
                     "LEFT JOIN rooms r ON p.room_id = r.room_id " +
                     "WHERE p.payment_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, paymentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToPaymentWithDetails(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[PaymentDAO] getPaymentById failed: " + e.getMessage());
        }
        return null;
    }

    /**
     * Updates an existing payment record.
     * 
     * @param payment Payment object containing updated fields
     * @return true if updated successfully, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean updatePayment(Payment payment) throws SQLException {
        String sql = "UPDATE payments SET tenant_id = ?, property_id = ?, room_id = ?, " +
                     "amount = ?, payment_date = ?, payment_month = ?, payment_mode = ?, " +
                     "transaction_ref = ?, payment_status = ?, remarks = ? " +
                     "WHERE payment_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, payment.getTenantId());
            pstmt.setInt(2, payment.getPropertyId());
            pstmt.setInt(3, payment.getRoomId());
            pstmt.setDouble(4, payment.getAmount());

            Date sqlDate = (payment.getPaymentDate() != null && !payment.getPaymentDate().trim().isEmpty())
                    ? Date.valueOf(payment.getPaymentDate().trim())
                    : new Date(System.currentTimeMillis());
            pstmt.setDate(5, sqlDate);

            pstmt.setString(6, payment.getPaymentMonth().trim());
            pstmt.setString(7, payment.getPaymentMode().trim());
            pstmt.setString(8, payment.getTransactionRef());
            pstmt.setString(9, payment.getPaymentStatus());
            pstmt.setString(10, payment.getRemarks());
            pstmt.setInt(11, payment.getPaymentId());

            int rowsUpdated = pstmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            System.err.println("[PaymentDAO] updatePayment failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Updates specifically the payment status of an existing payment (e.g. marking Pending -> Paid).
     * 
     * @param paymentId primary key of payment
     * @param newStatus new status ('Paid', 'Pending', 'Overdue', 'Failed')
     * @return true if update succeeded
     * @throws SQLException if a database error occurs
     */
    public boolean updatePaymentStatus(int paymentId, String newStatus) throws SQLException {
        String sql = "UPDATE payments SET payment_status = ? WHERE payment_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus.trim());
            pstmt.setInt(2, paymentId);

            int rowsUpdated = pstmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            System.err.println("[PaymentDAO] updatePaymentStatus failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Deletes a payment record from the database.
     * 
     * @param paymentId ID of the payment to delete
     * @return true if deleted successfully, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean deletePayment(int paymentId) throws SQLException {
        String sql = "DELETE FROM payments WHERE payment_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, paymentId);
            int rowsDeleted = pstmt.executeUpdate();
            return rowsDeleted > 0;
        } catch (SQLException e) {
            System.err.println("[PaymentDAO] deletePayment failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Searches and filters payments dynamically based on user criteria:
     * - keyword: matches tenant name, transaction reference, property name, or remarks
     * - tenantId: filters for a specific tenant (if > 0)
     * - paymentMonth: filters by specific month (if not null / not "ALL")
     * - paymentStatus: filters by status (if not null / not "ALL")
     * 
     * @return ArrayList of filtered Payment objects
     */
    public ArrayList<Payment> searchAndFilterPayments(String keyword, Integer tenantId, String paymentMonth, String paymentStatus) {
        ArrayList<Payment> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT p.payment_id, p.tenant_id, p.property_id, p.room_id, p.amount, ");
        sql.append("p.payment_date, p.payment_month, p.payment_mode, p.transaction_ref, ");
        sql.append("p.payment_status, p.remarks, p.created_at, ");
        sql.append("t.full_name AS tenant_name, pr.property_name, r.room_number ");
        sql.append("FROM payments p ");
        sql.append("LEFT JOIN tenants t ON p.tenant_id = t.tenant_id ");
        sql.append("LEFT JOIN properties pr ON p.property_id = pr.property_id ");
        sql.append("LEFT JOIN rooms r ON p.room_id = r.room_id ");
        sql.append("WHERE 1=1 ");

        ArrayList<Object> params = new ArrayList<>();

        // Tenant Filter
        if (tenantId != null && tenantId > 0) {
            sql.append("AND p.tenant_id = ? ");
            params.add(tenantId);
        }

        // Month Filter
        if (paymentMonth != null && !paymentMonth.trim().isEmpty() && !paymentMonth.equalsIgnoreCase("ALL")) {
            sql.append("AND p.payment_month = ? ");
            params.add(paymentMonth.trim());
        }

        // Status Filter
        if (paymentStatus != null && !paymentStatus.trim().isEmpty() && !paymentStatus.equalsIgnoreCase("ALL")) {
            sql.append("AND p.payment_status = ? ");
            params.add(paymentStatus.trim());
        }

        // Text Search Keyword
        if (keyword != null && !keyword.trim().isEmpty()) {
            String wildcard = "%" + keyword.trim().toLowerCase() + "%";
            sql.append("AND (LOWER(t.full_name) LIKE ? OR LOWER(p.transaction_ref) LIKE ? OR LOWER(pr.property_name) LIKE ? OR LOWER(p.remarks) LIKE ?) ");
            params.add(wildcard);
            params.add(wildcard);
            params.add(wildcard);
            params.add(wildcard);
        }

        sql.append("ORDER BY p.payment_date DESC, p.payment_id DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToPaymentWithDetails(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[PaymentDAO] searchAndFilterPayments failed: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves the most recent N payment records for the dashboard overview.
     */
    public ArrayList<Payment> getRecentPayments(int limit) {
        ArrayList<Payment> list = new ArrayList<>();
        String sql = "SELECT p.payment_id, p.tenant_id, p.property_id, p.room_id, p.amount, " +
                     "p.payment_date, p.payment_month, p.payment_mode, p.transaction_ref, " +
                     "p.payment_status, p.remarks, p.created_at, " +
                     "t.full_name AS tenant_name, pr.property_name, r.room_number " +
                     "FROM payments p " +
                     "LEFT JOIN tenants t ON p.tenant_id = t.tenant_id " +
                     "LEFT JOIN properties pr ON p.property_id = pr.property_id " +
                     "LEFT JOIN rooms r ON p.room_id = r.room_id " +
                     "ORDER BY p.payment_date DESC, p.payment_id DESC LIMIT ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToPaymentWithDetails(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[PaymentDAO] getRecentPayments failed: " + e.getMessage());
        }
        return list;
    }

    // ==========================================
    // STATISTICAL & AGGREGATION QUERIES
    // ==========================================

    /**
     * Calculates total rent collected from all 'Paid' transactions.
     */
    public double getTotalRentCollected(int landlordId) {
        String sql = "SELECT COALESCE(SUM(py.amount), 0) AS total FROM payments py "
                + "JOIN properties pr ON pr.property_id = py.property_id "
                + "WHERE py.payment_status = 'Paid' AND pr.landlord_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, landlordId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getDouble("total");
            }
        } catch (SQLException e) {
            System.err.println("[PaymentDAO] getTotalRentCollected failed: " + e.getMessage());
        }
        return 0.0;
    }

    /**
     * Returns count of successful payments (status = 'Paid').
     */
    public int getSuccessfulPaymentCount(int landlordId) {
        return getCountByStatus("Paid", landlordId);
    }

    /**
     * Returns count of pending payments (status = 'Pending').
     */
    public int getPendingPaymentCount(int landlordId) {
        return getCountByStatus("Pending", landlordId);
    }

    /**
     * Returns count of overdue payments (status = 'Overdue').
     */
    public int getOverduePaymentCount(int landlordId) {
        return getCountByStatus("Overdue", landlordId);
    }

    /**
     * Calculates the total amount assigned to overdue payments.
     */
    public double getOverdueAmount(int landlordId) {
        String sql = "SELECT COALESCE(SUM(py.amount), 0) AS total FROM payments py "
                + "JOIN properties pr ON pr.property_id = py.property_id "
                + "WHERE py.payment_status = 'Overdue' AND pr.landlord_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, landlordId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getDouble("total");
            }
        } catch (SQLException e) {
            System.err.println("[PaymentDAO] getOverdueAmount failed: " + e.getMessage());
        }
        return 0.0;
    }

    private int getCountByStatus(String status, int landlordId) {
        String sql = "SELECT COUNT(*) AS total FROM payments py "
                + "JOIN properties pr ON pr.property_id = py.property_id "
                + "WHERE py.payment_status = ? AND pr.landlord_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, landlordId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            System.err.println("[PaymentDAO] getCountByStatus(" + status + ") failed: " + e.getMessage());
        }
        return 0;
    }

    // =========================================================================
    // DYNAMIC DATABASE LOOKUP HELPERS (Load available records from MySQL)
    // Strictly avoids hardcoding records as required by Section 5 of specification
    // =========================================================================

    /**
     * Retrieves active tenants from MySQL database to populate the "Select Tenant" dropdown.
     * Includes assigned property_id, room_id, and room rent for auto-filling payment amounts.
     */
    public ArrayList<Map<String, Object>> getAvailableTenants() {
        ArrayList<Map<String, Object>> tenants = new ArrayList<>();
        String sql = "SELECT t.tenant_id, t.full_name, t.phone, t.property_id, t.room_id, " +
                     "pr.property_name, r.room_number, COALESCE(t.monthly_rent, r.monthly_rent) AS monthly_rent " +
                     "FROM tenants t " +
                     "LEFT JOIN properties pr ON t.property_id = pr.property_id " +
                     "LEFT JOIN rooms r ON t.room_id = r.room_id " +
                     "ORDER BY t.full_name ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("tenantId", rs.getInt("tenant_id"));
                map.put("fullName", rs.getString("full_name"));
                map.put("phone", rs.getString("phone"));
                map.put("propertyId", rs.getInt("property_id"));
                map.put("propertyName", rs.getString("property_name"));
                map.put("roomId", rs.getInt("room_id"));
                map.put("roomNumber", rs.getString("room_number"));
                map.put("monthlyRent", rs.getDouble("monthly_rent"));
                tenants.add(map);
            }
        } catch (SQLException e) {
            System.err.println("[PaymentDAO] getAvailableTenants failed: " + e.getMessage());
        }
        return tenants;
    }

    /**
     * Retrieves all properties from MySQL database to populate property selection dropdown.
     */
    public ArrayList<Map<String, Object>> getAvailableProperties() {
        ArrayList<Map<String, Object>> properties = new ArrayList<>();
        String sql = "SELECT property_id, property_name, property_type, city FROM properties ORDER BY property_name ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("propertyId", rs.getInt("property_id"));
                map.put("propertyName", rs.getString("property_name"));
                map.put("propertyType", rs.getString("property_type"));
                map.put("city", rs.getString("city"));
                properties.add(map);
            }
        } catch (SQLException e) {
            System.err.println("[PaymentDAO] getAvailableProperties failed: " + e.getMessage());
        }
        return properties;
    }

    /**
     * Retrieves all rooms belonging to a given property from MySQL database.
     */
    public ArrayList<Map<String, Object>> getRoomsByProperty(int propertyId) {
        ArrayList<Map<String, Object>> rooms = new ArrayList<>();
        String sql = "SELECT room_id, property_id, room_number, monthly_rent, room_status " +
                     "FROM rooms WHERE property_id = ? ORDER BY room_number ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, propertyId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("roomId", rs.getInt("room_id"));
                    map.put("propertyId", rs.getInt("property_id"));
                    map.put("roomNumber", rs.getString("room_number"));
                    map.put("monthlyRent", rs.getDouble("monthly_rent"));
                    map.put("roomStatus", rs.getString("room_status"));
                    rooms.add(map);
                }
            }
        } catch (SQLException e) {
            System.err.println("[PaymentDAO] getRoomsByProperty failed: " + e.getMessage());
        }
        return rooms;
    }

    // ==========================================
    // RESULT SET OBJECT-RELATIONAL MAPPING (ORM)
    // ==========================================

    /**
     * Maps a single row of ResultSet to a Payment domain object.
     */
    private Payment mapRowToPaymentWithDetails(ResultSet rs) throws SQLException {
        int paymentId = rs.getInt("payment_id");
        int tenantId = rs.getInt("tenant_id");
        int propertyId = rs.getInt("property_id");
        int roomId = rs.getInt("room_id");
        double amount = rs.getDouble("amount");

        Date sqlDate = rs.getDate("payment_date");
        String paymentDate = (sqlDate != null) ? sqlDate.toString() : "";

        String paymentMonth = rs.getString("payment_month");
        String paymentMode = rs.getString("payment_mode");
        String transactionRef = rs.getString("transaction_ref");
        String paymentStatus = rs.getString("payment_status");
        String remarks = rs.getString("remarks");
        Timestamp createdAt = rs.getTimestamp("created_at");

        String tenantName = rs.getString("tenant_name");
        String propertyName = rs.getString("property_name");
        String roomNumber = rs.getString("room_number");

        return new Payment(paymentId, tenantId, propertyId, roomId, amount, paymentDate, 
                           paymentMonth, paymentMode, transactionRef, paymentStatus, 
                           remarks, createdAt, tenantName, propertyName, roomNumber);
    }
}

