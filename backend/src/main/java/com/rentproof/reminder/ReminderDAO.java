package com.rentproof.reminder;

import com.rentproof.database.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;

/**
 * ReminderDAO handles Data Access Object operations for the 'reminders' table in MySQL.
 * 
 * Module: Rent Reminder Management Module
 * Developer: Gungun (BTech CSE)
 * 
 * Concepts used (Syllabus up to Lecture 42):
 * - JDBC API (Connection, PreparedStatement, ResultSet, Statement)
 * - Basic Collections (ArrayList)
 * - try-with-resources statement for automatic resource cleanup
 * - CRUD database operations (Create, Read, Update, Delete)
 * - Object-Relational Mapping (Mapping ResultSet rows to Reminder objects)
 * - SQL Queries with parameterized placeholders (?) to prevent SQL Injection
 * - Null handling with Types.INTEGER for nullable foreign keys
 */
public class ReminderDAO {

    /**
     * 1. Inserts a new reminder record into the MySQL 'reminders' table.
     * 
     * @param reminder Reminder object containing reminder details
     * @return generated reminder_id primary key if successful, or -1 on failure
     * @throws SQLException if a database access error occurs
     */
    public int addReminder(Reminder reminder) throws SQLException {
        String sql = "INSERT INTO reminders (landlord_id, tenant_id, title, message, due_date, reminder_type, reminder_status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, reminder.getLandlordId());

            // Handle nullable tenant_id
            if (reminder.getTenantId() != null && reminder.getTenantId() > 0) {
                pstmt.setInt(2, reminder.getTenantId());
            } else {
                pstmt.setNull(2, Types.INTEGER);
            }

            pstmt.setString(3, reminder.getTitle().trim());
            pstmt.setString(4, reminder.getMessage().trim());
            pstmt.setDate(5, reminder.getDueDate());
            pstmt.setString(6, reminder.getReminderType().trim());
            pstmt.setString(7, reminder.getReminderStatus().trim());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int generatedId = generatedKeys.getInt(1);
                        reminder.setReminderId(generatedId);
                        return generatedId;
                    }
                }
            }
            return -1;
        } catch (SQLException e) {
            System.err.println("[ReminderDAO] addReminder failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 2. Retrieves all reminders across the system (System Administrator / Debug view),
     * joining with the 'tenants' table to retrieve the tenant's full name.
     * 
     * @return ArrayList of Reminder objects
     */
    public ArrayList<Reminder> getAllReminders() {
        ArrayList<Reminder> list = new ArrayList<>();
        String sql = "SELECT r.reminder_id, r.landlord_id, r.tenant_id, t.full_name AS tenant_name, " +
                     "r.title, r.message, r.due_date, r.reminder_type, r.reminder_status, r.created_at " +
                     "FROM reminders r " +
                     "LEFT JOIN tenants t ON r.tenant_id = t.tenant_id " +
                     "ORDER BY r.due_date ASC, r.reminder_id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToReminder(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ReminderDAO] getAllReminders failed: " + e.getMessage());
        }
        return list;
    }

    /**
     * Retrieves all reminders owned by a specific landlord.
     * 
     * @param landlordId landlord ID
     * @return ArrayList of Reminder objects
     */
    public ArrayList<Reminder> getRemindersByLandlord(int landlordId) {
        ArrayList<Reminder> list = new ArrayList<>();
        String sql = "SELECT r.reminder_id, r.landlord_id, r.tenant_id, t.full_name AS tenant_name, " +
                     "r.title, r.message, r.due_date, r.reminder_type, r.reminder_status, r.created_at " +
                     "FROM reminders r " +
                     "LEFT JOIN tenants t ON r.tenant_id = t.tenant_id " +
                     "WHERE r.landlord_id = ? " +
                     "ORDER BY r.due_date ASC, r.reminder_id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, landlordId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToReminder(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ReminderDAO] getRemindersByLandlord failed: " + e.getMessage());
        }
        return list;
    }

    /**
     * 3. Finds a single reminder by its primary key ID.
     * 
     * @param reminderId primary key
     * @return Reminder object if found, or null
     */
    public Reminder getReminderById(int reminderId) {
        String sql = "SELECT r.reminder_id, r.landlord_id, r.tenant_id, t.full_name AS tenant_name, " +
                     "r.title, r.message, r.due_date, r.reminder_type, r.reminder_status, r.created_at " +
                     "FROM reminders r " +
                     "LEFT JOIN tenants t ON r.tenant_id = t.tenant_id " +
                     "WHERE r.reminder_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, reminderId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToReminder(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ReminderDAO] getReminderById failed: " + e.getMessage());
        }
        return null;
    }

    /**
     * 4. Displays all reminders assigned to a particular tenant.
     * 
     * @param tenantId tenant ID
     * @return ArrayList of Reminder objects
     */
    public ArrayList<Reminder> getRemindersByTenant(int tenantId) {
        ArrayList<Reminder> list = new ArrayList<>();
        String sql = "SELECT r.reminder_id, r.landlord_id, r.tenant_id, t.full_name AS tenant_name, " +
                     "r.title, r.message, r.due_date, r.reminder_type, r.reminder_status, r.created_at " +
                     "FROM reminders r " +
                     "LEFT JOIN tenants t ON r.tenant_id = t.tenant_id " +
                     "WHERE r.tenant_id = ? " +
                     "ORDER BY r.due_date ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, tenantId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToReminder(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ReminderDAO] getRemindersByTenant failed: " + e.getMessage());
        }
        return list;
    }

    /**
     * 5. Displays all pending reminders for a landlord (or globally if landlordId <= 0).
     * 
     * @param landlordId landlord ID (pass 0 for all landlords)
     * @return ArrayList of pending Reminder objects
     */
    public ArrayList<Reminder> getPendingReminders(int landlordId) {
        ArrayList<Reminder> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT r.reminder_id, r.landlord_id, r.tenant_id, t.full_name AS tenant_name, " +
            "r.title, r.message, r.due_date, r.reminder_type, r.reminder_status, r.created_at " +
            "FROM reminders r " +
            "LEFT JOIN tenants t ON r.tenant_id = t.tenant_id " +
            "WHERE r.reminder_status = 'Pending' "
        );

        if (landlordId > 0) {
            sql.append("AND r.landlord_id = ? ");
        }
        sql.append("ORDER BY r.due_date ASC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            if (landlordId > 0) {
                pstmt.setInt(1, landlordId);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToReminder(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ReminderDAO] getPendingReminders failed: " + e.getMessage());
        }
        return list;
    }

    /**
     * Overloaded convenience method to get all pending reminders across the database.
     * 
     * @return ArrayList of pending Reminder objects
     */
    public ArrayList<Reminder> getPendingReminders() {
        return getPendingReminders(0);
    }

    /**
     * 6. Updates the status of an existing reminder (e.g. from 'Pending' to 'Completed' or 'Sent').
     * 
     * @param reminderId primary key of the reminder
     * @param newStatus  new status string
     * @return true if updated successfully, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean updateReminderStatus(int reminderId, String newStatus) throws SQLException {
        String sql = "UPDATE reminders SET reminder_status = ? WHERE reminder_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus.trim());
            pstmt.setInt(2, reminderId);

            int rowsUpdated = pstmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            System.err.println("[ReminderDAO] updateReminderStatus failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Updates all editable fields of an existing reminder.
     * 
     * @param reminder Reminder object with updated fields
     * @return true if updated successfully, false otherwise
     * @throws SQLException if a database error occurs
     */
    public boolean updateReminder(Reminder reminder) throws SQLException {
        String sql = "UPDATE reminders SET tenant_id = ?, title = ?, message = ?, due_date = ?, " +
                     "reminder_type = ?, reminder_status = ? WHERE reminder_id = ? AND landlord_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (reminder.getTenantId() != null && reminder.getTenantId() > 0) {
                pstmt.setInt(1, reminder.getTenantId());
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }

            pstmt.setString(2, reminder.getTitle().trim());
            pstmt.setString(3, reminder.getMessage().trim());
            pstmt.setDate(4, reminder.getDueDate());
            pstmt.setString(5, reminder.getReminderType().trim());
            pstmt.setString(6, reminder.getReminderStatus().trim());
            pstmt.setInt(7, reminder.getReminderId());
            pstmt.setInt(8, reminder.getLandlordId());

            int rowsUpdated = pstmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            System.err.println("[ReminderDAO] updateReminder failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 7. Deletes a reminder record by primary key ID.
     * 
     * @param reminderId primary key to delete
     * @return true if deleted, false if record was not found
     * @throws SQLException if a database error occurs
     */
    public boolean deleteReminder(int reminderId) throws SQLException {
        String sql = "DELETE FROM reminders WHERE reminder_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, reminderId);
            int rowsDeleted = pstmt.executeUpdate();
            return rowsDeleted > 0;
        } catch (SQLException e) {
            System.err.println("[ReminderDAO] deleteReminder failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 8. Searches reminders for a landlord matching a search keyword
     * against title, message, reminder_type, or tenant full name.
     * 
     * @param landlordId landlord identifier (or 0 for all)
     * @param keyword    search string
     * @return ArrayList of matching Reminder objects
     */
    public ArrayList<Reminder> searchReminders(int landlordId, String keyword) {
        ArrayList<Reminder> list = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            return landlordId > 0 ? getRemindersByLandlord(landlordId) : getAllReminders();
        }

        String searchPattern = "%" + keyword.trim().toLowerCase() + "%";
        StringBuilder sql = new StringBuilder(
            "SELECT r.reminder_id, r.landlord_id, r.tenant_id, t.full_name AS tenant_name, " +
            "r.title, r.message, r.due_date, r.reminder_type, r.reminder_status, r.created_at " +
            "FROM reminders r " +
            "LEFT JOIN tenants t ON r.tenant_id = t.tenant_id " +
            "WHERE (LOWER(r.title) LIKE ? " +
            "   OR LOWER(r.message) LIKE ? " +
            "   OR LOWER(r.reminder_type) LIKE ? " +
            "   OR LOWER(r.reminder_status) LIKE ? " +
            "   OR LOWER(COALESCE(t.full_name, '')) LIKE ?) "
        );

        if (landlordId > 0) {
            sql.append("AND r.landlord_id = ? ");
        }
        sql.append("ORDER BY r.due_date ASC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);
            pstmt.setString(4, searchPattern);
            pstmt.setString(5, searchPattern);

            if (landlordId > 0) {
                pstmt.setInt(6, landlordId);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToReminder(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ReminderDAO] searchReminders failed: " + e.getMessage());
        }
        return list;
    }

    /**
     * Loads active tenants from the database for populating tenant dropdown options.
     * Meets Requirement 5: "Do not hardcode tenant records in the final implementation. Load tenants from the database."
     * 
     * @param landlordId landlord ID to filter tenants (or 0 for all active tenants)
     * @return ArrayList of TenantSummary records
     */
    public ArrayList<TenantSummary> getActiveTenants(int landlordId) {
        ArrayList<TenantSummary> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT t.tenant_id, t.full_name, t.phone, p.property_name, r.room_number " +
            "FROM tenants t " +
            "JOIN properties p ON t.property_id = p.property_id " +
            "JOIN rooms r ON t.room_id = r.room_id " +
            "WHERE t.tenant_status = 'Active' "
        );

        if (landlordId > 0) {
            sql.append("AND p.landlord_id = ? ");
        }
        sql.append("ORDER BY t.full_name ASC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {

            if (landlordId > 0) {
                pstmt.setInt(1, landlordId);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new TenantSummary(
                        rs.getInt("tenant_id"),
                        rs.getString("full_name"),
                        rs.getString("phone"),
                        rs.getString("property_name"),
                        rs.getString("room_number")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ReminderDAO] getActiveTenants failed: " + e.getMessage());
        }
        return list;
    }

    /**
     * Helper method to map a single ResultSet record to a Reminder model object.
     * 
     * @param rs active ResultSet positioned at the current row
     * @return populated Reminder object
     * @throws SQLException on column access error
     */
    private Reminder mapRowToReminder(ResultSet rs) throws SQLException {
        int reminderId = rs.getInt("reminder_id");
        int landlordId = rs.getInt("landlord_id");

        int tenantIdVal = rs.getInt("tenant_id");
        Integer tenantId = rs.wasNull() ? null : tenantIdVal;

        String tenantName = null;
        try {
            tenantName = rs.getString("tenant_name");
        } catch (SQLException ignored) {
            // In case the query was executed without JOIN
        }

        String title = rs.getString("title");
        String message = rs.getString("message");
        Date dueDate = rs.getDate("due_date");
        String reminderType = rs.getString("reminder_type");
        String reminderStatus = rs.getString("reminder_status");
        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");

        return new Reminder(
            reminderId,
            landlordId,
            tenantId,
            tenantName,
            title,
            message,
            dueDate,
            reminderType,
            reminderStatus,
            createdAt
        );
    }

    /**
     * Lightweight inner helper model representing an active tenant summary.
     */
    public static class TenantSummary {
        private final int tenantId;
        private final String fullName;
        private final String phone;
        private final String propertyName;
        private final String roomNumber;

        public TenantSummary(int tenantId, String fullName, String phone, String propertyName, String roomNumber) {
            this.tenantId = tenantId;
            this.fullName = fullName;
            this.phone = phone;
            this.propertyName = propertyName;
            this.roomNumber = roomNumber;
        }

        public int getTenantId() {
            return tenantId;
        }

        public String getFullName() {
            return fullName;
        }

        public String getPhone() {
            return phone;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public String getRoomNumber() {
            return roomNumber;
        }

        @Override
        public String toString() {
            return fullName + " (" + propertyName + " - Room " + roomNumber + ")";
        }
    }
}
