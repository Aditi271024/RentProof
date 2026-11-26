package com.rentproof;

import com.rentproof.database.DBConnection;
import com.rentproof.landlord.AuthService;
import com.rentproof.landlord.Landlord;
import com.rentproof.payment.Payment;
import com.rentproof.payment.PaymentService;
import com.rentproof.property.Property;
import com.rentproof.property.PropertyService;
import com.rentproof.tenant.Tenant;
import com.rentproof.tenant.TenantService;

import java.io.Console;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;

public class RentProofConsole {

    private static final Console CONSOLE = System.console();
    private static final Scanner SCANNER = CONSOLE == null ? new Scanner(System.in) : null;
    private static final AuthService AUTH_SERVICE = new AuthService();
    private static final PropertyService PROPERTY_SERVICE = new PropertyService();
    private static final TenantService TENANT_SERVICE = new TenantService();
    private static final PaymentService PAYMENT_SERVICE = new PaymentService();
    private static final String[] REMINDER_TYPES = {
            "Rent Due", "Agreement Expiry", "Maintenance", "General"
    };
    private static final String[] REMINDER_STATUSES = {"Pending", "Sent", "Resolved"};
    private static Landlord currentLandlord;
    private static boolean inputClosed;

    private interface Operation {
        void run() throws SQLException;
    }

    public static void main(String[] args) {
        boolean running = true;
        while (running && !inputClosed) {
            printHeader();
            System.out.println("  1. Landlord Management");
            System.out.println("  2. Property Management");
            System.out.println("  3. Room Management");
            System.out.println("  4. Tenant Management");
            System.out.println("  5. Rent Management");
            System.out.println("  6. Payment Management");
            System.out.println("  7. Reminder Management");
            System.out.println("  8. Dashboard & Reports");
            System.out.println("  9. Exit");
            System.out.println("========================================");

            int choice = readInt("Select an option: ");
            switch (choice) {
                case 1 -> landlordMenu();
                case 2 -> propertyMenu();
                case 3 -> roomMenu();
                case 4 -> tenantMenu();
                case 5 -> rentMenu();
                case 6 -> paymentMenu();
                case 7 -> reminderMenu();
                case 8 -> dashboardMenu();
                case 9 -> {
                    System.out.println("Thank you for using RentProof.");
                    running = false;
                }
                default -> {
                    System.out.println("Choose a number from 1 to 9.");
                    pause();
                }
            }
        }
        if (SCANNER != null) {
            SCANNER.close();
        }
    }

    private static void printHeader() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("             RENTPROOF");
        System.out.println("       PROPERTY MANAGEMENT");
        System.out.println("          CONSOLE EDITION");
        System.out.println("========================================");
        System.out.println(currentLandlord == null
                ? "Not logged in"
                : "Signed in: " + currentLandlord.getFullName()
                        + " (landlord #" + currentLandlord.getLandlordId() + ")");
        System.out.println();
    }

    private static void landlordMenu() {
        runMenu("LANDLORD MANAGEMENT",
                RentProofConsole::registerLandlord,
                RentProofConsole::viewLandlord,
                RentProofConsole::updateLandlord,
                RentProofConsole::loginLandlord);
    }

    private static void propertyMenu() {
        runMenu("PROPERTY MANAGEMENT",
                RentProofConsole::addProperty,
                RentProofConsole::viewProperties,
                RentProofConsole::searchProperty,
                RentProofConsole::updateProperty,
                RentProofConsole::deleteProperty);
    }

    private static void roomMenu() {
        runMenu("ROOM MANAGEMENT",
                RentProofConsole::addRoom,
                RentProofConsole::viewRooms,
                RentProofConsole::viewVacantRooms,
                RentProofConsole::updateRoom,
                RentProofConsole::deleteRoom);
    }

    private static void tenantMenu() {
        runMenu("TENANT MANAGEMENT",
                RentProofConsole::registerTenant,
                RentProofConsole::viewTenants,
                RentProofConsole::searchTenant,
                RentProofConsole::updateTenant,
                RentProofConsole::deleteTenant,
                RentProofConsole::assignTenant);
    }

    private static void rentMenu() {
        runMenu("RENT MANAGEMENT",
                RentProofConsole::viewMonthlyRent,
                RentProofConsole::viewExpectedRent,
                RentProofConsole::viewCollectedRent,
                RentProofConsole::viewPendingDues,
                RentProofConsole::viewOverdueRent,
                RentProofConsole::updateRoomRent);
    }

    private static void paymentMenu() {
        runMenu("PAYMENT MANAGEMENT",
                RentProofConsole::recordPayment,
                RentProofConsole::viewPaymentHistory,
                RentProofConsole::searchPayment,
                RentProofConsole::updatePayment,
                RentProofConsole::updatePaymentStatus,
                RentProofConsole::deletePayment);
    }

    private static void reminderMenu() {
        runMenu("REMINDER MANAGEMENT",
                RentProofConsole::addReminder,
                RentProofConsole::viewReminders,
                RentProofConsole::updateReminder,
                RentProofConsole::deleteReminder,
                RentProofConsole::toggleReminderStatus);
    }

    private static void dashboardMenu() {
        runMenu("DASHBOARD & REPORTS",
                RentProofConsole::showTotalProperties,
                RentProofConsole::showTotalRooms,
                RentProofConsole::showTotalTenants,
                RentProofConsole::showTotalRentCollected,
                RentProofConsole::showPendingPayments,
                RentProofConsole::showReminderStatistics);
    }

    private static void runMenu(String title, Operation... operations) {
        while (!inputClosed) {
            System.out.println("\n========================================");
            System.out.println("       " + title);
            System.out.println("========================================");
            for (int i = 0; i < operations.length; i++) {
                System.out.println("  " + (i + 1) + ". " + menuLabel(title, i));
            }
            System.out.println("  " + (operations.length + 1) + ". Back to Main Menu");
            int choice = readInt("Select an option: ");
            if (choice == operations.length + 1) {
                return;
            }
            if (choice < 1 || choice > operations.length) {
                System.out.println("Invalid option.");
                pause();
                continue;
            }
            try {
                operations[choice - 1].run();
            } catch (IllegalArgumentException | DateTimeParseException e) {
                System.out.println("Invalid input: " + e.getMessage());
            } catch (SQLException e) {
                System.out.println("Database operation failed: " + e.getMessage());
            }
            pause();
        }
    }

    private static String menuLabel(String title, int index) {
        String[][] labels = {
                {"Register Landlord", "View Landlord Details", "Update Landlord Profile", "Login"},
                {"Add Property", "View All Properties", "Search Property", "Update Property", "Delete Property"},
                {"Add Room", "View All Rooms", "View Vacant Rooms", "Update Room", "Delete Room"},
                {"Register Tenant", "View All Tenants", "Search Tenant", "Update Tenant", "Delete Tenant",
                        "Assign Property / Room"},
                {"View Monthly Rent", "View Expected Rent", "View Collected Rent", "View Pending Dues",
                        "View Overdue Rent", "Update Room Rent"},
                {"Record Payment", "View Payment History", "Search Payment", "Update Payment",
                        "Update Payment Status", "Delete Payment"},
                {"Add Reminder", "View All Reminders", "Update Reminder", "Delete Reminder",
                        "Toggle Reminder Status"},
                {"Total Properties", "Total Rooms", "Total Tenants", "Total Rent Collected",
                        "Pending Payments", "Reminder Statistics"}
        };
        int group = switch (title) {
            case "LANDLORD MANAGEMENT" -> 0;
            case "PROPERTY MANAGEMENT" -> 1;
            case "ROOM MANAGEMENT" -> 2;
            case "TENANT MANAGEMENT" -> 3;
            case "RENT MANAGEMENT" -> 4;
            case "PAYMENT MANAGEMENT" -> 5;
            case "REMINDER MANAGEMENT" -> 6;
            default -> 7;
        };
        return labels[group][index];
    }

    private static void registerLandlord() throws SQLException {
        System.out.println("\nCreate landlord account");
        String name = requiredText("Full name: ");
        String email = requiredText("Email: ");
        String phone = requiredText("Phone: ");
        String password = readSecret("Password (not displayed): ");
        String confirm = readSecret("Confirm password (not displayed): ");
        Landlord landlord = AUTH_SERVICE.register(name, email, phone, password, confirm);
        currentLandlord = landlord;
        System.out.println("Registration successful. You are signed in as landlord #"
                + landlord.getLandlordId() + ".");
    }

    private static void viewLandlord() throws SQLException {
        Landlord landlord = requireLogin();
        String sql = "SELECT landlord_id, full_name, email, phone, created_at "
                + "FROM landlords WHERE landlord_id = ?";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, landlord.getLandlordId());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    System.out.println("\nLandlord #" + rs.getInt("landlord_id"));
                    System.out.println("Name: " + rs.getString("full_name"));
                    System.out.println("Email: " + rs.getString("email"));
                    System.out.println("Phone: " + rs.getString("phone"));
                    System.out.println("Created: " + rs.getTimestamp("created_at"));
                } else {
                    System.out.println("Landlord account no longer exists.");
                    currentLandlord = null;
                }
            }
        }
    }

    private static void updateLandlord() throws SQLException {
        Landlord landlord = requireLogin();
        String name = requiredText("Updated full name: ");
        String phone = requiredText("Updated phone: ");
        if (name.length() < 2 || !phone.matches("[6-9][0-9]{9}")) {
            throw new IllegalArgumentException("Enter a name of at least 2 characters and a valid 10-digit mobile number.");
        }
        String sql = "UPDATE landlords SET full_name = ?, phone = ? WHERE landlord_id = ?";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name.trim());
            statement.setString(2, phone.trim());
            statement.setInt(3, landlord.getLandlordId());
            if (statement.executeUpdate() == 1) {
                landlord.setFullName(name.trim());
                landlord.setPhone(phone.trim());
                System.out.println("Landlord profile updated.");
            } else {
                System.out.println("No landlord profile was updated.");
            }
        }
    }

    private static void loginLandlord() {
        String email = requiredText("Email: ");
        String password = readSecret("Password (not displayed): ");
        Landlord landlord = AUTH_SERVICE.login(email, password);
        if (landlord == null) {
            System.out.println("Login failed. Check your email and password.");
            return;
        }
        currentLandlord = landlord;
        System.out.println("Login successful. Welcome, " + landlord.getFullName() + ".");
    }

    private static void addProperty() throws SQLException {
        Landlord landlord = requireLogin();
        Property property = promptProperty(0, landlord.getLandlordId());
        int propertyId = PROPERTY_SERVICE.addProperty(property, landlord.getLandlordId());
        System.out.println("Property added with ID " + propertyId + ".");
    }

    private static Property promptProperty(int propertyId, int landlordId) {
        Property property = new Property();
        property.setPropertyId(propertyId);
        property.setLandlordId(landlordId);
        property.setPropertyName(requiredText("Property name: "));
        property.setPropertyType(requiredText("Property type (PG, Flat, Apartment, House): "));
        property.setAddress(requiredText("Street address: "));
        property.setCity(requiredText("City: "));
        property.setState(requiredText("State: "));
        property.setPincode(requiredText("PIN code: "));
        property.setTotalRooms(readPositiveInt("Declared room count: "));
        property.setMonthlyRent(readNonNegativeMoney("Fallback monthly rent: "));
        property.setDescription(optionalText("Description (optional): "));
        String error = PROPERTY_SERVICE.validateProperty(property);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        return property;
    }

    private static void viewProperties() throws SQLException {
        Landlord landlord = requireLogin();
        String sql = "SELECT property_id, property_name, property_type, address, city, state, pincode, "
                + "total_rooms, monthly_rent FROM properties WHERE landlord_id = ? ORDER BY property_name";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, landlord.getLandlordId());
            try (ResultSet rs = statement.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    printPropertyRow(rs);
                    count++;
                }
                if (count == 0) {
                    System.out.println("No properties found.");
                }
            }
        }
    }

    private static void searchProperty() throws SQLException {
        Landlord landlord = requireLogin();
        String keyword = requiredText("Search by property name, type, address, or city: ");
        String sql = "SELECT property_id, property_name, property_type, address, city, state, pincode, "
                + "total_rooms, monthly_rent FROM properties WHERE landlord_id = ? "
                + "AND (property_name LIKE ? OR property_type LIKE ? OR address LIKE ? OR city LIKE ?) "
                + "ORDER BY property_name";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, landlord.getLandlordId());
            String pattern = "%" + keyword.trim() + "%";
            for (int i = 2; i <= 5; i++) {
                statement.setString(i, pattern);
            }
            try (ResultSet rs = statement.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    printPropertyRow(rs);
                    count++;
                }
                if (count == 0) {
                    System.out.println("No matching properties.");
                }
            }
        }
    }

    private static void updateProperty() throws SQLException {
        Landlord landlord = requireLogin();
        int propertyId = readPositiveInt("Property ID: ");
        Property existing = PROPERTY_SERVICE.getPropertyById(propertyId);
        if (existing == null || existing.getLandlordId() != landlord.getLandlordId()) {
            System.out.println("Property not found for this landlord.");
            return;
        }
        Property update = promptProperty(propertyId, landlord.getLandlordId());
        System.out.println(PROPERTY_SERVICE.updateProperty(update, landlord.getLandlordId())
                ? "Property updated."
                : "No property was updated.");
    }

    private static void deleteProperty() throws SQLException {
        Landlord landlord = requireLogin();
        int propertyId = readPositiveInt("Property ID to delete: ");
        if (!PROPERTY_SERVICE.checkPropertyOwnership(propertyId, landlord.getLandlordId())) {
            System.out.println("Property not found for this landlord.");
            return;
        }
        if (!confirm("Delete this property? Related rooms or records will prevent deletion (y/N): ")) {
            System.out.println("Deletion cancelled.");
            return;
        }
        System.out.println(PROPERTY_SERVICE.deleteProperty(propertyId, landlord.getLandlordId())
                ? "Property deleted."
                : "Property could not be deleted. Remove dependent records first.");
    }

    private static void printPropertyRow(ResultSet rs) throws SQLException {
        System.out.printf(Locale.ROOT, "#%d | %s (%s) | %s, %s | PIN %s | rooms %d | rent %.2f%n",
                rs.getInt("property_id"), rs.getString("property_name"), rs.getString("property_type"),
                rs.getString("city"), rs.getString("state"), rs.getString("pincode"),
                rs.getInt("total_rooms"), rs.getDouble("monthly_rent"));
        System.out.println("  Address: " + rs.getString("address"));
    }

    private static void addRoom() throws SQLException {
        Landlord landlord = requireLogin();
        int propertyId = readPositiveInt("Property ID: ");
        requireOwnedProperty(propertyId, landlord.getLandlordId());
        String number = requiredText("Room number/label: ");
        double rent = readNonNegativeMoney("Room monthly rent (0 uses property fallback): ");
        String status = chooseRoomStatus("Room status (Vacant or Maintenance): ", false);
        String sql = "INSERT INTO rooms (property_id, room_number, monthly_rent, room_status) VALUES (?, ?, ?, ?)";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, propertyId);
            statement.setString(2, number.trim());
            statement.setDouble(3, rent);
            statement.setString(4, status);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    System.out.println("Room added with ID " + keys.getInt(1) + ".");
                } else {
                    System.out.println("Room added.");
                }
            }
        }
    }

    private static void viewRooms() throws SQLException {
        showRooms(false);
    }

    private static void viewVacantRooms() throws SQLException {
        showRooms(true);
    }

    private static void showRooms(boolean vacantOnly) throws SQLException {
        Landlord landlord = requireLogin();
        String sql = "SELECT r.room_id, r.property_id, p.property_name, r.room_number, r.monthly_rent, "
                + "COALESCE(NULLIF(r.monthly_rent, 0), p.monthly_rent) AS effective_rent, r.room_status "
                + "FROM rooms r JOIN properties p ON p.property_id = r.property_id "
                + "WHERE p.landlord_id = ? " + (vacantOnly ? "AND r.room_status = 'Vacant' " : "")
                + "ORDER BY p.property_name, r.room_number";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, landlord.getLandlordId());
            try (ResultSet rs = statement.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    System.out.printf(Locale.ROOT,
                            "Room #%d | Property #%d %s | %s | rent %.2f | %s%n",
                            rs.getInt("room_id"), rs.getInt("property_id"), rs.getString("property_name"),
                            rs.getString("room_number"), rs.getDouble("effective_rent"),
                            rs.getString("room_status"));
                    count++;
                }
                if (count == 0) {
                    System.out.println(vacantOnly ? "No vacant rooms found." : "No rooms found.");
                }
            }
        }
    }

    private static void updateRoom() throws SQLException {
        Landlord landlord = requireLogin();
        int roomId = readPositiveInt("Room ID: ");
        String number = requiredText("Updated room number/label: ");
        double rent = readNonNegativeMoney("Updated monthly rent (0 uses property fallback): ");
        String status = chooseRoomStatus("Status (Vacant, Occupied, Maintenance): ", true);
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String ownershipSql = "SELECT r.room_id FROM rooms r JOIN properties p "
                        + "ON p.property_id = r.property_id WHERE r.room_id = ? AND p.landlord_id = ? FOR UPDATE";
                try (PreparedStatement statement = connection.prepareStatement(ownershipSql)) {
                    statement.setInt(1, roomId);
                    statement.setInt(2, landlord.getLandlordId());
                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("Room not found for this landlord.");
                        }
                    }
                }
                boolean occupied = false;
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT tenant_id FROM tenants WHERE room_id = ? AND tenant_status = 'Active' FOR UPDATE")) {
                    statement.setInt(1, roomId);
                    try (ResultSet rs = statement.executeQuery()) {
                        occupied = rs.next();
                    }
                }
                if (occupied != "Occupied".equals(status)) {
                    throw new IllegalArgumentException(occupied
                            ? "An active tenant is assigned to this room; keep it Occupied or move the tenant first."
                            : "A room can be marked Occupied only when an active tenant is assigned.");
                }
                String sql = "UPDATE rooms SET room_number = ?, monthly_rent = ?, room_status = ? WHERE room_id = ?";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, number.trim());
                    statement.setDouble(2, rent);
                    statement.setString(3, status);
                    statement.setInt(4, roomId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Room was not updated.");
                    }
                }
                connection.commit();
                System.out.println("Room updated.");
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private static void deleteRoom() throws SQLException {
        Landlord landlord = requireLogin();
        int roomId = readPositiveInt("Room ID to delete: ");
        requireOwnedRoom(roomId, landlord.getLandlordId(), false);
        if (!confirm("Delete this room? Related tenants or payments will prevent deletion (y/N): ")) {
            System.out.println("Deletion cancelled.");
            return;
        }
        String sql = "DELETE FROM rooms WHERE room_id = ?";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, roomId);
            System.out.println(statement.executeUpdate() == 1
                    ? "Room deleted."
                    : "Room was not deleted.");
        }
    }

    private static String chooseRoomStatus(String prompt, boolean allowOccupied) {
        String allowed = allowOccupied ? "Vacant, Occupied, Maintenance" : "Vacant, Maintenance";
        while (true) {
            String value = requiredText(prompt + "[" + allowed + "]: ");
            for (String candidate : new String[]{"Vacant", "Occupied", "Maintenance"}) {
                if (candidate.equalsIgnoreCase(value)) {
                    if (!allowOccupied && "Occupied".equals(candidate)) {
                        break;
                    }
                    return candidate;
                }
            }
            System.out.println("Choose one of: " + allowed + ".");
        }
    }

    private static void registerTenant() throws SQLException {
        Landlord landlord = requireLogin();
        int propertyId = readPositiveInt("Property ID: ");
        requireOwnedProperty(propertyId, landlord.getLandlordId());
        int roomId = readPositiveInt("Vacant room ID: ");
        RoomDetails room = requireOwnedRoom(roomId, landlord.getLandlordId(), true);
        if (room.propertyId != propertyId) {
            throw new IllegalArgumentException("The selected room does not belong to the selected property.");
        }

        Tenant tenant = promptTenant(propertyId, roomId,
                readDate("Move-in date (YYYY-MM-DD): "), "Active", room.effectiveRent);
        String error = TENANT_SERVICE.validateTenant(tenant);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        ensureTenantEmailAvailable(tenant.getEmail(), 0);

        String insertSql = "INSERT INTO tenants (property_id, room_id, full_name, email, phone, "
                + "emergency_contact, permanent_address, id_proof_number, move_in_date, monthly_rent, "
                + "security_deposit, tenant_status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'Active')";
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                assertRoomVacant(connection, roomId, propertyId);
                try (PreparedStatement statement = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    bindTenant(statement, tenant);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Tenant insert did not return an ID.");
                        }
                        tenant.setTenantId(keys.getInt(1));
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE rooms SET room_status = 'Occupied' WHERE room_id = ? AND room_status = 'Vacant'")) {
                    statement.setInt(1, roomId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Room availability changed before assignment; no tenant was registered.");
                    }
                }
                connection.commit();
                System.out.println("Tenant registered with ID " + tenant.getTenantId() + "; room marked Occupied.");
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private static Tenant promptTenant(int propertyId, int roomId, Date moveInDate,
            String status, double suggestedRent) {
        Tenant tenant = new Tenant();
        tenant.setPropertyId(propertyId);
        tenant.setRoomId(roomId);
        tenant.setFullName(requiredText("Full name: "));
        tenant.setEmail(requiredText("Email: "));
        tenant.setPhone(requiredText("Phone: "));
        tenant.setEmergencyContact(optionalText("Emergency contact (optional): "));
        tenant.setPermanentAddress(optionalText("Permanent address (optional): "));
        tenant.setIdProofNumber(optionalText("ID proof number (optional): "));
        tenant.setMoveInDate(moveInDate);
        tenant.setMonthlyRent(readMoneyWithDefault("Agreed monthly rent", suggestedRent));
        tenant.setSecurityDeposit(readNonNegativeMoney("Security deposit: "));
        tenant.setTenantStatus(status);
        return tenant;
    }

    private static void bindTenant(PreparedStatement statement, Tenant tenant) throws SQLException {
        statement.setInt(1, tenant.getPropertyId());
        statement.setInt(2, tenant.getRoomId());
        statement.setString(3, tenant.getFullName().trim());
        statement.setString(4, tenant.getEmail().trim().toLowerCase(Locale.ROOT));
        statement.setString(5, tenant.getPhone().trim());
        statement.setString(6, nullableTrim(tenant.getEmergencyContact()));
        statement.setString(7, nullableTrim(tenant.getPermanentAddress()));
        statement.setString(8, nullableTrim(tenant.getIdProofNumber()));
        statement.setDate(9, tenant.getMoveInDate());
        statement.setDouble(10, tenant.getMonthlyRent());
        statement.setDouble(11, tenant.getSecurityDeposit());
    }

    private static void viewTenants() throws SQLException {
        listTenants(null);
    }

    private static void searchTenant() throws SQLException {
        String query = requiredText("Search by tenant name, phone, or email: ");
        listTenants(query);
    }

    private static void listTenants(String search) throws SQLException {
        Landlord landlord = requireLogin();
        String sql = tenantSelect() + "WHERE p.landlord_id = ? ";
        if (search != null) {
            sql += "AND (t.full_name LIKE ? OR t.phone LIKE ? OR t.email LIKE ?) ";
        }
        sql += "ORDER BY t.full_name";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, landlord.getLandlordId());
            if (search != null) {
                String pattern = "%" + search.trim() + "%";
                statement.setString(2, pattern);
                statement.setString(3, pattern);
                statement.setString(4, pattern);
            }
            try (ResultSet rs = statement.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    printTenant(rs);
                    count++;
                }
                if (count == 0) {
                    System.out.println(search == null ? "No tenants found." : "No matching tenants.");
                }
            }
        }
    }

    private static String tenantSelect() {
        return "SELECT t.tenant_id, t.property_id, p.property_name, t.room_id, r.room_number, "
                + "t.full_name, t.email, t.phone, t.emergency_contact, t.permanent_address, "
                + "t.id_proof_number, t.move_in_date, t.move_out_date, t.monthly_rent, "
                + "t.security_deposit, t.tenant_status, "
                + "COALESCE(NULLIF(t.monthly_rent, 0), NULLIF(r.monthly_rent, 0), p.monthly_rent) AS effective_rent "
                + "FROM tenants t JOIN properties p ON p.property_id = t.property_id "
                + "JOIN rooms r ON r.room_id = t.room_id ";
    }

    private static void printTenant(ResultSet rs) throws SQLException {
        System.out.printf(Locale.ROOT, "#%d | %s | %s | %s / %s | rent %.2f | %s%n",
                rs.getInt("tenant_id"), rs.getString("full_name"), rs.getString("phone"),
                rs.getString("property_name"), rs.getString("room_number"),
                rs.getDouble("effective_rent"), rs.getString("tenant_status"));
        System.out.println("  Email: " + rs.getString("email")
                + " | Emergency: " + valueOrDash(rs.getString("emergency_contact"))
                + " | Address: " + valueOrDash(rs.getString("permanent_address")));
        System.out.println("  Move-in: " + rs.getDate("move_in_date")
                + " | Move-out: " + rs.getDate("move_out_date")
                + " | Deposit: " + rs.getBigDecimal("security_deposit"));
    }

    private static void updateTenant() throws SQLException {
        Landlord landlord = requireLogin();
        int tenantId = readPositiveInt("Tenant ID: ");
        Tenant tenant = getOwnedTenant(tenantId, landlord.getLandlordId());
        if (tenant == null) {
            System.out.println("Tenant not found for this landlord.");
            return;
        }
        tenant.setFullName(requiredText("Updated full name: "));
        tenant.setEmail(requiredText("Updated email: "));
        tenant.setPhone(requiredText("Updated phone: "));
        tenant.setEmergencyContact(optionalText("Updated emergency contact (optional): "));
        tenant.setPermanentAddress(optionalText("Updated permanent address (optional): "));
        tenant.setIdProofNumber(optionalText("Updated ID proof number (optional): "));
        tenant.setMonthlyRent(readMoneyWithDefault("Updated monthly rent", tenant.getMonthlyRent()));
        tenant.setSecurityDeposit(readMoneyWithDefault("Updated security deposit", tenant.getSecurityDeposit()));
        tenant.setTenantStatus(readChoice("Tenant status", new String[]{"Active", "Pending", "Moved Out"}));
        String error = TENANT_SERVICE.validateUpdate(tenant);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        ensureTenantEmailAvailable(tenant.getEmail(), tenantId);
        updateTenantTransaction(tenant);
    }

    private static void updateTenantTransaction(Tenant tenant) throws SQLException {
        String updateSql = "UPDATE tenants SET full_name = ?, email = ?, phone = ?, emergency_contact = ?, "
                + "permanent_address = ?, id_proof_number = ?, monthly_rent = ?, security_deposit = ?, "
                + "tenant_status = ?, move_out_date = ? WHERE tenant_id = ?";
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                assertTenantOwned(connection, tenant.getTenantId(), currentLandlord.getLandlordId());
                if ("Active".equalsIgnoreCase(tenant.getTenantStatus())) {
                    assertRoomVacantOrAssignedToTenant(connection, tenant.getRoomId(), tenant.getPropertyId(),
                            tenant.getTenantId());
                }
                try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                    statement.setString(1, tenant.getFullName().trim());
                    statement.setString(2, tenant.getEmail().trim().toLowerCase(Locale.ROOT));
                    statement.setString(3, tenant.getPhone().trim());
                    statement.setString(4, nullableTrim(tenant.getEmergencyContact()));
                    statement.setString(5, nullableTrim(tenant.getPermanentAddress()));
                    statement.setString(6, nullableTrim(tenant.getIdProofNumber()));
                    statement.setDouble(7, tenant.getMonthlyRent());
                    statement.setDouble(8, tenant.getSecurityDeposit());
                    statement.setString(9, tenant.getTenantStatus());
                    statement.setDate(10, "Moved Out".equalsIgnoreCase(tenant.getTenantStatus())
                            ? Date.valueOf(LocalDate.now()) : null);
                    statement.setInt(11, tenant.getTenantId());
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Tenant was not updated.");
                    }
                }
                synchronizeRoomStatus(connection, tenant.getRoomId());
                connection.commit();
                System.out.println("Tenant profile updated; room occupancy synchronized.");
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private static void deleteTenant() throws SQLException {
        Landlord landlord = requireLogin();
        int tenantId = readPositiveInt("Tenant ID to delete: ");
        Tenant tenant = getOwnedTenant(tenantId, landlord.getLandlordId());
        if (tenant == null) {
            System.out.println("Tenant not found for this landlord.");
            return;
        }
        if (!confirm("Delete tenant #" + tenantId + "? Related payments can block deletion (y/N): ")) {
            System.out.println("Deletion cancelled.");
            return;
        }
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                assertTenantOwned(connection, tenantId, landlord.getLandlordId());
                try (PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM tenants WHERE tenant_id = ?")) {
                    statement.setInt(1, tenantId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Tenant was not deleted.");
                    }
                }
                synchronizeRoomStatus(connection, tenant.getRoomId());
                connection.commit();
                System.out.println("Tenant deleted; room occupancy synchronized.");
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private static void assignTenant() throws SQLException {
        Landlord landlord = requireLogin();
        int tenantId = readPositiveInt("Tenant ID: ");
        Tenant tenant = getOwnedTenant(tenantId, landlord.getLandlordId());
        if (tenant == null) {
            System.out.println("Tenant not found for this landlord.");
            return;
        }
        int propertyId = readPositiveInt("Destination property ID: ");
        requireOwnedProperty(propertyId, landlord.getLandlordId());
        int roomId = readPositiveInt("Destination vacant room ID: ");
        RoomDetails room = requireOwnedRoom(roomId, landlord.getLandlordId(), true);
        if (room.propertyId != propertyId) {
            throw new IllegalArgumentException("The selected room does not belong to that property.");
        }
        double rent = readMoneyWithDefault("Agreed monthly rent", room.effectiveRent);
        Date moveIn = readDate("Move-in date (YYYY-MM-DD): ");
        String sql = "UPDATE tenants SET property_id = ?, room_id = ?, monthly_rent = ?, move_in_date = ?, "
                + "move_out_date = NULL, tenant_status = 'Active' WHERE tenant_id = ?";
        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int previousRoomId = tenant.getRoomId();
                if (previousRoomId != roomId) {
                    assertRoomVacant(connection, roomId, propertyId);
                } else {
                    assertRoomVacantOrAssignedToTenant(connection, roomId, propertyId, tenantId);
                }
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, propertyId);
                    statement.setInt(2, roomId);
                    statement.setDouble(3, rent);
                    statement.setDate(4, moveIn);
                    statement.setInt(5, tenantId);
                    if (statement.executeUpdate() != 1) {
                        throw new SQLException("Tenant assignment was not updated.");
                    }
                }
                if (previousRoomId != roomId) {
                    synchronizeRoomStatus(connection, previousRoomId);
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE rooms SET room_status = 'Occupied' WHERE room_id = ?")) {
                    statement.setInt(1, roomId);
                    statement.executeUpdate();
                }
                connection.commit();
                System.out.println("Tenant assigned to property #" + propertyId + ", room #" + roomId + ".");
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private static Tenant getOwnedTenant(int tenantId, int landlordId) throws SQLException {
        String sql = tenantSelect() + "WHERE t.tenant_id = ? AND p.landlord_id = ?";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, tenantId);
            statement.setInt(2, landlordId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
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
                tenant.setMonthlyRent(rs.getDouble("monthly_rent"));
                tenant.setSecurityDeposit(rs.getDouble("security_deposit"));
                tenant.setTenantStatus(rs.getString("tenant_status"));
                return tenant;
            }
        }
    }

    private static void ensureTenantEmailAvailable(String email, int exceptTenantId) throws SQLException {
        String sql = "SELECT tenant_id FROM tenants WHERE LOWER(email) = LOWER(?) AND tenant_id <> ?";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email.trim());
            statement.setInt(2, exceptTenantId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    throw new IllegalArgumentException("Another tenant already uses that email address.");
                }
            }
        }
    }

    private static void assertTenantOwned(Connection connection, int tenantId, int landlordId)
            throws SQLException {
        String sql = "SELECT t.room_id FROM tenants t JOIN properties p ON p.property_id = t.property_id "
                + "WHERE t.tenant_id = ? AND p.landlord_id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, tenantId);
            statement.setInt(2, landlordId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Tenant not found for this landlord.");
                }
            }
        }
    }

    private static void assertRoomVacant(Connection connection, int roomId, int propertyId)
            throws SQLException {
        String sql = "SELECT room_status FROM rooms WHERE room_id = ? AND property_id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, roomId);
            statement.setInt(2, propertyId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next() || !"Vacant".equalsIgnoreCase(rs.getString("room_status"))) {
                    throw new IllegalArgumentException("Room is missing, belongs to another property, or is not vacant.");
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT tenant_id FROM tenants WHERE room_id = ? AND tenant_status = 'Active' FOR UPDATE")) {
            statement.setInt(1, roomId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    throw new IllegalArgumentException("An active tenant is already assigned to this room.");
                }
            }
        }
    }

    private static void assertRoomVacantOrAssignedToTenant(Connection connection, int roomId,
            int propertyId, int tenantId) throws SQLException {
        String sql = "SELECT room_status FROM rooms WHERE room_id = ? AND property_id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, roomId);
            statement.setInt(2, propertyId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Room does not belong to the selected property.");
                }
            }
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT tenant_id FROM tenants WHERE room_id = ? AND tenant_status = 'Active' "
                        + "AND tenant_id <> ? FOR UPDATE")) {
            statement.setInt(1, roomId);
            statement.setInt(2, tenantId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    throw new IllegalArgumentException("Another active tenant is already assigned to this room.");
                }
            }
        }
    }

    private static void synchronizeRoomStatus(Connection connection, int roomId) throws SQLException {
        if (roomId <= 0) {
            return;
        }
        String sql = "UPDATE rooms SET room_status = CASE WHEN EXISTS "
                + "(SELECT 1 FROM tenants WHERE room_id = ? AND tenant_status = 'Active') "
                + "THEN 'Occupied' ELSE 'Vacant' END WHERE room_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, roomId);
            statement.setInt(2, roomId);
            statement.executeUpdate();
        }
    }

    private static void viewMonthlyRent() throws SQLException {
        Landlord landlord = requireLogin();
        String sql = "SELECT t.tenant_id, t.full_name, p.property_name, r.room_number, "
                + "COALESCE(NULLIF(t.monthly_rent, 0), NULLIF(r.monthly_rent, 0), p.monthly_rent) AS rent "
                + "FROM tenants t JOIN properties p ON p.property_id = t.property_id "
                + "JOIN rooms r ON r.room_id = t.room_id "
                + "WHERE p.landlord_id = ? AND t.tenant_status = 'Active' ORDER BY t.full_name";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, landlord.getLandlordId());
            try (ResultSet rs = statement.executeQuery()) {
                BigDecimal total = BigDecimal.ZERO;
                int count = 0;
                while (rs.next()) {
                    BigDecimal rent = rs.getBigDecimal("rent");
                    total = total.add(rent);
                    System.out.printf(Locale.ROOT, "Tenant #%d %s | %s / %s | %.2f%n",
                            rs.getInt("tenant_id"), rs.getString("full_name"), rs.getString("property_name"),
                            rs.getString("room_number"), rent);
                    count++;
                }
                System.out.printf(Locale.ROOT, "Active tenants: %d | Monthly rent total: %.2f%n", count, total);
            }
        }
    }

    private static void viewExpectedRent() throws SQLException {
        YearMonth month = readMonth();
        System.out.printf(Locale.ROOT, "Expected rent for %s: %.2f%n",
                month.format(DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH)),
                rentTotalForMonth(month));
    }

    private static void viewCollectedRent() throws SQLException {
        YearMonth month = readMonth();
        System.out.printf(Locale.ROOT, "Collected (Paid) rent for %s: %.2f%n",
                month.format(DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH)),
                collectedForMonth(month));
    }

    private static void viewPendingDues() throws SQLException {
        Landlord landlord = requireLogin();
        YearMonth month = readMonth();
        String monthText = month.format(DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH));
        String sql = "SELECT t.tenant_id, t.full_name, "
                + "COALESCE(NULLIF(t.monthly_rent, 0), NULLIF(r.monthly_rent, 0), p.monthly_rent) AS expected, "
                + "COALESCE(SUM(CASE WHEN pay.payment_status = 'Paid' THEN pay.amount ELSE 0 END), 0) AS paid "
                + "FROM tenants t JOIN properties p ON p.property_id = t.property_id "
                + "JOIN rooms r ON r.room_id = t.room_id "
                + "LEFT JOIN payments pay ON pay.tenant_id = t.tenant_id AND pay.property_id = t.property_id "
                + "AND pay.payment_month = ? "
                + "WHERE p.landlord_id = ? AND t.tenant_status IN ('Active', 'Moved Out') "
                + "AND t.move_in_date <= ? AND (t.move_out_date IS NULL OR t.move_out_date >= ?) "
                + "GROUP BY t.tenant_id, t.full_name, t.monthly_rent, r.monthly_rent, p.monthly_rent "
                + "ORDER BY t.full_name";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, monthText);
            statement.setInt(2, landlord.getLandlordId());
            statement.setDate(3, Date.valueOf(month.atEndOfMonth()));
            statement.setDate(4, Date.valueOf(month.atDay(1)));
            try (ResultSet rs = statement.executeQuery()) {
                BigDecimal totalDue = BigDecimal.ZERO;
                int count = 0;
                while (rs.next()) {
                    BigDecimal due = rs.getBigDecimal("expected").subtract(rs.getBigDecimal("paid"));
                    if (due.signum() > 0) {
                        System.out.printf(Locale.ROOT, "Tenant #%d %s | expected %.2f | paid %.2f | due %.2f%n",
                                rs.getInt("tenant_id"), rs.getString("full_name"),
                                rs.getBigDecimal("expected"), rs.getBigDecimal("paid"), due);
                        totalDue = totalDue.add(due);
                        count++;
                    }
                }
                System.out.printf(Locale.ROOT, "%s: %d tenant(s), total pending %.2f%n",
                        monthText, count, totalDue);
            }
        }
    }

    private static void viewOverdueRent() throws SQLException {
        Landlord landlord = requireLogin();
        String sql = "SELECT pay.payment_id, pay.tenant_id, t.full_name, pay.amount, pay.payment_month, "
                + "pay.payment_status FROM payments pay "
                + "JOIN tenants t ON t.tenant_id = pay.tenant_id "
                + "JOIN properties p ON p.property_id = pay.property_id "
                + "WHERE p.landlord_id = ? AND (pay.payment_status = 'Overdue' OR "
                + "(pay.payment_status = 'Pending' AND STR_TO_DATE(pay.payment_month, '%M %Y') "
                + "< STR_TO_DATE(DATE_FORMAT(CURRENT_DATE, '%M %Y'), '%M %Y'))) "
                + "ORDER BY pay.payment_month, t.full_name";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, landlord.getLandlordId());
            try (ResultSet rs = statement.executeQuery()) {
                int count = 0;
                BigDecimal total = BigDecimal.ZERO;
                while (rs.next()) {
                    System.out.printf(Locale.ROOT, "Payment #%d | Tenant #%d %s | %s | %.2f | %s%n",
                            rs.getInt("payment_id"), rs.getInt("tenant_id"), rs.getString("full_name"),
                            rs.getString("payment_month"), rs.getBigDecimal("amount"),
                            rs.getString("payment_status"));
                    total = total.add(rs.getBigDecimal("amount"));
                    count++;
                }
                System.out.printf(Locale.ROOT, "Overdue payment records: %d | amount %.2f%n", count, total);
            }
        }
    }

    private static void updateRoomRent() throws SQLException {
        Landlord landlord = requireLogin();
        int roomId = readPositiveInt("Room ID: ");
        RoomDetails room = requireOwnedRoom(roomId, landlord.getLandlordId(), false);
        double rent = readNonNegativeMoney("New room monthly rent (0 uses property fallback): ");
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE rooms SET monthly_rent = ? WHERE room_id = ?")) {
            statement.setDouble(1, rent);
            statement.setInt(2, room.roomId);
            System.out.println(statement.executeUpdate() == 1
                    ? "Room rent updated."
                    : "No room rent was updated.");
        }
    }

    private static BigDecimal rentTotalForMonth(YearMonth month) throws SQLException {
        Landlord landlord = requireLogin();
        String sql = "SELECT COALESCE(SUM(COALESCE(NULLIF(t.monthly_rent, 0), "
                + "NULLIF(r.monthly_rent, 0), p.monthly_rent)), 0) AS expected "
                + "FROM tenants t JOIN properties p ON p.property_id = t.property_id "
                + "JOIN rooms r ON r.room_id = t.room_id "
                + "WHERE p.landlord_id = ? AND t.tenant_status IN ('Active', 'Moved Out') "
                + "AND t.move_in_date <= ? AND (t.move_out_date IS NULL OR t.move_out_date >= ?)";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, landlord.getLandlordId());
            statement.setDate(2, Date.valueOf(month.atEndOfMonth()));
            statement.setDate(3, Date.valueOf(month.atDay(1)));
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getBigDecimal("expected");
            }
        }
    }

    private static BigDecimal collectedForMonth(YearMonth month) throws SQLException {
        Landlord landlord = requireLogin();
        String monthText = month.format(DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH));
        String sql = "SELECT COALESCE(SUM(pay.amount), 0) AS collected FROM payments pay "
                + "JOIN properties p ON p.property_id = pay.property_id "
                + "WHERE p.landlord_id = ? AND pay.payment_month = ? AND pay.payment_status = 'Paid'";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, landlord.getLandlordId());
            statement.setString(2, monthText);
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getBigDecimal("collected");
            }
        }
    }

    private static void recordPayment() throws SQLException {
        Landlord landlord = requireLogin();
        Tenant tenant = promptPaymentTenant(landlord.getLandlordId());
        double amount = readPositiveMoney("Payment amount (suggested " + tenant.getMonthlyRent() + "): ");
        String date = readDate("Payment date (YYYY-MM-DD): ").toString();
        String month = readMonth().format(DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH));
        String mode = readChoice("Payment method", PaymentService.ALLOWED_MODES.toArray(new String[0]));
        String reference = optionalText("Transaction reference (required for paid UPI/Bank Transfer): ");
        String status = readChoice("Payment status", PaymentService.ALLOWED_STATUSES.toArray(new String[0]));
        String remarks = optionalText("Remarks (optional): ");
        Payment payment = new Payment(tenant.getTenantId(), tenant.getPropertyId(), tenant.getRoomId(),
                amount, date, month, mode, reference, status, remarks);
        String error = PAYMENT_SERVICE.validatePayment(payment);
        if (error != null) {
            throw new IllegalArgumentException(error);
        }
        int paymentId = PAYMENT_SERVICE.recordPayment(payment);
        System.out.println("Payment recorded with ID " + paymentId + ".");
    }

    private static Tenant promptPaymentTenant(int landlordId) throws SQLException {
        int tenantId = readPositiveInt("Tenant ID: ");
        Tenant tenant = getOwnedTenant(tenantId, landlordId);
        if (tenant == null) {
            throw new IllegalArgumentException("Tenant not found for this landlord.");
        }
        if ("Moved Out".equalsIgnoreCase(tenant.getTenantStatus())) {
            throw new IllegalArgumentException("A moved-out tenant cannot be selected for a new rent payment.");
        }
        tenant.setMonthlyRent(getEffectiveRent(tenantId, landlordId));
        return tenant;
    }

    private static double getEffectiveRent(int tenantId, int landlordId) throws SQLException {
        String sql = "SELECT COALESCE(NULLIF(t.monthly_rent, 0), NULLIF(r.monthly_rent, 0), "
                + "p.monthly_rent) AS effective_rent FROM tenants t "
                + "JOIN properties p ON p.property_id = t.property_id "
                + "JOIN rooms r ON r.room_id = t.room_id "
                + "WHERE t.tenant_id = ? AND p.landlord_id = ?";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, tenantId);
            statement.setInt(2, landlordId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Tenant assignment is no longer available.");
                }
                return rs.getDouble("effective_rent");
            }
        }
    }

    private static void viewPaymentHistory() throws SQLException {
        Landlord landlord = requireLogin();
        String sql = paymentSelect() + "WHERE p.property_id IN "
                + "(SELECT property_id FROM properties WHERE landlord_id = ?) "
                + "ORDER BY p.payment_date DESC, p.payment_id DESC";
        printPayments(sql, landlord.getLandlordId());
    }

    private static void searchPayment() throws SQLException {
        Landlord landlord = requireLogin();
        String keyword = requiredText("Search by tenant, property, payment month, status, or reference: ");
        String sql = paymentSelect() + "WHERE pr.landlord_id = ? AND "
                + "(t.full_name LIKE ? OR pr.property_name LIKE ? OR p.payment_month LIKE ? "
                + "OR p.payment_status LIKE ? OR p.transaction_ref LIKE ?) "
                + "ORDER BY p.payment_date DESC, p.payment_id DESC";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, landlord.getLandlordId());
            String pattern = "%" + keyword.trim() + "%";
            for (int i = 2; i <= 6; i++) {
                statement.setString(i, pattern);
            }
            printPayments(statement);
        }
    }

    private static String paymentSelect() {
        return "SELECT p.payment_id, p.tenant_id, t.full_name, p.property_id, pr.property_name, "
                + "p.room_id, r.room_number, p.amount, p.payment_date, p.payment_month, p.payment_mode, "
                + "p.transaction_ref, p.payment_status, p.remarks FROM payments p "
                + "JOIN tenants t ON t.tenant_id = p.tenant_id "
                + "JOIN properties pr ON pr.property_id = p.property_id "
                + "JOIN rooms r ON r.room_id = p.room_id ";
    }

    private static void printPayments(String sql, int landlordId) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, landlordId);
            printPayments(statement);
        }
    }

    private static void printPayments(PreparedStatement statement) throws SQLException {
        try (ResultSet rs = statement.executeQuery()) {
            int count = 0;
            while (rs.next()) {
                printPayment(rs);
                count++;
            }
            if (count == 0) {
                System.out.println("No matching payment records.");
            }
        }
    }

    private static void printPayment(ResultSet rs) throws SQLException {
        System.out.printf(Locale.ROOT,
                "#%d | tenant #%d %s | %s / %s | %.2f | %s | %s | %s | ref %s%n",
                rs.getInt("payment_id"), rs.getInt("tenant_id"), rs.getString("full_name"),
                rs.getString("property_name"), rs.getString("room_number"), rs.getBigDecimal("amount"),
                rs.getString("payment_date"), rs.getString("payment_month"),
                rs.getString("payment_status"), valueOrDash(rs.getString("transaction_ref")));
    }

    private static void updatePayment() throws SQLException {
        Landlord landlord = requireLogin();
        int paymentId = readPositiveInt("Payment ID: ");
        Payment payment = getOwnedPayment(paymentId, landlord.getLandlordId());
        if (payment == null) {
            System.out.println("Payment not found for this landlord.");
            return;
        }
        Tenant tenant = promptPaymentTenant(landlord.getLandlordId());
        payment.setTenantId(tenant.getTenantId());
        payment.setPropertyId(tenant.getPropertyId());
        payment.setRoomId(tenant.getRoomId());
        payment.setAmount(readPositiveMoney("Updated amount: "));
        payment.setPaymentDate(readDate("Payment date (YYYY-MM-DD): ").toString());
        payment.setPaymentMonth(readMonth().format(DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH)));
        payment.setPaymentMode(readChoice("Payment method", PaymentService.ALLOWED_MODES.toArray(new String[0])));
        payment.setTransactionRef(optionalText("Transaction reference: "));
        payment.setPaymentStatus(readChoice("Payment status", PaymentService.ALLOWED_STATUSES.toArray(new String[0])));
        payment.setRemarks(optionalText("Remarks (optional): "));
        boolean updated = PAYMENT_SERVICE.updatePayment(payment);
        System.out.println(updated ? "Payment updated." : "Payment was not updated.");
    }

    private static void updatePaymentStatus() throws SQLException {
        Landlord landlord = requireLogin();
        int paymentId = readPositiveInt("Payment ID: ");
        if (getOwnedPayment(paymentId, landlord.getLandlordId()) == null) {
            System.out.println("Payment not found for this landlord.");
            return;
        }
        String status = readChoice("New status", PaymentService.ALLOWED_STATUSES.toArray(new String[0]));
        System.out.println(PAYMENT_SERVICE.updatePaymentStatus(paymentId, status)
                ? "Payment status updated."
                : "Payment status was not updated.");
    }

    private static void deletePayment() throws SQLException {
        Landlord landlord = requireLogin();
        int paymentId = readPositiveInt("Payment ID to delete: ");
        if (getOwnedPayment(paymentId, landlord.getLandlordId()) == null) {
            System.out.println("Payment not found for this landlord.");
            return;
        }
        if (!confirm("Delete this payment record permanently? (y/N): ")) {
            System.out.println("Deletion cancelled.");
            return;
        }
        System.out.println(PAYMENT_SERVICE.deletePayment(paymentId)
                ? "Payment deleted."
                : "Payment was not deleted.");
    }

    private static Payment getOwnedPayment(int paymentId, int landlordId) throws SQLException {
        String sql = paymentSelect() + "WHERE p.payment_id = ? AND pr.landlord_id = ?";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, paymentId);
            statement.setInt(2, landlordId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Payment payment = new Payment(paymentId, rs.getInt("tenant_id"), rs.getInt("property_id"),
                        rs.getInt("room_id"), rs.getDouble("amount"), rs.getDate("payment_date").toString(),
                        rs.getString("payment_month"), rs.getString("payment_mode"),
                        rs.getString("transaction_ref"), rs.getString("payment_status"),
                        rs.getString("remarks"), null);
                payment.setTenantName(rs.getString("full_name"));
                payment.setPropertyName(rs.getString("property_name"));
                payment.setRoomNumber(rs.getString("room_number"));
                return payment;
            }
        }
    }

    private static void addReminder() throws SQLException {
        Landlord landlord = requireLogin();
        int tenantId = readOptionalPositiveInt("Tenant ID (0 for a general reminder): ");
        if (tenantId > 0 && getOwnedTenant(tenantId, landlord.getLandlordId()) == null) {
            throw new IllegalArgumentException("Tenant not found for this landlord.");
        }
        String title = requiredText("Title: ");
        String message = requiredText("Description/message: ");
        Date dueDate = readDate("Due date (YYYY-MM-DD): ");
        String type = readChoice("Reminder type", REMINDER_TYPES);
        String sql = "INSERT INTO reminders (landlord_id, tenant_id, title, message, due_date, "
                + "reminder_type, reminder_status) VALUES (?, ?, ?, ?, ?, ?, 'Pending')";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, landlord.getLandlordId());
            if (tenantId == 0) {
                statement.setNull(2, java.sql.Types.INTEGER);
            } else {
                statement.setInt(2, tenantId);
            }
            statement.setString(3, title.trim());
            statement.setString(4, message.trim());
            statement.setDate(5, dueDate);
            statement.setString(6, type);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                System.out.println(keys.next()
                        ? "Reminder added with ID " + keys.getInt(1) + "."
                        : "Reminder added.");
            }
        }
    }

    private static void viewReminders() throws SQLException {
        Landlord landlord = requireLogin();
        String sql = "SELECT r.reminder_id, r.tenant_id, t.full_name, r.title, r.message, r.due_date, "
                + "r.reminder_type, r.reminder_status FROM reminders r LEFT JOIN tenants t "
                + "ON t.tenant_id = r.tenant_id WHERE r.landlord_id = ? ORDER BY r.due_date, r.reminder_id";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, landlord.getLandlordId());
            printReminders(statement);
        }
    }

    private static void printReminders(PreparedStatement statement) throws SQLException {
        try (ResultSet rs = statement.executeQuery()) {
            int count = 0;
            while (rs.next()) {
                System.out.printf("#%d | due %s | %s | %s | tenant %s%n",
                        rs.getInt("reminder_id"), rs.getDate("due_date"), rs.getString("reminder_type"),
                        rs.getString("reminder_status"), rs.getString("full_name") == null
                                ? "General" : rs.getString("full_name"));
                System.out.println("  " + rs.getString("title") + ": " + rs.getString("message"));
                count++;
            }
            if (count == 0) {
                System.out.println("No reminders found.");
            }
        }
    }

    private static void updateReminder() throws SQLException {
        Landlord landlord = requireLogin();
        int reminderId = readPositiveInt("Reminder ID: ");
        if (!reminderBelongsToLandlord(reminderId, landlord.getLandlordId())) {
            System.out.println("Reminder not found for this landlord.");
            return;
        }
        int tenantId = readOptionalPositiveInt("Tenant ID (0 for a general reminder): ");
        if (tenantId > 0 && getOwnedTenant(tenantId, landlord.getLandlordId()) == null) {
            throw new IllegalArgumentException("Tenant not found for this landlord.");
        }
        String title = requiredText("Updated title: ");
        String message = requiredText("Updated description/message: ");
        Date dueDate = readDate("Updated due date (YYYY-MM-DD): ");
        String type = readChoice("Reminder type", REMINDER_TYPES);
        String status = readChoice("Reminder status", REMINDER_STATUSES);
        String sql = "UPDATE reminders SET tenant_id = ?, title = ?, message = ?, due_date = ?, "
                + "reminder_type = ?, reminder_status = ? WHERE reminder_id = ? AND landlord_id = ?";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            if (tenantId == 0) {
                statement.setNull(1, java.sql.Types.INTEGER);
            } else {
                statement.setInt(1, tenantId);
            }
            statement.setString(2, title.trim());
            statement.setString(3, message.trim());
            statement.setDate(4, dueDate);
            statement.setString(5, type);
            statement.setString(6, status);
            statement.setInt(7, reminderId);
            statement.setInt(8, landlord.getLandlordId());
            System.out.println(statement.executeUpdate() == 1 ? "Reminder updated." : "Reminder was not updated.");
        }
    }

    private static void deleteReminder() throws SQLException {
        Landlord landlord = requireLogin();
        int reminderId = readPositiveInt("Reminder ID to delete: ");
        if (!confirm("Delete this reminder? (y/N): ")) {
            System.out.println("Deletion cancelled.");
            return;
        }
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "DELETE FROM reminders WHERE reminder_id = ? AND landlord_id = ?")) {
            statement.setInt(1, reminderId);
            statement.setInt(2, landlord.getLandlordId());
            System.out.println(statement.executeUpdate() == 1
                    ? "Reminder deleted."
                    : "Reminder not found for this landlord.");
        }
    }

    private static void toggleReminderStatus() throws SQLException {
        Landlord landlord = requireLogin();
        int reminderId = readPositiveInt("Reminder ID: ");
        String current;
        String selectSql = "SELECT reminder_status FROM reminders WHERE reminder_id = ? AND landlord_id = ?";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(selectSql)) {
            statement.setInt(1, reminderId);
            statement.setInt(2, landlord.getLandlordId());
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Reminder not found for this landlord.");
                    return;
                }
                current = rs.getString("reminder_status");
            }
        }
        String next = switch (current) {
            case "Pending" -> "Sent";
            case "Sent" -> "Resolved";
            default -> "Pending";
        };
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE reminders SET reminder_status = ? WHERE reminder_id = ? AND landlord_id = ?")) {
            statement.setString(1, next);
            statement.setInt(2, reminderId);
            statement.setInt(3, landlord.getLandlordId());
            System.out.println(statement.executeUpdate() == 1
                    ? "Reminder status changed from " + current + " to " + next + "."
                    : "Reminder status was not changed.");
        }
    }

    private static boolean reminderBelongsToLandlord(int reminderId, int landlordId) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT reminder_id FROM reminders WHERE reminder_id = ? AND landlord_id = ?")) {
            statement.setInt(1, reminderId);
            statement.setInt(2, landlordId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void showTotalProperties() throws SQLException {
        System.out.println("Total properties: " + countOwned("properties"));
    }

    private static void showTotalRooms() throws SQLException {
        System.out.println("Total rooms: " + countOwnedRooms());
    }

    private static void showTotalTenants() throws SQLException {
        System.out.println("Total tenants: " + countOwnedTenants());
    }

    private static void showTotalRentCollected() throws SQLException {
        Landlord landlord = requireLogin();
        String sql = "SELECT COALESCE(SUM(pay.amount), 0) FROM payments pay "
                + "JOIN properties p ON p.property_id = pay.property_id "
                + "WHERE p.landlord_id = ? AND pay.payment_status = 'Paid'";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, landlord.getLandlordId());
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                System.out.printf(Locale.ROOT, "Total rent collected: %.2f%n", rs.getBigDecimal(1));
            }
        }
    }

    private static void showPendingPayments() throws SQLException {
        Landlord landlord = requireLogin();
        String sql = "SELECT COUNT(*) AS payment_count, COALESCE(SUM(pay.amount), 0) AS amount "
                + "FROM payments pay JOIN properties p ON p.property_id = pay.property_id "
                + "WHERE p.landlord_id = ? AND pay.payment_status = 'Pending'";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, landlord.getLandlordId());
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                System.out.printf(Locale.ROOT, "Pending payment records: %d | amount %.2f%n",
                        rs.getInt("payment_count"), rs.getBigDecimal("amount"));
            }
        }
    }

    private static void showReminderStatistics() throws SQLException {
        Landlord landlord = requireLogin();
        String sql = "SELECT reminder_status, COUNT(*) AS total FROM reminders "
                + "WHERE landlord_id = ? GROUP BY reminder_status ORDER BY reminder_status";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, landlord.getLandlordId());
            try (ResultSet rs = statement.executeQuery()) {
                int total = 0;
                while (rs.next()) {
                    int count = rs.getInt("total");
                    System.out.println(rs.getString("reminder_status") + ": " + count);
                    total += count;
                }
                System.out.println("Total reminders: " + total);
            }
        }
    }

    private static long countOwned(String table) throws SQLException {
        Landlord landlord = requireLogin();
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE landlord_id = ?";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, landlord.getLandlordId());
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private static long countOwnedRooms() throws SQLException {
        Landlord landlord = requireLogin();
        String sql = "SELECT COUNT(*) FROM rooms r JOIN properties p ON p.property_id = r.property_id "
                + "WHERE p.landlord_id = ?";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, landlord.getLandlordId());
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private static long countOwnedTenants() throws SQLException {
        Landlord landlord = requireLogin();
        String sql = "SELECT COUNT(*) FROM tenants t JOIN properties p ON p.property_id = t.property_id "
                + "WHERE p.landlord_id = ?";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, landlord.getLandlordId());
            try (ResultSet rs = statement.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    private static void requireOwnedProperty(int propertyId, int landlordId) throws SQLException {
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT property_id FROM properties WHERE property_id = ? AND landlord_id = ?")) {
            statement.setInt(1, propertyId);
            statement.setInt(2, landlordId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Property not found for this landlord.");
                }
            }
        }
    }

    private static RoomDetails requireOwnedRoom(int roomId, int landlordId, boolean mustBeVacant)
            throws SQLException {
        String sql = "SELECT r.room_id, r.property_id, r.room_number, r.monthly_rent, r.room_status, "
                + "COALESCE(NULLIF(r.monthly_rent, 0), p.monthly_rent) AS effective_rent "
                + "FROM rooms r JOIN properties p ON p.property_id = r.property_id "
                + "WHERE r.room_id = ? AND p.landlord_id = ?";
        try (Connection connection = DBConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, roomId);
            statement.setInt(2, landlordId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Room not found for this landlord.");
                }
                RoomDetails room = new RoomDetails(rs.getInt("room_id"), rs.getInt("property_id"),
                        rs.getDouble("effective_rent"), rs.getString("room_status"));
                if (mustBeVacant && !"Vacant".equalsIgnoreCase(room.status)) {
                    throw new IllegalArgumentException("Room is not vacant.");
                }
                return room;
            }
        }
    }

    private static final class RoomDetails {
        private final int roomId;
        private final int propertyId;
        private final double effectiveRent;
        private final String status;

        private RoomDetails(int roomId, int propertyId, double effectiveRent, String status) {
            this.roomId = roomId;
            this.propertyId = propertyId;
            this.effectiveRent = effectiveRent;
            this.status = status;
        }
    }

    private static Landlord requireLogin() {
        if (currentLandlord == null) {
            throw new IllegalArgumentException("Please register or log in from Landlord Management first.");
        }
        return currentLandlord;
    }

    private static int readInt(String prompt) {
        while (!inputClosed) {
            String text = readLine(prompt);
            if (text == null) {
                return -1;
            }
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException e) {
                System.out.println("Enter a valid whole number.");
            }
        }
        return -1;
    }

    private static int readPositiveInt(String prompt) {
        while (true) {
            int value = readInt(prompt);
            if (inputClosed || value > 0) {
                return value;
            }
            System.out.println("Enter a number greater than zero.");
        }
    }

    private static int readOptionalPositiveInt(String prompt) {
        while (true) {
            int value = readInt(prompt);
            if (inputClosed || value >= 0) {
                return value;
            }
            System.out.println("Enter zero or a positive number.");
        }
    }

    private static double readPositiveMoney(String prompt) {
        return readMoney(prompt, true, false);
    }

    private static double readNonNegativeMoney(String prompt) {
        return readMoney(prompt, false, false);
    }

    private static double readMoneyWithDefault(String prompt, double defaultValue) {
        String text = readLine(prompt + " [" + BigDecimal.valueOf(defaultValue).setScale(2, RoundingMode.HALF_UP)
                + "]: ");
        if (text == null || text.trim().isEmpty()) {
            return defaultValue;
        }
        return parseMoney(text, false);
    }

    private static double readMoney(String prompt, boolean positive, boolean allowBlank) {
        while (true) {
            String text = readLine(prompt);
            if (text == null) {
                return 0;
            }
            if (allowBlank && text.trim().isEmpty()) {
                return 0;
            }
            try {
                double amount = parseMoney(text, positive);
                return amount;
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private static double parseMoney(String text, boolean positive) {
        try {
            BigDecimal amount = new BigDecimal(text.trim()).setScale(2, RoundingMode.UNNECESSARY);
            if (amount.signum() < 0 || (positive && amount.signum() == 0)
                    || amount.compareTo(new BigDecimal("99999999.99")) > 0) {
                throw new IllegalArgumentException(positive
                        ? "Enter an amount above zero, up to 99999999.99."
                        : "Enter an amount from zero to 99999999.99.");
            }
            return amount.doubleValue();
        } catch (NumberFormatException | ArithmeticException e) {
            throw new IllegalArgumentException("Enter a valid amount with at most two decimal places.");
        }
    }

    private static Date readDate(String prompt) {
        while (true) {
            String text = requiredText(prompt);
            if (inputClosed) {
                throw new IllegalArgumentException("Input ended before a date was entered.");
            }
            try {
                return Date.valueOf(LocalDate.parse(text.trim()));
            } catch (DateTimeParseException | IllegalArgumentException e) {
                System.out.println("Enter a real calendar date in YYYY-MM-DD format.");
            }
        }
    }

    private static YearMonth readMonth() {
        String current = YearMonth.now().format(DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH));
        while (true) {
            String text = readLine("Payment month (e.g. November 2026) [" + current + "]: ");
            if (text == null || text.trim().isEmpty()) {
                return YearMonth.now();
            }
            try {
                return YearMonth.parse(text.trim(),
                        DateTimeFormatter.ofPattern("MMMM uuuu", Locale.ENGLISH));
            } catch (DateTimeParseException e) {
                System.out.println("Enter the month as 'MMMM yyyy', for example November 2026.");
            }
        }
    }

    private static String readChoice(String label, String[] choices) {
        while (true) {
            System.out.println(label + ": " + String.join(", ", choices));
            String value = requiredText("Choose a value: ");
            if (inputClosed) {
                throw new IllegalArgumentException("Input ended before a selection was made.");
            }
            for (String choice : choices) {
                if (choice.equalsIgnoreCase(value.trim())) {
                    return choice;
                }
            }
            System.out.println("Select one of the listed values.");
        }
    }

    private static String requiredText(String prompt) {
        while (!inputClosed) {
            String value = readLine(prompt);
            if (value == null) {
                return "";
            }
            if (!value.trim().isEmpty()) {
                return value.trim();
            }
            System.out.println("This field is required.");
        }
        return "";
    }

    private static String optionalText(String prompt) {
        String value = readLine(prompt);
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static String readLine(String prompt) {
        if (inputClosed) {
            return null;
        }
        if (CONSOLE != null) {
            String value = CONSOLE.readLine("%s", prompt);
            if (value == null) {
                inputClosed = true;
            }
            return value;
        }
        System.out.print(prompt);
        if (!SCANNER.hasNextLine()) {
            inputClosed = true;
            return null;
        }
        return SCANNER.nextLine();
    }

    private static String readSecret(String prompt) {
        if (CONSOLE == null) {
            return requiredText(prompt);
        }
        char[] value = CONSOLE.readPassword("%s", prompt);
        if (value == null) {
            inputClosed = true;
            return "";
        }
        try {
            return new String(value);
        } finally {
            Arrays.fill(value, '\0');
        }
    }

    private static boolean confirm(String prompt) {
        String answer = readLine(prompt);
        return answer != null && ("y".equalsIgnoreCase(answer.trim()) || "yes".equalsIgnoreCase(answer.trim()));
    }

    private static String nullableTrim(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private static String valueOrDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private static void pause() {
        if (!inputClosed) {
            readLine("Press Enter to continue...");
        }
    }
}
