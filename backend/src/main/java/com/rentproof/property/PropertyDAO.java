package com.rentproof.property;

import com.rentproof.database.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * PropertyDAO handles Data Access Object operations for the 'properties' table.
 * 
 * Concepts used (Syllabus up to Lecture 42):
 * - JDBC API (Connection, PreparedStatement, ResultSet)
 * - Basic Collections (ArrayList)
 * - try-with-resources
 * - CRUD database operations
 * - Object-Relational Mapping (ResultSet to Property)
 * - Foreign-key and referential integrity protection
 */
public class PropertyDAO {

    /**
     * Inserts a new property record into MySQL.
     * 
     * @param property Property object with details to insert
     * @return generated property_id if successful, or -1 on failure
     * @throws SQLException if a database error occurs
     */
    public int addProperty(Property property) throws SQLException {
        String sql = "INSERT INTO properties (landlord_id, property_name, property_type, address, " +
                "city, state, pincode, total_rooms, monthly_rent, description) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, property.getLandlordId());
            pstmt.setString(2, property.getPropertyName().trim());
            pstmt.setString(3, property.getPropertyType().trim());
            pstmt.setString(4, property.getAddress().trim());
            pstmt.setString(5, property.getCity().trim());
            pstmt.setString(6, property.getState().trim());
            pstmt.setString(7, property.getPincode().trim());
            pstmt.setInt(8, property.getTotalRooms());
            pstmt.setDouble(9, property.getMonthlyRent());
            pstmt.setString(10, property.getDescription());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int generatedId = generatedKeys.getInt(1);
                        property.setPropertyId(generatedId);
                        return generatedId;
                    }
                }
            }
            return -1;
        } catch (SQLException e) {
            System.err.println("[PropertyDAO] addProperty failed: " + e.getMessage());
            throw e;
        }
    }

    public int addPropertyWithInitialRooms(Property property) throws SQLException {
        String propertySql = "INSERT INTO properties (landlord_id, property_name, property_type, address, "
                + "city, state, pincode, total_rooms, monthly_rent, description) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String roomSql = "INSERT INTO rooms (property_id, room_number, monthly_rent, room_status) "
                + "VALUES (?, ?, ?, 'Vacant')";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement propertyStatement = conn.prepareStatement(
                    propertySql, Statement.RETURN_GENERATED_KEYS)) {
                propertyStatement.setInt(1, property.getLandlordId());
                propertyStatement.setString(2, property.getPropertyName().trim());
                propertyStatement.setString(3, property.getPropertyType().trim());
                propertyStatement.setString(4, property.getAddress().trim());
                propertyStatement.setString(5, property.getCity().trim());
                propertyStatement.setString(6, property.getState().trim());
                propertyStatement.setString(7, property.getPincode().trim());
                propertyStatement.setInt(8, property.getTotalRooms());
                propertyStatement.setDouble(9, property.getMonthlyRent());
                propertyStatement.setString(10, property.getDescription());

                if (propertyStatement.executeUpdate() != 1) {
                    throw new SQLException("Property insert did not create a record.");
                }

                int propertyId;
                try (ResultSet keys = propertyStatement.getGeneratedKeys()) {
                    if (!keys.next() || (propertyId = keys.getInt(1)) <= 0) {
                        throw new SQLException("Property insert did not return a generated ID.");
                    }
                }

                try (PreparedStatement roomStatement = conn.prepareStatement(roomSql)) {
                    for (int room = 1; room <= property.getTotalRooms(); room++) {
                        roomStatement.setInt(1, propertyId);
                        roomStatement.setString(2, "Room " + (room < 10 ? "0" + room : room));
                        roomStatement.setDouble(3, property.getMonthlyRent());
                        roomStatement.addBatch();
                    }
                    int[] roomRows = roomStatement.executeBatch();
                    if (roomRows.length != property.getTotalRooms()) {
                        throw new SQLException("Initial room creation was incomplete.");
                    }
                }

                conn.commit();
                property.setPropertyId(propertyId);
                return propertyId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Retrieves all properties across the system (System Administrator / Debug
     * view).
     * 
     * @return ArrayList of Property objects
     */
    public ArrayList<Property> getAllProperties() {
        ArrayList<Property> list = new ArrayList<>();
        String sql = "SELECT property_id, landlord_id, property_name, property_type, address, " +
                "city, state, pincode, total_rooms, monthly_rent, description, created_at " +
                "FROM properties ORDER BY property_id DESC";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToProperty(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not retrieve properties from the database.", e);
        }
        return list;
    }

    /**
     * Retrieves a single property by primary key ID.
     * 
     * @param propertyId primary key
     * @return Property object if found, or null
     */
    public Property getPropertyById(int propertyId) {
        String sql = "SELECT property_id, landlord_id, property_name, property_type, address, " +
                "city, state, pincode, total_rooms, monthly_rent, description, created_at " +
                "FROM properties WHERE property_id = ?";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, propertyId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToProperty(rs);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not retrieve property " + propertyId + " from the database.", e);
        }
        return null;
    }

    /**
     * Retrieves all properties belonging to a specific landlord.
     * Core query powering the Landlord Properties dashboard.
     * 
     * @param landlordId landlord owner ID
     * @return ArrayList of properties owned by this landlord
     */
    public ArrayList<Property> getPropertiesByLandlordId(int landlordId) {
        ArrayList<Property> list = new ArrayList<>();
        String sql = "SELECT property_id, landlord_id, property_name, property_type, address, " +
                "city, state, pincode, total_rooms, monthly_rent, description, created_at " +
                "FROM properties WHERE landlord_id = ? ORDER BY property_id DESC";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, landlordId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToProperty(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not retrieve properties for the landlord.", e);
        }
        return list;
    }

    /**
     * Updates an existing property record.
     * 
     * @param property Property object containing updated attributes
     * @return true if updated successfully, false otherwise
     */
    public boolean updateProperty(Property property) {
        String sql = "UPDATE properties SET property_name = ?, property_type = ?, address = ?, " +
                "city = ?, state = ?, pincode = ?, total_rooms = ?, monthly_rent = ?, description = ? " +
                "WHERE property_id = ? AND landlord_id = ?";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, property.getPropertyName().trim());
            pstmt.setString(2, property.getPropertyType().trim());
            pstmt.setString(3, property.getAddress().trim());
            pstmt.setString(4, property.getCity().trim());
            pstmt.setString(5, property.getState().trim());
            pstmt.setString(6, property.getPincode().trim());
            pstmt.setInt(7, property.getTotalRooms());
            pstmt.setDouble(8, property.getMonthlyRent());
            pstmt.setString(9, property.getDescription());
            pstmt.setInt(10, property.getPropertyId());
            pstmt.setInt(11, property.getLandlordId());

            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not update the property in the database.", e);
        }
    }

    /**
     * Deletes a property record by ID.
     * Enforces safety: prevents deletion if foreign keys (e.g. active rooms,
     * tenants) are attached.
     * 
     * @param propertyId property to delete
     * @return true if deleted, false if deletion blocked or record not found
     */
    /**
     * Deletes a property record by ID within a database transaction.
     * Enforces safety: prevents deletion if active tenants, payments, or occupied rooms exist.
     * If the property only has unused vacant rooms, those rooms are safely deleted
     * along with the property inside the same transaction.
     * 
     * @param propertyId property to delete
     * @return true if deleted, false if deletion blocked or record not found
     */
    public boolean deleteProperty(int propertyId) {
        String checkOccupiedRooms = "SELECT COUNT(*) FROM rooms WHERE property_id = ? AND LOWER(room_status) != 'vacant'";
        String checkTenants = "SELECT COUNT(*) FROM tenants WHERE property_id = ?";
        String checkPayments = "SELECT COUNT(*) FROM payments WHERE property_id = ?";
        String deleteVacantRooms = "DELETE FROM rooms WHERE property_id = ? AND LOWER(room_status) = 'vacant'";
        String deletePropertySql = "DELETE FROM properties WHERE property_id = ?";

        try (Connection conn = DBConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                // 1. Guard check: Occupied or non-vacant rooms
                try (PreparedStatement pstmt = conn.prepareStatement(checkOccupiedRooms)) {
                    pstmt.setInt(1, propertyId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            System.err.println("[PropertyDAO] Cannot delete property ID " + propertyId +
                                    ": Occupied rooms exist. Remove or reassign tenants first.");
                            conn.rollback();
                            return false;
                        }
                    }
                }

                // 2. Guard check: Associated tenants
                try (PreparedStatement pstmt = conn.prepareStatement(checkTenants)) {
                    pstmt.setInt(1, propertyId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            System.err.println("[PropertyDAO] Cannot delete property ID " + propertyId +
                                    ": Associated tenant records exist.");
                            conn.rollback();
                            return false;
                        }
                    }
                }

                // 3. Guard check: Associated payments
                try (PreparedStatement pstmt = conn.prepareStatement(checkPayments)) {
                    pstmt.setInt(1, propertyId);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            System.err.println("[PropertyDAO] Cannot delete property ID " + propertyId +
                                    ": Associated payment records exist.");
                            conn.rollback();
                            return false;
                        }
                    }
                }

                // 4. Safe cascade: delete unused vacant rooms for this property
                try (PreparedStatement pstmt = conn.prepareStatement(deleteVacantRooms)) {
                    pstmt.setInt(1, propertyId);
                    pstmt.executeUpdate();
                }

                // 5. Delete the property record
                int rowsDeleted;
                try (PreparedStatement pstmt = conn.prepareStatement(deletePropertySql)) {
                    pstmt.setInt(1, propertyId);
                    rowsDeleted = pstmt.executeUpdate();
                }

                if (rowsDeleted > 0) {
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
            } catch (SQLException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("[PropertyDAO] Rollback failed: " + rollbackEx.getMessage());
                }
                throw new IllegalStateException("Could not delete property ID " + propertyId + " from the database.", e);
            } finally {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                } catch (SQLException ignored) {}
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database connection failed during property deletion.", e);
        }
    }

    /**
     * Checks if a property has associated active dependencies (tenants, payments, or occupied rooms)
     * that strictly forbid deletion.
     */
    public boolean hasActiveDependencies(int propertyId) {
        String checkOccupiedRooms = "SELECT COUNT(*) FROM rooms WHERE property_id = ? AND LOWER(room_status) != 'vacant'";
        String checkTenants = "SELECT COUNT(*) FROM tenants WHERE property_id = ?";
        String checkPayments = "SELECT COUNT(*) FROM payments WHERE property_id = ?";

        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(checkOccupiedRooms)) {
                pstmt.setInt(1, propertyId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return true;
                    }
                }
            }
            try (PreparedStatement pstmt = conn.prepareStatement(checkTenants)) {
                pstmt.setInt(1, propertyId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return true;
                    }
                }
            }
            try (PreparedStatement pstmt = conn.prepareStatement(checkPayments)) {
                pstmt.setInt(1, propertyId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not verify property dependencies.", e);
        }
        return false;
    }

    /**
     * Helper method to map a single SQL ResultSet row to a Java Property object.
     */
    private Property mapRowToProperty(ResultSet rs) throws SQLException {
        Property property = new Property();
        property.setPropertyId(rs.getInt("property_id"));
        property.setLandlordId(rs.getInt("landlord_id"));
        property.setPropertyName(rs.getString("property_name"));
        property.setPropertyType(rs.getString("property_type"));
        property.setAddress(rs.getString("address"));
        property.setCity(rs.getString("city"));
        property.setState(rs.getString("state"));
        property.setPincode(rs.getString("pincode"));
        property.setTotalRooms(rs.getInt("total_rooms"));
        property.setMonthlyRent(rs.getDouble("monthly_rent"));
        property.setDescription(rs.getString("description"));
        property.setCreatedAt(rs.getTimestamp("created_at"));
        return property;
    }
}
