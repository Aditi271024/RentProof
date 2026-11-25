package com.rentproof.property;

import com.rentproof.database.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * RoomDAO handles Data Access Object operations for the 'rooms' table in MySQL.
 * 
 * Part of Property & Room Management.
 * 
 * Concepts used (Syllabus up to Lecture 42):
 * - JDBC API (Connection, PreparedStatement, ResultSet, Statement)
 * - Basic Collections (ArrayList)
 * - try-with-resources statement
 * - CRUD database operations
 * - Object-Relational Mapping (ResultSet to Room)
 * - Foreign-key protection against deleting occupied rooms
 */
public class RoomDAO {

    /**
     * Inserts a new room into MySQL.
     * 
     * @param room Room object to insert
     * @return generated room_id if successful, or -1 on failure
     * @throws SQLException if a database error occurs
     */
    public int addRoom(Room room) throws SQLException {
        String sql = "INSERT INTO rooms (property_id, room_number, monthly_rent, room_status) " +
                     "VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, room.getPropertyId());
            pstmt.setString(2, room.getRoomNumber().trim());
            pstmt.setDouble(3, room.getMonthlyRent());
            pstmt.setString(4, room.getRoomStatus() != null ? room.getRoomStatus().trim() : "Vacant");

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int generatedId = generatedKeys.getInt(1);
                        room.setRoomId(generatedId);
                        return generatedId;
                    }
                }
            }
            return -1;
        } catch (SQLException e) {
            System.err.println("[RoomDAO] addRoom failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Retrieves all rooms belonging to a specific property.
     * 
     * @param propertyId property ID
     * @return ArrayList of Room objects
     */
    public ArrayList<Room> getRoomsByPropertyId(int propertyId) throws SQLException {
        ArrayList<Room> list = new ArrayList<>();
        String sql = "SELECT r.room_id, r.property_id, r.room_number, r.monthly_rent, r.room_status, " +
                     "r.created_at, p.property_name " +
                     "FROM rooms r " +
                     "JOIN properties p ON r.property_id = p.property_id " +
                     "WHERE r.property_id = ? ORDER BY r.room_number ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, propertyId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToRoom(rs));
                }
            }
        }
        return list;
    }

    /**
     * Retrieves all rooms across all properties.
     * 
     * @return ArrayList of Room objects
     */
    public ArrayList<Room> getAllRooms() throws SQLException {
        ArrayList<Room> list = new ArrayList<>();
        String sql = "SELECT r.room_id, r.property_id, r.room_number, r.monthly_rent, r.room_status, " +
                     "r.created_at, p.property_name " +
                     "FROM rooms r " +
                     "LEFT JOIN properties p ON r.property_id = p.property_id " +
                     "ORDER BY r.property_id, r.room_number";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToRoom(rs));
            }
        }
        return list;
    }

    /**
     * Retrieves a room by its primary key ID.
     * 
     * @param roomId room primary key
     * @return Room object or null
     */
    public Room getRoomById(int roomId) throws SQLException {
        String sql = "SELECT r.room_id, r.property_id, r.room_number, r.monthly_rent, r.room_status, " +
                     "r.created_at, p.property_name " +
                     "FROM rooms r " +
                     "LEFT JOIN properties p ON r.property_id = p.property_id " +
                     "WHERE r.room_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, roomId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToRoom(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves only vacant rooms for a specific property.
     * Useful for assigning rooms to new tenants.
     */
    public ArrayList<Room> getVacantRoomsByPropertyId(int propertyId) throws SQLException {
        ArrayList<Room> list = new ArrayList<>();
        String sql = "SELECT r.room_id, r.property_id, r.room_number, r.monthly_rent, r.room_status, " +
                     "r.created_at, p.property_name " +
                     "FROM rooms r " +
                     "JOIN properties p ON r.property_id = p.property_id " +
                     "WHERE r.property_id = ? AND r.room_status = 'Vacant' " +
                     "ORDER BY r.room_number ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, propertyId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToRoom(rs));
                }
            }
        }
        return list;
    }

    /**
     * Updates an existing room record.
     */
    public boolean updateRoom(Room room) throws SQLException {
        String sql = "UPDATE rooms SET room_number = ?, monthly_rent = ?, room_status = ? " +
                     "WHERE room_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, room.getRoomNumber().trim());
            pstmt.setDouble(2, room.getMonthlyRent());
            pstmt.setString(3, room.getRoomStatus().trim());
            pstmt.setInt(4, room.getRoomId());

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Updates room status (e.g. 'Vacant', 'Occupied', 'Maintenance').
     */
    public boolean updateRoomStatus(int roomId, String status) throws SQLException {
        String sql = "UPDATE rooms SET room_status = ? WHERE room_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.trim());
            pstmt.setInt(2, roomId);

            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a room only when no tenant or payment records reference it.
     */
    public boolean deleteRoom(int roomId) throws SQLException {
        String checkDependencies = "SELECT "
                + "(SELECT COUNT(*) FROM tenants WHERE room_id = ?) + "
                + "(SELECT COUNT(*) FROM payments WHERE room_id = ?)";
        String deleteSql = "DELETE FROM rooms WHERE room_id = ?";
        try (Connection conn = DBConnection.getConnection()) {
            try (PreparedStatement check = conn.prepareStatement(checkDependencies)) {
                check.setInt(1, roomId);
                check.setInt(2, roomId);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        System.err.println("[RoomDAO] Cannot delete room ID " + roomId
                                + ": tenant or payment records are linked.");
                        return false;
                    }
                }
            }
            try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
                pstmt.setInt(1, roomId);
                return pstmt.executeUpdate() > 0;
            }
        }
    }

    public boolean hasActiveTenant(int roomId) throws SQLException {
        String sql = "SELECT 1 FROM tenants WHERE room_id = ? AND tenant_status = 'Active' LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, roomId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Helper to map a ResultSet row to a Room model instance.
     */
    private Room mapRowToRoom(ResultSet rs) throws SQLException {
        Room r = new Room();
        r.setRoomId(rs.getInt("room_id"));
        r.setPropertyId(rs.getInt("property_id"));
        r.setRoomNumber(rs.getString("room_number"));
        r.setMonthlyRent(rs.getDouble("monthly_rent"));
        r.setRoomStatus(rs.getString("room_status"));
        r.setCreatedAt(rs.getTimestamp("created_at"));
        try {
            r.setPropertyName(rs.getString("property_name"));
        } catch (SQLException ignored) {
            // column might not be present if not joined
        }
        return r;
    }
}
