package com.rentproof.dashboard;

import com.rentproof.database.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * DashboardDAO - Data Access Object for Landlord Dashboard Analytics
 * Module: Dashboard Module
 * Developer: Gungun (BTech CSE Project)
 * 
 * Executes live relational SQL queries against MySQL `rentproof` database.
 * No hardcoded or fake statistics.
 */
public class DashboardDAO {

    /**
     * Aggregates live system-wide statistics for a landlord (or globally if landlordId <= 0).
     * 
     * @param landlordId ID of landlord (default 1 in demo)
     * @return populated DashboardStats instance
     */
    public DashboardStats getDashboardStats(int landlordId) {
        DashboardStats stats = new DashboardStats();

        try (Connection conn = DBConnection.getConnection()) {
            // 1. Properties count
            String sqlProp = (landlordId > 0)
                ? "SELECT COUNT(*) FROM properties WHERE landlord_id = ?"
                : "SELECT COUNT(*) FROM properties";
            try (PreparedStatement ps = conn.prepareStatement(sqlProp)) {
                if (landlordId > 0) ps.setInt(1, landlordId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) stats.setTotalProperties(rs.getInt(1));
                }
            }

            // 2. Rooms inventory by status
            String sqlRooms = (landlordId > 0)
                ? "SELECT r.room_status, COUNT(*) FROM rooms r " +
                  "JOIN properties p ON r.property_id = p.property_id " +
                  "WHERE p.landlord_id = ? GROUP BY r.room_status"
                : "SELECT room_status, COUNT(*) FROM rooms GROUP BY room_status";
            try (PreparedStatement ps = conn.prepareStatement(sqlRooms)) {
                if (landlordId > 0) ps.setInt(1, landlordId);
                try (ResultSet rs = ps.executeQuery()) {
                    int totalRooms = 0;
                    int occupied = 0;
                    int vacant = 0;
                    int maintenance = 0;
                    while (rs.next()) {
                        String status = rs.getString(1);
                        int count = rs.getInt(2);
                        totalRooms += count;
                        if ("Occupied".equalsIgnoreCase(status)) occupied += count;
                        else if ("Vacant".equalsIgnoreCase(status)) vacant += count;
                        else if ("Maintenance".equalsIgnoreCase(status)) maintenance += count;
                    }
                    stats.setTotalRooms(totalRooms);
                    stats.setOccupiedRooms(occupied);
                    stats.setVacantRooms(vacant);
                    stats.setMaintenanceRooms(maintenance);
                    double rate = totalRooms > 0 ? ((double) occupied / totalRooms) * 100.0 : 0.0;
                    stats.setOccupancyRate(rate);
                }
            }

            // 3. Active Tenants count
            String sqlTenants = (landlordId > 0)
                ? "SELECT COUNT(*) FROM tenants t " +
                  "JOIN properties p ON t.property_id = p.property_id " +
                  "WHERE p.landlord_id = ? AND t.tenant_status = 'Active'"
                : "SELECT COUNT(*) FROM tenants WHERE tenant_status = 'Active'";
            try (PreparedStatement ps = conn.prepareStatement(sqlTenants)) {
                if (landlordId > 0) ps.setInt(1, landlordId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) stats.setTotalTenants(rs.getInt(1));
                }
            }

            // 4. Payments summary
            String sqlPayments = (landlordId > 0)
                ? "SELECT COUNT(*), " +
                  "COALESCE(SUM(CASE WHEN py.payment_status = 'Paid' THEN py.amount ELSE 0 END), 0) AS total_collected, " +
                  "COUNT(CASE WHEN py.payment_status = 'Paid' THEN 1 END) AS paid_count, " +
                  "COUNT(CASE WHEN py.payment_status = 'Pending' THEN 1 END) AS pending_count, " +
                  "COUNT(CASE WHEN py.payment_status = 'Overdue' THEN 1 END) AS overdue_count " +
                  "FROM payments py " +
                  "JOIN properties p ON py.property_id = p.property_id " +
                  "WHERE p.landlord_id = ?"
                : "SELECT COUNT(*), " +
                  "COALESCE(SUM(CASE WHEN payment_status = 'Paid' THEN amount ELSE 0 END), 0) AS total_collected, " +
                  "COUNT(CASE WHEN payment_status = 'Paid' THEN 1 END) AS paid_count, " +
                  "COUNT(CASE WHEN payment_status = 'Pending' THEN 1 END) AS pending_count, " +
                  "COUNT(CASE WHEN payment_status = 'Overdue' THEN 1 END) AS overdue_count " +
                  "FROM payments";
            try (PreparedStatement ps = conn.prepareStatement(sqlPayments)) {
                if (landlordId > 0) ps.setInt(1, landlordId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        stats.setTotalPayments(rs.getInt(1));
                        stats.setTotalRentCollected(rs.getDouble(2));
                        stats.setCompletedPayments(rs.getInt(3));
                        stats.setPendingPayments(rs.getInt(4));
                        stats.setOverduePayments(rs.getInt(5));
                    }
                }
            }

            // 5. Reminders summary
            String sqlReminders = (landlordId > 0)
                ? "SELECT COUNT(*), " +
                  "COUNT(CASE WHEN reminder_status = 'Pending' THEN 1 END) AS pending_count, " +
                  "COUNT(CASE WHEN reminder_status IN ('Completed', 'Resolved') THEN 1 END) AS completed_count, " +
                  "COUNT(CASE WHEN reminder_status NOT IN ('Completed', 'Resolved') AND due_date < CURRENT_DATE THEN 1 END) AS overdue_count, " +
                  "COUNT(CASE WHEN reminder_status NOT IN ('Completed', 'Resolved') AND due_date >= CURRENT_DATE AND due_date <= DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY) THEN 1 END) AS upcoming_count " +
                  "FROM reminders WHERE landlord_id = ?"
                : "SELECT COUNT(*), " +
                  "COUNT(CASE WHEN reminder_status = 'Pending' THEN 1 END) AS pending_count, " +
                  "COUNT(CASE WHEN reminder_status IN ('Completed', 'Resolved') THEN 1 END) AS completed_count, " +
                  "COUNT(CASE WHEN reminder_status NOT IN ('Completed', 'Resolved') AND due_date < CURRENT_DATE THEN 1 END) AS overdue_count, " +
                  "COUNT(CASE WHEN reminder_status NOT IN ('Completed', 'Resolved') AND due_date >= CURRENT_DATE AND due_date <= DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY) THEN 1 END) AS upcoming_count " +
                  "FROM reminders";
            try (PreparedStatement ps = conn.prepareStatement(sqlReminders)) {
                if (landlordId > 0) ps.setInt(1, landlordId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        stats.setTotalReminders(rs.getInt(1));
                        stats.setPendingReminders(rs.getInt(2));
                        stats.setCompletedReminders(rs.getInt(3));
                        stats.setOverdueReminders(rs.getInt(4));
                        stats.setUpcomingReminders(rs.getInt(5));
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("[DashboardDAO] getDashboardStats error: " + e.getMessage());
        }

        return stats;
    }

    /**
     * Retrieves unified recent activity records across MySQL database.
     * 
     * @param landlordId landlord ID
     * @param limit maximum activities to fetch (e.g. 10)
     * @return ArrayList of Activity maps
     */
    public ArrayList<Map<String, String>> getRecentActivities(int landlordId, int limit) {
        ArrayList<Map<String, String>> list = new ArrayList<>();
        int max = limit > 0 ? limit : 8;

        String sql = 
            "SELECT 'reminder' AS source, " +
            "CONCAT('Reminder: ', r.title, ' (Due: ', r.due_date, ')') AS description, " +
            "r.created_at, " +
            "CASE WHEN r.reminder_status = 'Completed' THEN '✅' WHEN r.due_date < CURRENT_DATE THEN '⚠️' ELSE '🔔' END AS icon " +
            "FROM reminders r WHERE r.landlord_id = ? " +
            "UNION ALL " +
            "SELECT 'payment' AS source, " +
            "CONCAT('Payment of Rs. ', py.amount, ' received (', py.payment_month, ')') AS description, " +
            "py.created_at, " +
            "'💳' AS icon " +
            "FROM payments py JOIN properties p ON py.property_id = p.property_id WHERE p.landlord_id = ? " +
            "UNION ALL " +
            "SELECT 'tenant' AS source, " +
            "CONCAT('Tenant registered: ', t.full_name) AS description, " +
            "t.created_at, " +
            "'👤' AS icon " +
            "FROM tenants t JOIN properties p ON t.property_id = p.property_id WHERE p.landlord_id = ? " +
            "ORDER BY created_at DESC LIMIT ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, landlordId > 0 ? landlordId : 1);
            ps.setInt(2, landlordId > 0 ? landlordId : 1);
            ps.setInt(3, landlordId > 0 ? landlordId : 1);
            ps.setInt(4, max);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, String> act = new HashMap<>();
                    act.put("source", rs.getString("source"));
                    act.put("description", rs.getString("description"));
                    act.put("createdAt", String.valueOf(rs.getTimestamp("created_at")));
                    act.put("icon", rs.getString("icon"));
                    list.add(act);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DashboardDAO] getRecentActivities error: " + e.getMessage());
        }

        return list;
    }

    /**
     * Retrieves urgent reminders (Overdue or Due within 7 days) to display on Dashboard.
     */
    public ArrayList<Map<String, Object>> getUrgentReminders(int landlordId, int limit) {
        ArrayList<Map<String, Object>> list = new ArrayList<>();
        int max = limit > 0 ? limit : 5;

        String sql = 
            "SELECT r.reminder_id, r.title, r.due_date, r.reminder_type, r.reminder_status, " +
            "COALESCE(t.full_name, 'General Property') AS tenant_name, " +
            "DATEDIFF(r.due_date, CURRENT_DATE) AS days_left " +
            "FROM reminders r " +
            "LEFT JOIN tenants t ON r.tenant_id = t.tenant_id " +
            "WHERE r.landlord_id = ? AND r.reminder_status NOT IN ('Completed', 'Resolved') " +
            "ORDER BY r.due_date ASC LIMIT ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, landlordId > 0 ? landlordId : 1);
            ps.setInt(2, max);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("reminderId", rs.getInt("reminder_id"));
                    map.put("title", rs.getString("title"));
                    map.put("dueDate", rs.getString("due_date"));
                    map.put("reminderType", rs.getString("reminder_type"));
                    map.put("reminderStatus", rs.getString("reminder_status"));
                    map.put("tenantName", rs.getString("tenant_name"));
                    map.put("daysLeft", rs.getInt("days_left"));
                    list.add(map);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DashboardDAO] getUrgentReminders error: " + e.getMessage());
        }

        return list;
    }
}
