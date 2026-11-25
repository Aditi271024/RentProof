package com.rentproof.dashboard;

import com.rentproof.database.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * DashboardDAO - Data Access Object for Landlord Dashboard analytics.
 * 
 * Reuses com.rentproof.database.DBConnection.
 * Strictly adheres to Core Java SE syllabus (Lecture 1 to 42):
 * - JDBC API (DriverManager, Connection, PreparedStatement, ResultSet)
 * - try-with-resources
 * - Standard iteration loops (no lambdas/streams)
 * - Safe parameterized queries preventing SQL injection
 */
public class DashboardDAO {

    /**
     * Computes high-level overview metrics for a landlord.
     * 
     * @param landlordId landlord ID
     * @return DashboardStats object populated from database
     */
    public DashboardStats getDashboardStats(int landlordId) {
        DashboardStats stats = new DashboardStats();

        // 1. Total Properties
        String propSql = "SELECT COUNT(*) FROM properties WHERE landlord_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(propSql)) {
            pstmt.setInt(1, landlordId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    stats.setTotalProperties(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DashboardDAO] Error fetching total properties: " + e.getMessage());
        }

        // 2. Total Rooms, Occupied Rooms, Vacant Rooms
        String roomSql = "SELECT COUNT(r.room_id) AS total_rooms, " +
                         "COUNT(CASE WHEN r.room_status = 'Occupied' THEN 1 END) AS occupied_rooms, " +
                         "COUNT(CASE WHEN r.room_status = 'Vacant' THEN 1 END) AS vacant_rooms " +
                         "FROM rooms r JOIN properties p ON r.property_id = p.property_id " +
                         "WHERE p.landlord_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(roomSql)) {
            pstmt.setInt(1, landlordId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    stats.setTotalRooms(rs.getInt("total_rooms"));
                    stats.setOccupiedRooms(rs.getInt("occupied_rooms"));
                    stats.setVacantRooms(rs.getInt("vacant_rooms"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DashboardDAO] Error fetching room stats: " + e.getMessage());
        }

        // 3. Total Active Tenants
        String tenantSql = "SELECT COUNT(t.tenant_id) " +
                           "FROM tenants t JOIN properties p ON t.property_id = p.property_id " +
                           "WHERE p.landlord_id = ? AND t.tenant_status = 'Active'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(tenantSql)) {
            pstmt.setInt(1, landlordId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    stats.setTotalTenants(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DashboardDAO] Error fetching total tenants: " + e.getMessage());
        }

        // 4. Rent Collected (Paid)
        String paidSql = "SELECT COALESCE(SUM(pm.amount), 0) " +
                         "FROM payments pm JOIN properties p ON pm.property_id = p.property_id " +
                         "WHERE p.landlord_id = ? AND pm.payment_status = 'Paid'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(paidSql)) {
            pstmt.setInt(1, landlordId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    stats.setRentCollected(rs.getDouble(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DashboardDAO] Error fetching rent collected: " + e.getMessage());
        }

        // 5. Pending Rent (Pending or Overdue)
        String pendingSql = "SELECT COALESCE(SUM(pm.amount), 0) " +
                            "FROM payments pm JOIN properties p ON pm.property_id = p.property_id " +
                            "WHERE p.landlord_id = ? AND pm.payment_status IN ('Pending', 'Overdue')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(pendingSql)) {
            pstmt.setInt(1, landlordId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    stats.setPendingRent(rs.getDouble(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("[DashboardDAO] Error fetching pending rent: " + e.getMessage());
        }

        return stats;
    }

    /**
     * Retrieves properties with their room inventory breakdown for this landlord.
     */
    public ArrayList<PropertyOverviewDTO> getPropertyOverview(int landlordId) {
        ArrayList<PropertyOverviewDTO> list = new ArrayList<>();
        String sql = "SELECT p.property_id, p.property_name, p.property_type, p.city, p.state, p.total_rooms, " +
                     "COUNT(CASE WHEN r.room_status = 'Occupied' THEN 1 END) AS occupied_rooms, " +
                     "COUNT(CASE WHEN r.room_status = 'Vacant' THEN 1 END) AS vacant_rooms " +
                     "FROM properties p " +
                     "LEFT JOIN rooms r ON p.property_id = r.property_id " +
                     "WHERE p.landlord_id = ? " +
                     "GROUP BY p.property_id, p.property_name, p.property_type, p.city, p.state, p.total_rooms " +
                     "ORDER BY p.property_id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, landlordId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String location = rs.getString("city") + ", " + rs.getString("state");
                    PropertyOverviewDTO dto = new PropertyOverviewDTO(
                            rs.getInt("property_id"),
                            rs.getString("property_name"),
                            rs.getString("property_type"),
                            location,
                            rs.getInt("total_rooms"),
                            rs.getInt("occupied_rooms"),
                            rs.getInt("vacant_rooms")
                    );
                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DashboardDAO] Error fetching property overview: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves recent payment records for landlord properties.
     */
    public ArrayList<PaymentRecordDTO> getRecentPayments(int landlordId, int limit) {
        ArrayList<PaymentRecordDTO> list = new ArrayList<>();
        String sql = "SELECT pm.payment_id, t.full_name AS tenant_name, p.property_name, " +
                     "pm.amount, pm.payment_date, pm.payment_month, pm.payment_status " +
                     "FROM payments pm " +
                     "JOIN tenants t ON pm.tenant_id = t.tenant_id " +
                     "JOIN properties p ON pm.property_id = p.property_id " +
                     "WHERE p.landlord_id = ? " +
                     "ORDER BY pm.payment_date DESC, pm.payment_id DESC LIMIT ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, landlordId);
            pstmt.setInt(2, limit > 0 ? limit : 5);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    PaymentRecordDTO dto = new PaymentRecordDTO(
                            rs.getInt("payment_id"),
                            rs.getString("tenant_name"),
                            rs.getString("property_name"),
                            rs.getDouble("amount"),
                            rs.getString("payment_date"),
                            rs.getString("payment_month"),
                            rs.getString("payment_status")
                    );
                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DashboardDAO] Error fetching recent payments: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves records where rent is currently pending or overdue.
     */
    public ArrayList<PaymentRecordDTO> getPendingRent(int landlordId) {
        ArrayList<PaymentRecordDTO> list = new ArrayList<>();
        String sql = "SELECT pm.payment_id, t.full_name AS tenant_name, p.property_name, " +
                     "pm.amount, pm.payment_date, pm.payment_month, pm.payment_status " +
                     "FROM payments pm " +
                     "JOIN tenants t ON pm.tenant_id = t.tenant_id " +
                     "JOIN properties p ON pm.property_id = p.property_id " +
                     "WHERE p.landlord_id = ? AND pm.payment_status IN ('Pending', 'Overdue') " +
                     "ORDER BY pm.payment_date ASC LIMIT 10";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, landlordId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    PaymentRecordDTO dto = new PaymentRecordDTO(
                            rs.getInt("payment_id"),
                            rs.getString("tenant_name"),
                            rs.getString("property_name"),
                            rs.getDouble("amount"),
                            rs.getString("payment_date"),
                            rs.getString("payment_month"),
                            rs.getString("payment_status")
                    );
                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DashboardDAO] Error fetching pending rent: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves upcoming reminders for this landlord.
     */
    public ArrayList<ReminderDTO> getUpcomingReminders(int landlordId, int limit) {
        ArrayList<ReminderDTO> list = new ArrayList<>();
        String sql = "SELECT r.reminder_id, r.title, r.due_date, r.reminder_type, r.reminder_status, " +
                     "COALESCE(t.full_name, 'General Property Task') AS related_to " +
                     "FROM reminders r " +
                     "LEFT JOIN tenants t ON r.tenant_id = t.tenant_id " +
                     "WHERE r.landlord_id = ? " +
                     "ORDER BY r.due_date ASC LIMIT ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, landlordId);
            pstmt.setInt(2, limit > 0 ? limit : 5);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ReminderDTO dto = new ReminderDTO(
                            rs.getInt("reminder_id"),
                            rs.getString("title"),
                            rs.getString("related_to"),
                            rs.getString("due_date"),
                            rs.getString("reminder_type"),
                            rs.getString("reminder_status")
                    );
                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DashboardDAO] Error fetching reminders: " + e.getMessage());
        }
        return list;
    }
}
