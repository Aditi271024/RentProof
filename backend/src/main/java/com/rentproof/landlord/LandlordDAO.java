package com.rentproof.landlord;

import com.rentproof.database.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * LandlordDAO handles Data Access Object operations for the 'landlords' table.
 * 
 * Concepts used (Syllabus up to Lecture 42):
 * - JDBC API (Connection, PreparedStatement, Statement, ResultSet)
 * - CRUD operations (Create, Read, Update)
 * - Parameterized SQL queries (preventing SQL injection)
 * - Object-Relational Mapping (ResultSet to Landlord object)
 * - try-with-resources for automatic resource deallocation
 * - Exception handling (SQLException)
 */
public class LandlordDAO {

    /**
     * Inserts a new landlord record into MySQL.
     * Uses PreparedStatement with RETURN_GENERATED_KEYS to retrieve the auto-increment ID.
     * 
     * @param landlord Landlord object containing registration details
     * @return generated landlord_id if successful, or -1 if insertion fails
     * @throws SQLException if a database error occurs (e.g. duplicate email)
     */
    public int registerLandlord(Landlord landlord) throws SQLException {
        String sql = "INSERT INTO landlords (full_name, email, phone, password_hash) VALUES (?, ?, ?, ?)";

        // try-with-resources automatically closes Connection and PreparedStatement
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, landlord.getFullName());
            pstmt.setString(2, landlord.getEmail().toLowerCase().trim());
            pstmt.setString(3, landlord.getPhone().trim());
            pstmt.setString(4, landlord.getPasswordHash());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int generatedId = generatedKeys.getInt(1);
                        landlord.setLandlordId(generatedId);
                        return generatedId;
                    }
                }
            }
            return -1;
        } catch (SQLException e) {
            System.err.println("[LandlordDAO] registerLandlord failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Fetches a landlord by their unique email address.
     * Used primarily for authentication login verification.
     * 
     * @param email landlord's registered email
     * @return Landlord object if found, or null if no matching record
     */
    public Landlord getLandlordByEmail(String email) {
        String sql = "SELECT landlord_id, full_name, email, phone, password_hash, created_at FROM landlords WHERE email = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email.toLowerCase().trim());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToLandlord(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[LandlordDAO] getLandlordByEmail failed for " + email + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Fetches a landlord by primary key ID.
     * 
     * @param landlordId unique ID of the landlord
     * @return Landlord object if found, or null
     */
    public Landlord getLandlordById(int landlordId) {
        String sql = "SELECT landlord_id, full_name, email, phone, password_hash, created_at FROM landlords WHERE landlord_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, landlordId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToLandlord(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[LandlordDAO] getLandlordById failed for ID " + landlordId + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Updates profile details of an existing landlord.
     * 
     * @param landlord Landlord object containing updated values
     * @return true if updated successfully, false otherwise
     */
    public boolean updateLandlord(Landlord landlord) {
        String sql = "UPDATE landlords SET full_name = ?, phone = ? WHERE landlord_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, landlord.getFullName());
            pstmt.setString(2, landlord.getPhone());
            pstmt.setInt(3, landlord.getLandlordId());

            int rowsUpdated = pstmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            System.err.println("[LandlordDAO] updateLandlord failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Helper method to map a single SQL ResultSet row to a Java Landlord object.
     * Demonstrates Object-Relational Mapping (ORM) fundamentals in Core Java.
     */
    private Landlord mapRowToLandlord(ResultSet rs) throws SQLException {
        Landlord landlord = new Landlord();
        landlord.setLandlordId(rs.getInt("landlord_id"));
        landlord.setFullName(rs.getString("full_name"));
        landlord.setEmail(rs.getString("email"));
        landlord.setPhone(rs.getString("phone"));
        landlord.setPasswordHash(rs.getString("password_hash"));
        landlord.setCreatedAt(rs.getTimestamp("created_at"));
        return landlord;
    }
}
