package com.rentproof.tenant;

import com.rentproof.database.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * TenantDAO handles all Data Access Object operations for the 'tenants' table
 * and coordinates room status updates with the 'rooms' table.
 * 
 * Part of Shweta's Tenant Management & Property Assignment module.
 * 
 * Concepts used (Syllabus up to Lecture 42):
 * - JDBC API: Connection, PreparedStatement, Statement, ResultSet
 * - try-with-resources for automatic resource closing (clean memory and socket management)
 * - Parameterized SQL queries using ? placeholders (SQL injection defense)
 * - Object-Relational Mapping (ResultSet to Tenant object)
 * - Collection Framework: ArrayList for in-memory record storage
 * - Exception Handling: SQLException propagation and logging
 */
public class TenantDAO {

    /**
     * Inserts a new tenant into the MySQL database and sets the assigned room status to 'Occupied'.
     * 
     * @param tenant Tenant object containing registration details
     * @return generated tenant_id, or -1 on failure
     * @throws SQLException on database error
     */
    public int addTenant(Tenant tenant) throws SQLException {
        String insertSql = "INSERT INTO tenants (property_id, room_id, full_name, email, phone, " +
                           "emergency_contact, permanent_address, id_proof_number, move_in_date, monthly_rent, security_deposit, tenant_status) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        String updateRoomSql = "UPDATE rooms SET room_status = 'Occupied' "
                             + "WHERE room_id = ? AND room_status = 'Vacant'";

        Connection conn = null;
        PreparedStatement pstmt = null;
        PreparedStatement updateRoomStmt = null;
        ResultSet generatedKeys = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction to guarantee atomicity

            pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setInt(1, tenant.getPropertyId());
            pstmt.setInt(2, tenant.getRoomId());
            pstmt.setString(3, tenant.getFullName().trim());
            pstmt.setString(4, tenant.getEmail().trim());
            pstmt.setString(5, tenant.getPhone().trim());
            pstmt.setString(6, tenant.getEmergencyContact() != null ? tenant.getEmergencyContact().trim() : null);
            pstmt.setString(7, tenant.getPermanentAddress());
            pstmt.setString(8, tenant.getIdProofNumber());
            pstmt.setDate(9, tenant.getMoveInDate());
            pstmt.setDouble(10, tenant.getMonthlyRent());
            pstmt.setDouble(11, tenant.getSecurityDeposit());
            pstmt.setString(12, tenant.getTenantStatus() != null ? tenant.getTenantStatus() : "Active");

            int affectedRows = pstmt.executeUpdate();
            int generatedId = -1;

            if (affectedRows > 0) {
                generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    generatedId = generatedKeys.getInt(1);
                    tenant.setTenantId(generatedId);
                }
                if (generatedId <= 0) {
                    throw new SQLException("Tenant insert did not return a generated tenant ID.");
                }

                // Active and pending tenants both reserve their assigned room.
                if ("Active".equalsIgnoreCase(tenant.getTenantStatus())
                        || "Pending".equalsIgnoreCase(tenant.getTenantStatus())) {
                    updateRoomStmt = conn.prepareStatement(updateRoomSql);
                    updateRoomStmt.setInt(1, tenant.getRoomId());
                    if (updateRoomStmt.executeUpdate() != 1) {
                        throw new SQLException("The selected room is no longer vacant.", "45000");
                    }
                }

                conn.commit(); // Transaction successful
                return generatedId;
            } else {
                throw new SQLException("Tenant insert did not create a record.");
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("[TenantDAO] Rollback failed: " + ex.getMessage());
                }
            }
            System.err.println("[TenantDAO] addTenant failed: " + e.getMessage());
            throw e;
        } finally {
            if (generatedKeys != null) try { generatedKeys.close(); } catch (SQLException ignored) {}
            if (pstmt != null) try { pstmt.close(); } catch (SQLException ignored) {}
            if (updateRoomStmt != null) try { updateRoomStmt.close(); } catch (SQLException ignored) {}
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    public boolean roomBelongsToProperty(int roomId, int propertyId) throws SQLException {
        String sql = "SELECT 1 FROM rooms WHERE room_id = ? AND property_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, roomId);
            pstmt.setInt(2, propertyId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Retrieves all tenants in the system, joined with property and room details.
     * 
     * @return ArrayList of Tenant objects
     */
    public ArrayList<Tenant> getAllTenants() {
        ArrayList<Tenant> list = new ArrayList<>();
        String sql = "SELECT t.tenant_id, t.property_id, t.room_id, t.full_name, t.email, t.phone, " +
                     "t.emergency_contact, t.permanent_address, t.id_proof_number, t.move_in_date, t.move_out_date, " +
                     "t.security_deposit, t.tenant_status, t.created_at, " +
                     "p.property_name, r.room_number, COALESCE(t.monthly_rent, r.monthly_rent) AS monthly_rent " +
                     "FROM tenants t " +
                     "LEFT JOIN properties p ON t.property_id = p.property_id " +
                     "LEFT JOIN rooms r ON t.room_id = r.room_id " +
                     "ORDER BY t.tenant_id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToTenant(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not retrieve tenants from the database.", e);
        }
        return list;
    }

    /**
     * Retrieves a single tenant by their primary key ID.
     * 
     * @param tenantId unique tenant ID
     * @return Tenant object if found, or null
     */
    public Tenant getTenantById(int tenantId) {
        String sql = "SELECT t.tenant_id, t.property_id, t.room_id, t.full_name, t.email, t.phone, " +
                     "t.emergency_contact, t.permanent_address, t.id_proof_number, t.move_in_date, t.move_out_date, " +
                     "t.security_deposit, t.tenant_status, t.created_at, " +
                     "p.property_name, r.room_number, COALESCE(t.monthly_rent, r.monthly_rent) AS monthly_rent " +
                     "FROM tenants t " +
                     "LEFT JOIN properties p ON t.property_id = p.property_id " +
                     "LEFT JOIN rooms r ON t.room_id = r.room_id " +
                     "WHERE t.tenant_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, tenantId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToTenant(rs);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not retrieve tenant " + tenantId + " from the database.", e);
        }
        return null;
    }

    /**
     * Updates an existing tenant's profile and synchronizes room occupancy status.
     * If the tenant is moved out, the room status is reset to 'Vacant'.
     * 
     * @param tenant Tenant object with updated information
     * @return true if updated successfully, false otherwise
     */
    public boolean updateTenant(Tenant tenant) {
        String updateSql = "UPDATE tenants SET full_name = ?, email = ?, phone = ?, " +
                           "emergency_contact = ?, permanent_address = ?, id_proof_number = ?, monthly_rent = ?, security_deposit = ?, " +
                           "tenant_status = ?, move_out_date = ? " +
                           "WHERE tenant_id = ?";

        String occupyRoomSql = "UPDATE rooms SET room_status = 'Occupied' WHERE room_id = ?";
        String vacateRoomSql = "UPDATE rooms SET room_status = 'Vacant' WHERE room_id = ? "
                + "AND NOT EXISTS (SELECT 1 FROM tenants other_tenant "
                + "WHERE other_tenant.room_id = rooms.room_id AND other_tenant.tenant_id <> ? "
                + "AND other_tenant.tenant_status IN ('Active', 'Pending'))";

        Connection conn = null;
        PreparedStatement pstmt = null;
        PreparedStatement roomStmt = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            pstmt = conn.prepareStatement(updateSql);
            pstmt.setString(1, tenant.getFullName().trim());
            pstmt.setString(2, tenant.getEmail().trim());
            pstmt.setString(3, tenant.getPhone().trim());
            pstmt.setString(4, tenant.getEmergencyContact() != null ? tenant.getEmergencyContact().trim() : null);
            pstmt.setString(5, tenant.getPermanentAddress());
            pstmt.setString(6, tenant.getIdProofNumber());
            pstmt.setDouble(7, tenant.getMonthlyRent());
            pstmt.setDouble(8, tenant.getSecurityDeposit());
            pstmt.setString(9, tenant.getTenantStatus());
            pstmt.setDate(10, tenant.getMoveOutDate());
            pstmt.setInt(11, tenant.getTenantId());

            int affected = pstmt.executeUpdate();

            // Synchronize room status if tenant status is 'Moved Out'
            if (affected > 0 && tenant.getRoomId() > 0) {
                if ("Moved Out".equalsIgnoreCase(tenant.getTenantStatus())) {
                    roomStmt = conn.prepareStatement(vacateRoomSql);
                    roomStmt.setInt(1, tenant.getRoomId());
                    roomStmt.setInt(2, tenant.getTenantId());
                } else if ("Active".equalsIgnoreCase(tenant.getTenantStatus())
                        || "Pending".equalsIgnoreCase(tenant.getTenantStatus())) {
                    roomStmt = conn.prepareStatement(occupyRoomSql);
                    roomStmt.setInt(1, tenant.getRoomId());
                } else {
                    throw new SQLException("Unsupported tenant status.", "45000");
                }
                roomStmt.executeUpdate();
            }

            conn.commit();
            return affected > 0;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            System.err.println("[TenantDAO] updateTenant failed: " + e.getMessage());
            return false;
        } finally {
            if (pstmt != null) try { pstmt.close(); } catch (SQLException ignored) {}
            if (roomStmt != null) try { roomStmt.close(); } catch (SQLException ignored) {}
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Deletes a tenant record and resets their occupied room to 'Vacant'.
     * 
     * @param tenantId ID of the tenant to delete
     * @return true if deleted successfully, false otherwise
     */
    public boolean deleteTenant(int tenantId) {
        // Retrieve tenant first to identify their assigned room
        Tenant existing = getTenantById(tenantId);
        if (existing == null) {
            return false;
        }

        String deleteSql = "DELETE FROM tenants WHERE tenant_id = ?";
        String freeRoomSql = "UPDATE rooms SET room_status = 'Vacant' WHERE room_id = ? "
                + "AND NOT EXISTS (SELECT 1 FROM tenants other_tenant "
                + "WHERE other_tenant.room_id = rooms.room_id "
                + "AND other_tenant.tenant_status IN ('Active', 'Pending'))";

        Connection conn = null;
        PreparedStatement deleteStmt = null;
        PreparedStatement roomStmt = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            deleteStmt = conn.prepareStatement(deleteSql);
            deleteStmt.setInt(1, tenantId);
            int affected = deleteStmt.executeUpdate();

            if (affected > 0 && existing.getRoomId() > 0) {
                roomStmt = conn.prepareStatement(freeRoomSql);
                roomStmt.setInt(1, existing.getRoomId());
                roomStmt.executeUpdate();
            }

            conn.commit();
            return affected > 0;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            System.err.println("[TenantDAO] deleteTenant failed: " + e.getMessage());
            return false;
        } finally {
            if (deleteStmt != null) try { deleteStmt.close(); } catch (SQLException ignored) {}
            if (roomStmt != null) try { roomStmt.close(); } catch (SQLException ignored) {}
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Assigns a tenant to a property and room.
     * Enforces the business rule: Target room MUST be Vacant before assignment.
     * If tenant was previously in another room, previous room is freed to 'Vacant'.
     * 
     * @param tenantId target tenant ID
     * @param propertyId property ID
     * @param roomId room ID
     * @param monthlyRent monthly rent agreed
     * @param moveInDate date of moving in
     * @return true if assignment succeeded, false if room is occupied or update failed
     */
    public boolean assignProperty(int tenantId, int propertyId, int roomId, double monthlyRent, Date moveInDate) {
        try {
            if (!roomBelongsToProperty(roomId, propertyId)) {
                System.err.println("[TenantDAO] Cannot assign room ID " + roomId
                        + " because it does not belong to property ID " + propertyId + ".");
                return false;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not verify the room/property assignment.", e);
        }

        Tenant existing = getTenantById(tenantId);
        if (existing == null) {
            System.err.println("[TenantDAO] Cannot assign property: Tenant ID " + tenantId + " not found.");
            return false;
        }

        int previousRoomId = existing.getRoomId();
        boolean sameActiveAssignment = previousRoomId == roomId
                && existing.getPropertyId() == propertyId
                && ("Active".equalsIgnoreCase(existing.getTenantStatus())
                    || "Pending".equalsIgnoreCase(existing.getTenantStatus()));
        if (sameActiveAssignment) {
            String updateRentSql = "UPDATE tenants SET monthly_rent = ?, move_in_date = ? WHERE tenant_id = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(updateRentSql)) {
                stmt.setDouble(1, monthlyRent);
                stmt.setDate(2, moveInDate != null ? moveInDate : new Date(System.currentTimeMillis()));
                stmt.setInt(3, tenantId);
                return stmt.executeUpdate() == 1;
            } catch (SQLException e) {
                throw new IllegalStateException("Could not update the existing tenant assignment.", e);
            }
        }

        if (!isRoomVacant(roomId)) {
            System.err.println("[TenantDAO] Cannot assign room ID " + roomId + ": Room is already Occupied or in Maintenance.");
            return false;
        }

        String assignSql = "UPDATE tenants SET property_id = ?, room_id = ?, monthly_rent = ?, move_in_date = ?, " +
                           "tenant_status = 'Active' WHERE tenant_id = ?";
        String occupyNewRoomSql = "UPDATE rooms SET room_status = 'Occupied' "
                                + "WHERE room_id = ? AND room_status = 'Vacant'";
        String freeOldRoomSql = "UPDATE rooms SET room_status = 'Vacant' WHERE room_id = ? "
                + "AND NOT EXISTS (SELECT 1 FROM tenants other_tenant "
                + "WHERE other_tenant.room_id = rooms.room_id "
                + "AND other_tenant.tenant_status IN ('Active', 'Pending'))";

        Connection conn = null;
        PreparedStatement assignStmt = null;
        PreparedStatement occupyStmt = null;
        PreparedStatement freeStmt = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Reserve the destination room before changing the tenant assignment.
            occupyStmt = conn.prepareStatement(occupyNewRoomSql);
            occupyStmt.setInt(1, roomId);
            if (occupyStmt.executeUpdate() != 1) {
                conn.rollback();
                return false;
            }

            assignStmt = conn.prepareStatement(assignSql);
            assignStmt.setInt(1, propertyId);
            assignStmt.setInt(2, roomId);
            assignStmt.setDouble(3, monthlyRent);
            assignStmt.setDate(4, moveInDate != null ? moveInDate : new Date(System.currentTimeMillis()));
            assignStmt.setInt(5, tenantId);
            if (assignStmt.executeUpdate() != 1) {
                conn.rollback();
                return false;
            }

            // Free the previous room only after the new assignment succeeds.
            if (previousRoomId > 0 && previousRoomId != roomId) {
                freeStmt = conn.prepareStatement(freeOldRoomSql);
                freeStmt.setInt(1, previousRoomId);
                freeStmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw new IllegalStateException("Could not assign the tenant to the selected room.", e);
        } finally {
            if (assignStmt != null) try { assignStmt.close(); } catch (SQLException ignored) {}
            if (occupyStmt != null) try { occupyStmt.close(); } catch (SQLException ignored) {}
            if (freeStmt != null) try { freeStmt.close(); } catch (SQLException ignored) {}
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Retrieves all tenants residing in a specific property.
     * 
     * @param propertyId property ID
     * @return ArrayList of Tenant objects
     */
    public ArrayList<Tenant> getTenantsByProperty(int propertyId) {
        ArrayList<Tenant> list = new ArrayList<>();
        String sql = "SELECT t.tenant_id, t.property_id, t.room_id, t.full_name, t.email, t.phone, " +
                     "t.emergency_contact, t.permanent_address, t.id_proof_number, t.move_in_date, t.move_out_date, " +
                     "t.security_deposit, t.tenant_status, t.created_at, " +
                     "p.property_name, r.room_number, COALESCE(t.monthly_rent, r.monthly_rent) AS monthly_rent " +
                     "FROM tenants t " +
                     "LEFT JOIN properties p ON t.property_id = p.property_id " +
                     "LEFT JOIN rooms r ON t.room_id = r.room_id " +
                     "WHERE t.property_id = ? " +
                     "ORDER BY t.tenant_id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, propertyId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToTenant(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not retrieve tenants for the selected property.", e);
        }
        return list;
    }

    /**
     * Checks if a room has 'Vacant' status.
     * 
     * @param roomId room ID to verify
     * @return true if vacant, false if occupied/maintenance/non-existent
     */
    public boolean isRoomVacant(int roomId) {
        String sql = "SELECT room_status FROM rooms WHERE room_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, roomId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String status = rs.getString("room_status");
                    return "Vacant".equalsIgnoreCase(status);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not verify room availability.", e);
        }
        return false;
    }

    /**
     * Helper method to map a ResultSet row to a Tenant entity.
     */
    private Tenant mapRowToTenant(ResultSet rs) throws SQLException {
        Tenant tenant = new Tenant();
        tenant.setTenantId(rs.getInt("tenant_id"));
        tenant.setPropertyId(rs.getInt("property_id"));
        tenant.setRoomId(rs.getInt("room_id"));
        tenant.setFullName(rs.getString("full_name"));
        tenant.setEmail(rs.getString("email"));
        tenant.setPhone(rs.getString("phone"));
        tenant.setEmergencyContact(rs.getString("emergency_contact"));
        tenant.setPermanentAddress(rs.getString("permanent_address"));
        tenant.setIdProofNumber(rs.getString("id_proof_number"));
        tenant.setMoveInDate(rs.getDate("move_in_date"));
        tenant.setMoveOutDate(rs.getDate("move_out_date"));
        tenant.setSecurityDeposit(rs.getDouble("security_deposit"));
        tenant.setTenantStatus(rs.getString("tenant_status"));
        tenant.setCreatedAt(rs.getTimestamp("created_at"));

        // Join display columns
        tenant.setPropertyName(rs.getString("property_name"));
        tenant.setRoomNumber(rs.getString("room_number"));
        tenant.setMonthlyRent(rs.getDouble("monthly_rent"));

        return tenant;
    }
}
