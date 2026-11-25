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
 * - try-with-resources for automatic resource closing (clean memory and socket
 * management)
 * - Parameterized SQL queries using ? placeholders (SQL injection defense)
 * - Object-Relational Mapping (ResultSet to Tenant object)
 * - Collection Framework: ArrayList for in-memory record storage
 * - Exception Handling: SQLException propagation and logging
 */
public class TenantDAO {

    /**
     * Inserts a new tenant into the MySQL database and sets the assigned room
     * status to 'Occupied'.
     * 
     * @param tenant Tenant object containing registration details
     * @return generated tenant_id, or -1 on failure
     * @throws SQLException on database error
     */
    public int addTenant(Tenant tenant) throws SQLException {
        String insertSql = "INSERT INTO tenants (property_id, room_id, full_name, email, phone, " +
                "emergency_contact, id_proof_number, move_in_date, monthly_rent, security_deposit, tenant_status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        String updateRoomSql = "UPDATE rooms SET room_status = 'Occupied' WHERE room_id = ?";

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
            // Map permanentAddress into id_proof_number / address metadata
            String idOrAddress = tenant.getPermanentAddress() != null ? tenant.getPermanentAddress().trim()
                    : tenant.getIdProofNumber();
            pstmt.setString(7, idOrAddress);
            pstmt.setDate(8, tenant.getMoveInDate());
            pstmt.setDouble(9, tenant.getMonthlyRent());
            pstmt.setDouble(10, tenant.getSecurityDeposit());
            pstmt.setString(11, tenant.getTenantStatus() != null ? tenant.getTenantStatus() : "Active");

            int affectedRows = pstmt.executeUpdate();
            int generatedId = -1;

            if (affectedRows > 0) {
                generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    generatedId = generatedKeys.getInt(1);
                    tenant.setTenantId(generatedId);
                }

                // If tenant is marked Active, update room occupancy to 'Occupied'
                if ("Active".equalsIgnoreCase(tenant.getTenantStatus())) {
                    updateRoomStmt = conn.prepareStatement(updateRoomSql);
                    updateRoomStmt.setInt(1, tenant.getRoomId());
                    updateRoomStmt.executeUpdate();
                }

                conn.commit(); // Transaction successful
                return generatedId;
            } else {
                conn.rollback();
                return -1;
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
            if (generatedKeys != null)
                try {
                    generatedKeys.close();
                } catch (SQLException ignored) {
                }
            if (pstmt != null)
                try {
                    pstmt.close();
                } catch (SQLException ignored) {
                }
            if (updateRoomStmt != null)
                try {
                    updateRoomStmt.close();
                } catch (SQLException ignored) {
                }
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {
                }
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
                "t.emergency_contact, t.id_proof_number, t.move_in_date, t.move_out_date, " +
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
            System.err.println("[TenantDAO] getAllTenants failed: " + e.getMessage());
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
                "t.emergency_contact, t.id_proof_number, t.move_in_date, t.move_out_date, " +
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
            System.err.println("[TenantDAO] getTenantById failed for ID " + tenantId + ": " + e.getMessage());
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
                "emergency_contact = ?, id_proof_number = ?, monthly_rent = ?, tenant_status = ?, move_out_date = ? " +
                "WHERE tenant_id = ?";

        String roomStatusSql = "UPDATE rooms SET room_status = ? WHERE room_id = ?";

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
            String idOrAddress = tenant.getPermanentAddress() != null ? tenant.getPermanentAddress().trim()
                    : tenant.getIdProofNumber();
            pstmt.setString(5, idOrAddress);
            pstmt.setDouble(6, tenant.getMonthlyRent());
            pstmt.setString(7, tenant.getTenantStatus());
            pstmt.setDate(8, tenant.getMoveOutDate());
            pstmt.setInt(9, tenant.getTenantId());

            int affected = pstmt.executeUpdate();

            // Synchronize room status if tenant status is 'Moved Out'
            if (affected > 0 && tenant.getRoomId() > 0) {
                roomStmt = conn.prepareStatement(roomStatusSql);
                if ("Moved Out".equalsIgnoreCase(tenant.getTenantStatus())) {
                    roomStmt.setString(1, "Vacant");
                } else if ("Active".equalsIgnoreCase(tenant.getTenantStatus())) {
                    roomStmt.setString(1, "Occupied");
                } else {
                    roomStmt.setString(1, "Vacant");
                }
                roomStmt.setInt(2, tenant.getRoomId());
                roomStmt.executeUpdate();
            }

            conn.commit();
            return affected > 0;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            System.err.println("[TenantDAO] updateTenant failed: " + e.getMessage());
            return false;
        } finally {
            if (pstmt != null)
                try {
                    pstmt.close();
                } catch (SQLException ignored) {
                }
            if (roomStmt != null)
                try {
                    roomStmt.close();
                } catch (SQLException ignored) {
                }
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {
                }
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
        String freeRoomSql = "UPDATE rooms SET room_status = 'Vacant' WHERE room_id = ?";

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
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            System.err.println("[TenantDAO] deleteTenant failed: " + e.getMessage());
            return false;
        } finally {
            if (deleteStmt != null)
                try {
                    deleteStmt.close();
                } catch (SQLException ignored) {
                }
            if (roomStmt != null)
                try {
                    roomStmt.close();
                } catch (SQLException ignored) {
                }
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    /**
     * Assigns a tenant to a property and room.
     * Enforces the business rule: Target room MUST be Vacant before assignment.
     * If tenant was previously in another room, previous room is freed to 'Vacant'.
     * 
     * @param tenantId    target tenant ID
     * @param propertyId  property ID
     * @param roomId      room ID
     * @param monthlyRent monthly rent agreed
     * @param moveInDate  date of moving in
     * @return true if assignment succeeded, false if room is occupied or update
     *         failed
     */
    public boolean assignProperty(int tenantId, int propertyId, int roomId, double monthlyRent, Date moveInDate) {
        try {
            if (!roomBelongsToProperty(roomId, propertyId)) {
                System.err.println("[TenantDAO] Cannot assign room ID " + roomId
                        + " because it does not belong to property ID " + propertyId + ".");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("[TenantDAO] Could not verify room/property assignment: " + e.getMessage());
            return false;
        }

        // Check if destination room is vacant
        if (!isRoomVacant(roomId)) {
            System.err.println(
                    "[TenantDAO] Cannot assign room ID " + roomId + ": Room is already Occupied or in Maintenance.");
            return false;
        }

        Tenant existing = getTenantById(tenantId);
        if (existing == null) {
            System.err.println("[TenantDAO] Cannot assign property: Tenant ID " + tenantId + " not found.");
            return false;
        }

        int previousRoomId = existing.getRoomId();

        String assignSql = "UPDATE tenants SET property_id = ?, room_id = ?, monthly_rent = ?, move_in_date = ?, " +
                "tenant_status = 'Active' WHERE tenant_id = ?";
        String occupyNewRoomSql = "UPDATE rooms SET room_status = 'Occupied' WHERE room_id = ?";
        String freeOldRoomSql = "UPDATE rooms SET room_status = 'Vacant' WHERE room_id = ?";

        Connection conn = null;
        PreparedStatement assignStmt = null;
        PreparedStatement occupyStmt = null;
        PreparedStatement freeStmt = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Update Tenant record with assigned property, room, and negotiated monthly
            // rent
            assignStmt = conn.prepareStatement(assignSql);
            assignStmt.setInt(1, propertyId);
            assignStmt.setInt(2, roomId);
            assignStmt.setDouble(3, monthlyRent);
            assignStmt.setDate(4, moveInDate != null ? moveInDate : new Date(System.currentTimeMillis()));
            assignStmt.setInt(5, tenantId);
            assignStmt.executeUpdate();

            // 2. Mark new room Occupied (preserving original room default rent)
            occupyStmt = conn.prepareStatement(occupyNewRoomSql);
            occupyStmt.setInt(1, roomId);
            occupyStmt.executeUpdate();

            // 3. If previous room was different, mark it Vacant
            if (previousRoomId > 0 && previousRoomId != roomId) {
                freeStmt = conn.prepareStatement(freeOldRoomSql);
                freeStmt.setInt(1, previousRoomId);
                freeStmt.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            System.err.println("[TenantDAO] assignProperty transaction failed: " + e.getMessage());
            return false;
        } finally {
            if (assignStmt != null)
                try {
                    assignStmt.close();
                } catch (SQLException ignored) {
                }
            if (occupyStmt != null)
                try {
                    occupyStmt.close();
                } catch (SQLException ignored) {
                }
            if (freeStmt != null)
                try {
                    freeStmt.close();
                } catch (SQLException ignored) {
                }
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {
                }
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
                "t.emergency_contact, t.id_proof_number, t.move_in_date, t.move_out_date, " +
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
            System.err.println("[TenantDAO] getTenantsByProperty failed: " + e.getMessage());
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
            System.err.println("[TenantDAO] isRoomVacant check failed: " + e.getMessage());
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
        tenant.setIdProofNumber(rs.getString("id_proof_number"));
        tenant.setPermanentAddress(rs.getString("id_proof_number"));
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