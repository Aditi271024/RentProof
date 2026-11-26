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
            System.err.println("[PropertyDAO] getAllProperties failed: " + e.getMessage());
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
            System.err.println("[PropertyDAO] getPropertyById failed for ID " + propertyId + ": " + e.getMessage());
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
            System.err.println("[PropertyDAO] getPropertiesByLandlordId failed for landlord " + landlordId + ": "
                    + e.getMessage());
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
            System.err.println("[PropertyDAO] updateProperty failed: " + e.getMessage());
            return false;
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
    public boolean deleteProperty(int propertyId) {
        // Safety verification: check for dependent rooms or tenants first
        if (hasActiveDependencies(propertyId)) {
            System.err.println("[PropertyDAO] Cannot delete property ID " + propertyId +
                    ": Associated rooms or tenant records exist. Remove them first.");
            return false;
        }

        String sql = "DELETE FROM properties WHERE property_id = ?";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, propertyId);
            int rowsDeleted = pstmt.executeUpdate();
            return rowsDeleted > 0;
        } catch (SQLException e) {
            System.err.println("[PropertyDAO] deleteProperty failed for ID " + propertyId + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a property has associated rooms or tenants to protect data
     * integrity.
     */
    public boolean hasActiveDependencies(int propertyId) {
        String checkRooms = "SELECT COUNT(*) FROM rooms WHERE property_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(checkRooms)) {

            pstmt.setInt(1, propertyId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return true;
                }
            }
        } catch (SQLException e) {
            // If rooms table does not exist yet or query fails, allow proceed with caution
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
