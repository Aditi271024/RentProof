package com.rentproof.database;

import com.rentproof.landlord.AuthService;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseSeeder provides safe, idempotent seeding of demo data.
 * It checks for existing records before inserting, preventing duplicates.
 */
public class DatabaseSeeder {

    private static final String DEMO_LANDLORD_EMAIL = "aditi.landlord@rentproof.com";
    private static final String DEMO_PASSWORD = "RentProof@123";

    /**
     * Seeds initial demo records if they are not already present in the database.
     */
    public static void seedIfEmpty() {
        try (Connection conn = DBConnection.getConnection()) {
            seedDemoData(conn);
        } catch (Exception e) {
            System.err.println("[DatabaseSeeder] Notice: Demo data seeding skipped or partial: " + e.getMessage());
        }
    }

    /**
     * Main entry point to run seeder directly via CLI or batch scripts.
     */
    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("   RENTPROOF: IDEMPOTENT DATABASE SEEDER                    ");
        System.out.println("============================================================\n");
        try (Connection conn = DBConnection.getConnection()) {
            seedDemoData(conn);
            System.out.println("\n[SUCCESS] Demo data verified/seeded successfully without duplicates.");
        } catch (SQLException e) {
            System.err.println("[ERROR] Seeding failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void seedDemoData(Connection conn) throws SQLException {
        // 1. Seed or find demo landlord (Aditi Sharma)
        int landlordId = getOrCreateLandlord(conn, "Aditi Sharma", DEMO_LANDLORD_EMAIL, "9876543210", DEMO_PASSWORD);

        // Also ensure secondary demo landlords exist
        getOrCreateLandlord(conn, "Rajesh Kumar Verma", "rajesh.verma@gmail.com", "9811223344", DEMO_PASSWORD);
        getOrCreateLandlord(conn, "Pooja Hegde", "pooja.properties@yahoo.com", "9822334455", DEMO_PASSWORD);

        // 2. Seed demo properties for Aditi Sharma
        int propSunrise = getOrCreateProperty(conn, landlordId, "Sunrise Executive PG", "PG",
                "Plot 42, Knowledge Park III", "Greater Noida", "Uttar Pradesh", "201306", 6, 8500.00,
                "Premium fully furnished student PG with Wi-Fi, 3-time meals, power backup and 24x7 security.");

        int propIvory = getOrCreateProperty(conn, landlordId, "Ivory Heights 3BHK Flat", "Flat",
                "Tower B, Flat 702, Golf City", "Noida", "Uttar Pradesh", "201301", 3, 28000.00,
                "Spacious semi-furnished apartment for working professionals, modular kitchen and reserved car parking.");

        int propBlueRidge = getOrCreateProperty(conn, landlordId, "Blue Ridge Residency", "Apartment",
                "Sector 62, Near Metro Station", "Noida", "Uttar Pradesh", "201309", 4, 34000.00,
                "Modern 4-room independent apartment with club house access, swimming pool and round-the-clock maintenance.");

        // 3. Seed demo rooms for Sunrise Executive PG
        int room101 = getOrCreateRoom(conn, propSunrise, "101", 8500.00, "Occupied");
        int room102 = getOrCreateRoom(conn, propSunrise, "102", 8500.00, "Occupied");
        getOrCreateRoom(conn, propSunrise, "103", 8500.00, "Vacant");
        getOrCreateRoom(conn, propSunrise, "201", 9000.00, "Vacant");
        getOrCreateRoom(conn, propSunrise, "202", 9000.00, "Occupied");
        getOrCreateRoom(conn, propSunrise, "203", 9500.00, "Maintenance");

        // Seed demo rooms for Ivory Heights
        int roomA = getOrCreateRoom(conn, propIvory, "Room-A (Master)", 11000.00, "Occupied");
        getOrCreateRoom(conn, propIvory, "Room-B (Guest)", 9000.00, "Vacant");
        getOrCreateRoom(conn, propIvory, "Room-C (Standard)", 8000.00, "Occupied");

        // Seed demo rooms for Blue Ridge Residency
        getOrCreateRoom(conn, propBlueRidge, "B-101", 8500.00, "Vacant");
        getOrCreateRoom(conn, propBlueRidge, "B-102", 8500.00, "Occupied");
        getOrCreateRoom(conn, propBlueRidge, "B-103", 8500.00, "Vacant");
        getOrCreateRoom(conn, propBlueRidge, "B-104", 8500.00, "Vacant");

        // 4. Seed demo tenants
        int tenantRohan = getOrCreateTenant(conn, propSunrise, room101, "Rohan Verma",
                "rohan.v@gmail.com", "9988776655", "AADHAAR-8921-3412-9012", "2026-08-01", 8500.00, 17000.00, "Active");

        int tenantAnkit = getOrCreateTenant(conn, propSunrise, room102, "Ankit Mishra",
                "ankit.m@yahoo.com", "9871122334", "AADHAAR-6712-4590-1123", "2026-09-15", 8500.00, 17000.00, "Active");

        int tenantSneha = getOrCreateTenant(conn, propIvory, roomA, "Sneha Kapoor",
                "sneha.k@outlook.com", "9711334455", "PAN-ABCDE1234F", "2026-07-01", 11000.00, 22000.00, "Active");

        // 5. Seed demo payments
        getOrCreatePayment(conn, tenantRohan, propSunrise, room101, 8500.00,
                "2026-11-05", "November 2026", "UPI", "UPI/20261105/98218731", "Paid", "Rent received via Google Pay");

        getOrCreatePayment(conn, tenantAnkit, propSunrise, room102, 8500.00,
                "2026-11-04", "November 2026", "Bank Transfer", "NEFT/HDFC/001928371", "Paid", "On-time monthly rental");

        getOrCreatePayment(conn, tenantSneha, propIvory, roomA, 11000.00,
                "2026-11-02", "November 2026", "UPI", "UPI/20261102/44129031", "Paid", "Direct payment received");

        // 6. Seed demo reminders
        getOrCreateReminder(conn, landlordId, tenantRohan, "Upcoming Rental Due",
                "Friendly reminder: December rent is due on 5th Dec.", "2026-12-05", "Rent Due", "Pending");

        getOrCreateReminder(conn, landlordId, null, "Bi-annual Fire Extinguisher Inspection",
                "Inspect all fire extinguishers and smoke alarms in Sunrise PG.", "2026-11-30", "Maintenance", "Pending");
    }

    private static int getOrCreateLandlord(Connection conn, String name, String email, String phone, String plainPassword)
            throws SQLException {
        String query = "SELECT landlord_id FROM landlords WHERE email = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        String salt = AuthService.generateSalt();
        String hash = AuthService.hashPassword(plainPassword, salt, AuthService.PBKDF2_ITERATIONS);
        String insert = "INSERT INTO landlords (full_name, email, phone, password_hash, password_salt, password_iterations) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.setString(4, hash);
            ps.setString(5, salt);
            ps.setInt(6, AuthService.PBKDF2_ITERATIONS);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    System.out.println("  [+] Seeded landlord: " + email + " (ID: " + id + ")");
                    return id;
                }
            }
        }
        return 0;
    }

    private static int getOrCreateProperty(Connection conn, int landlordId, String name, String type,
                                           String address, String city, String state, String pincode,
                                           int totalRooms, double rent, String description) throws SQLException {
        String query = "SELECT property_id FROM properties WHERE landlord_id = ? AND property_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, landlordId);
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        String insert = "INSERT INTO properties (landlord_id, property_name, property_type, address, city, state, "
                + "pincode, total_rooms, monthly_rent, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, landlordId);
            ps.setString(2, name);
            ps.setString(3, type);
            ps.setString(4, address);
            ps.setString(5, city);
            ps.setString(6, state);
            ps.setString(7, pincode);
            ps.setInt(8, totalRooms);
            ps.setDouble(9, rent);
            ps.setString(10, description);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    System.out.println("  [+] Seeded property: " + name + " (ID: " + id + ")");
                    return id;
                }
            }
        }
        return 0;
    }

    private static int getOrCreateRoom(Connection conn, int propId, String roomNum, double rent, String status)
            throws SQLException {
        String query = "SELECT room_id FROM rooms WHERE property_id = ? AND room_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, propId);
            ps.setString(2, roomNum);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        String insert = "INSERT INTO rooms (property_id, room_number, monthly_rent, room_status) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, propId);
            ps.setString(2, roomNum);
            ps.setDouble(3, rent);
            ps.setString(4, status);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    return id;
                }
            }
        }
        return 0;
    }

    private static int getOrCreateTenant(Connection conn, int propId, int roomId, String name, String email,
                                         String phone, String idProof, String moveInDate, double rent,
                                         double deposit, String status) throws SQLException {
        String query = "SELECT tenant_id FROM tenants WHERE email = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        String insert = "INSERT INTO tenants (property_id, room_id, full_name, email, phone, id_proof_number, "
                + "move_in_date, monthly_rent, security_deposit, tenant_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, propId);
            ps.setInt(2, roomId);
            ps.setString(3, name);
            ps.setString(4, email);
            ps.setString(5, phone);
            ps.setString(6, idProof);
            ps.setDate(7, Date.valueOf(moveInDate));
            ps.setDouble(8, rent);
            ps.setDouble(9, deposit);
            ps.setString(10, status);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    System.out.println("  [+] Seeded tenant: " + name + " (ID: " + id + ")");
                    return id;
                }
            }
        }
        return 0;
    }

    private static int getOrCreatePayment(Connection conn, int tenantId, int propId, int roomId, double amount,
                                          String date, String month, String mode, String ref, String status, String remarks)
            throws SQLException {
        String query = "SELECT payment_id FROM payments WHERE transaction_ref = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, ref);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        String insert = "INSERT INTO payments (tenant_id, property_id, room_id, amount, payment_date, payment_month, "
                + "payment_mode, transaction_ref, payment_status, remarks) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, tenantId);
            ps.setInt(2, propId);
            ps.setInt(3, roomId);
            ps.setDouble(4, amount);
            ps.setDate(5, Date.valueOf(date));
            ps.setString(6, month);
            ps.setString(7, mode);
            ps.setString(8, ref);
            ps.setString(9, status);
            ps.setString(10, remarks);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    System.out.println("  [+] Seeded payment ref: " + ref + " (ID: " + id + ")");
                    return id;
                }
            }
        }
        return 0;
    }

    private static int getOrCreateReminder(Connection conn, int landlordId, Integer tenantId, String title,
                                           String message, String dueDate, String type, String status) throws SQLException {
        String query = "SELECT reminder_id FROM reminders WHERE landlord_id = ? AND title = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, landlordId);
            ps.setString(2, title);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        String insert = "INSERT INTO reminders (landlord_id, tenant_id, title, message, due_date, reminder_type, reminder_status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, landlordId);
            if (tenantId != null) {
                ps.setInt(2, tenantId);
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            ps.setString(3, title);
            ps.setString(4, message);
            ps.setDate(5, Date.valueOf(dueDate));
            ps.setString(6, type);
            ps.setString(7, status);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    System.out.println("  [+] Seeded reminder: " + title + " (ID: " + id + ")");
                    return id;
                }
            }
        }
        return 0;
    }
}
