package com.rentproof.tenant;

import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;

/**
 * TenantService encapsulates business logic, domain rules, and data validation
 * for Tenant Management and Property/Room Assignment.
 * 
 * Part of Shweta's Tenant Management & Property Assignment module.
 * 
 * Follows strict BTech CSE Java Syllabus limits (up to Lecture 42):
 * - OOP: Encapsulation, Constructor Overloading, Delegation
 * - Exception Handling: IllegalArgumentException, SQLException propagation
 * - Collections Framework: ArrayList and Collections.sort()
 * - Comparable Interface utilization (natural sorting by full name)
 * - Regular Expressions for phone and email validation
 * - Standard loops (NO lambda expressions, NO Stream API)
 */
public class TenantService {

    private final TenantDAO tenantDAO;

    // Validation Regex Patterns (10-digit Indian phone format & standard email RFC format)
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[6-9][0-9]{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // Allowed Tenant Status options
    private static final String[] VALID_STATUSES = {"Active", "Moved Out", "Pending"};

    /**
     * Default constructor creating a standard TenantDAO instance.
     */
    public TenantService() {
        this.tenantDAO = new TenantDAO();
    }

    /**
     * Overloaded constructor for dependency injection and unit testing.
     * 
     * @param tenantDAO injected DAO instance
     */
    public TenantService(TenantDAO tenantDAO) {
        this.tenantDAO = tenantDAO;
    }

    // ==========================================
    // 1. INPUT VALIDATION LOGIC
    // ==========================================

    /**
     * Validates tenant registration information.
     * 
     * @param tenant Tenant object to validate
     * @return error message if invalid, or null if all checks pass
     */
    public String validateTenant(Tenant tenant) {
        if (tenant == null) {
            return "Tenant details cannot be null.";
        }

        // 1. Full Name Validation
        if (tenant.getFullName() == null || tenant.getFullName().trim().isEmpty()) {
            return "Tenant full name is required.";
        }
        if (tenant.getFullName().trim().length() < 3) {
            return "Tenant name must be at least 3 characters long.";
        }

        // 2. Phone Number Validation (10 digits starting with 6, 7, 8, or 9)
        if (tenant.getPhone() == null || !PHONE_PATTERN.matcher(tenant.getPhone().trim()).matches()) {
            return "Invalid phone number. Must be a valid 10-digit mobile number.";
        }

        // 3. Email Validation
        if (tenant.getEmail() == null || !EMAIL_PATTERN.matcher(tenant.getEmail().trim()).matches()) {
            return "Invalid email address format (e.g. tenant@domain.com).";
        }

        // 4. Emergency Contact Validation (optional, but if provided must be 10 digits)
        if (tenant.getEmergencyContact() != null && !tenant.getEmergencyContact().trim().isEmpty()) {
            if (!PHONE_PATTERN.matcher(tenant.getEmergencyContact().trim()).matches()) {
                return "Emergency contact must be a valid 10-digit mobile number.";
            }
        }

        // 5. Financial Validation (no negative rents or deposits)
        if (!Double.isFinite(tenant.getMonthlyRent()) || tenant.getMonthlyRent() < 0
                || tenant.getMonthlyRent() > 99999999.99) {
            return "Monthly rent must be between 0 and 99,999,999.99.";
        }
        if (!Double.isFinite(tenant.getSecurityDeposit()) || tenant.getSecurityDeposit() < 0
                || tenant.getSecurityDeposit() > 99999999.99) {
            return "Security deposit must be between 0 and 99,999,999.99.";
        }

        // 6. Property and Room Association
        if (tenant.getPropertyId() <= 0) {
            return "A valid property selection is required.";
        }
        if (tenant.getRoomId() <= 0) {
            return "A valid room selection is required.";
        }

        // 7. Joining / Move-in Date Validation
        if (tenant.getMoveInDate() == null) {
            return "Joining date / move-in date is required.";
        }

        return null; // Valid
    }

    /**
     * Validates tenant profile update details.
     * 
     * @param tenant Tenant object with updated attributes
     * @return error message if invalid, or null if valid
     */
    public String validateUpdate(Tenant tenant) {
        if (tenant == null) {
            return "Tenant update payload cannot be null.";
        }
        if (tenant.getTenantId() <= 0) {
            return "Valid Tenant ID is required for update.";
        }
        if (tenant.getFullName() == null || tenant.getFullName().trim().length() < 3) {
            return "Valid full name is required (at least 3 characters).";
        }
        if (tenant.getPhone() == null || !PHONE_PATTERN.matcher(tenant.getPhone().trim()).matches()) {
            return "Valid 10-digit phone number is required.";
        }
        if (tenant.getEmail() == null || !EMAIL_PATTERN.matcher(tenant.getEmail().trim()).matches()) {
            return "Valid email format is required.";
        }
        if (tenant.getEmergencyContact() != null && !tenant.getEmergencyContact().trim().isEmpty()) {
            if (!PHONE_PATTERN.matcher(tenant.getEmergencyContact().trim()).matches()) {
                return "Emergency contact must be a valid 10-digit phone number.";
            }
        }
        if (!isValidStatus(tenant.getTenantStatus())) {
            return "Invalid tenant status. Allowed values: Active, Moved Out, Pending.";
        }
        if (!Double.isFinite(tenant.getMonthlyRent()) || tenant.getMonthlyRent() < 0
                || tenant.getMonthlyRent() > 99999999.99) {
            return "Monthly rent must be between 0 and 99,999,999.99.";
        }
        if (!Double.isFinite(tenant.getSecurityDeposit()) || tenant.getSecurityDeposit() < 0
                || tenant.getSecurityDeposit() > 99999999.99) {
            return "Security deposit must be between 0 and 99,999,999.99.";
        }

        return null; // Valid
    }

    /**
     * Validates property assignment to prevent assigning occupied rooms.
     * 
     * @param tenantId target tenant ID
     * @param propertyId property ID
     * @param roomId room ID
     * @return error message if assignment is invalid, or null if allowed
     */
    public String validateAssignment(int tenantId, int propertyId, int roomId) {
        if (tenantId <= 0) {
            return "Please select a valid tenant.";
        }
        if (propertyId <= 0) {
            return "Please select a valid property.";
        }
        if (roomId <= 0) {
            return "Please select a valid room.";
        }

        Tenant tenant = tenantDAO.getTenantById(tenantId);
        if (tenant == null) {
            return "Tenant with ID " + tenantId + " does not exist.";
        }

        try {
            if (!tenantDAO.roomBelongsToProperty(roomId, propertyId)) {
                return "The selected room does not belong to the selected property.";
            }
        } catch (SQLException e) {
            return "Unable to verify that the selected room belongs to the property.";
        }

        boolean alreadyAssigned = tenant.getPropertyId() == propertyId && tenant.getRoomId() == roomId
                && ("Active".equalsIgnoreCase(tenant.getTenantStatus())
                    || "Pending".equalsIgnoreCase(tenant.getTenantStatus()));
        if (!alreadyAssigned && !tenantDAO.isRoomVacant(roomId)) {
            return "The selected room is currently occupied or under maintenance. Please select a vacant room.";
        }

        return null; // Valid
    }

    // ==========================================
    // 2. CORE BUSINESS OPERATIONS
    // ==========================================

    /**
     * Registers a new tenant after performing complete business validation
     * and verifying room vacancy.
     * 
     * @param tenant Tenant object to register
     * @return generated tenant_id
     * @throws IllegalArgumentException if validation fails
     * @throws SQLException if database operation fails
     */
    public int registerTenant(Tenant tenant) throws SQLException {
        String validationError = validateTenant(tenant);
        if (validationError != null) {
            throw new IllegalArgumentException(validationError);
        }

        if (!tenantDAO.roomBelongsToProperty(tenant.getRoomId(), tenant.getPropertyId())) {
            throw new IllegalArgumentException("The selected room does not belong to the selected property.");
        }

        // Ensure target room is not occupied
        if (!tenantDAO.isRoomVacant(tenant.getRoomId())) {
            throw new IllegalArgumentException("Cannot assign room: Room is already occupied or undergoing maintenance.");
        }

        return tenantDAO.addTenant(tenant);
    }

    /**
     * Updates an existing tenant profile.
     * 
     * @param tenant Tenant object to update
     * @return true if update succeeded
     * @throws IllegalArgumentException if validation fails
     */
    public boolean updateTenant(Tenant tenant) {
        String validationError = validateUpdate(tenant);
        if (validationError != null) {
            throw new IllegalArgumentException(validationError);
        }

        return tenantDAO.updateTenant(tenant);
    }

    /**
     * Deletes a tenant record and resets their room to Vacant.
     * 
     * @param tenantId ID of the tenant to remove
     * @return true if deleted
     */
    public boolean deleteTenant(int tenantId) {
        if (tenantId <= 0) {
            throw new IllegalArgumentException("Tenant ID must be greater than zero.");
        }
        return tenantDAO.deleteTenant(tenantId);
    }

    /**
     * Assigns a tenant to a property and room with complete vacancy safety.
     * 
     * @param tenantId tenant ID
     * @param propertyId property ID
     * @param roomId room ID
     * @param monthlyRent monthly rent agreed
     * @param moveInDate move-in date
     * @return true if assignment succeeded
     */
    public boolean assignProperty(int tenantId, int propertyId, int roomId, double monthlyRent, Date moveInDate) {
        String assignmentError = validateAssignment(tenantId, propertyId, roomId);
        if (assignmentError != null) {
            throw new IllegalArgumentException(assignmentError);
        }
        if (!Double.isFinite(monthlyRent) || monthlyRent < 0 || monthlyRent > 99999999.99) {
            throw new IllegalArgumentException("Monthly rent must be between 0 and 99,999,999.99.");
        }

        return tenantDAO.assignProperty(tenantId, propertyId, roomId, monthlyRent, moveInDate);
    }

    // ==========================================
    // 3. RETRIEVAL & FILTERING (Using Core Java Loops)
    // ==========================================

    /**
     * Retrieves all tenants across the system.
     * 
     * @return ArrayList of Tenant records
     */
    public ArrayList<Tenant> getAllTenants() {
        return tenantDAO.getAllTenants();
    }

    /**
     * Retrieves a single tenant by ID.
     */
    public Tenant getTenantById(int tenantId) {
        if (tenantId <= 0) {
            return null;
        }
        return tenantDAO.getTenantById(tenantId);
    }

    /**
     * Retrieves tenants for a specific property.
     */
    public ArrayList<Tenant> getTenantsByProperty(int propertyId) {
        if (propertyId <= 0) {
            return new ArrayList<>();
        }
        return tenantDAO.getTenantsByProperty(propertyId);
    }

    /**
     * Filters tenants by status ('Active', 'Moved Out', 'Pending') using standard Java for-loop.
     * Demonstrates Collections traversal without Stream API (Syllabus Lecture 16-22 limit).
     * 
     * @param status target status
     * @return filtered list of tenants
     */
    public ArrayList<Tenant> getTenantsByStatus(String status) {
        ArrayList<Tenant> all = tenantDAO.getAllTenants();
        if (status == null || status.trim().isEmpty() || "All".equalsIgnoreCase(status)) {
            return all;
        }

        ArrayList<Tenant> filtered = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            Tenant t = all.get(i);
            if (status.equalsIgnoreCase(t.getTenantStatus())) {
                filtered.add(t);
            }
        }
        return filtered;
    }

    /**
     * Searches tenants by Name or Phone number (case-insensitive substring match).
     * Implements linear search with Collections traversal (Syllabus Lecture 16-22 limit).
     * 
     * @param query search query
     * @return matching list of tenants
     */
    public ArrayList<Tenant> searchTenants(String query) {
        ArrayList<Tenant> all = tenantDAO.getAllTenants();
        if (query == null || query.trim().isEmpty()) {
            return all;
        }

        String lowerQuery = query.trim().toLowerCase();
        ArrayList<Tenant> results = new ArrayList<>();

        for (int i = 0; i < all.size(); i++) {
            Tenant t = all.get(i);
            boolean nameMatch = t.getFullName() != null && t.getFullName().toLowerCase().contains(lowerQuery);
            boolean phoneMatch = t.getPhone() != null && t.getPhone().contains(lowerQuery);
            boolean emailMatch = t.getEmail() != null && t.getEmail().toLowerCase().contains(lowerQuery);

            if (nameMatch || phoneMatch || emailMatch) {
                results.add(t);
            }
        }
        return results;
    }

    /**
     * Sorts tenants alphabetically by name using the Comparable interface
     * and Collections.sort() (Syllabus Lecture 23-25).
     * 
     * @return sorted list of tenants
     */
    public ArrayList<Tenant> getTenantsSortedByName() {
        ArrayList<Tenant> all = tenantDAO.getAllTenants();
        Collections.sort(all);
        return all;
    }

    /**
     * Counts active tenants in the system.
     * 
     * @return count of Active tenants
     */
    public int getActiveTenantsCount() {
        ArrayList<Tenant> all = tenantDAO.getAllTenants();
        int count = 0;
        for (int i = 0; i < all.size(); i++) {
            if ("Active".equalsIgnoreCase(all.get(i).getTenantStatus())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Handles tenant move-out workflow: updates status to 'Moved Out',
     * sets move-out date, and frees the assigned room back to 'Vacant'.
     * 
     * @param tenantId tenant moving out
     * @param moveOutDate date of moving out
     * @return true if updated successfully
     */
    public boolean handleTenantMoveOut(int tenantId, Date moveOutDate) {
        Tenant tenant = tenantDAO.getTenantById(tenantId);
        if (tenant == null) {
            return false;
        }
        tenant.setTenantStatus("Moved Out");
        tenant.setMoveOutDate(moveOutDate != null ? moveOutDate : new Date(System.currentTimeMillis()));
        return tenantDAO.updateTenant(tenant);
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private boolean isValidStatus(String status) {
        if (status == null) {
            return false;
        }
        for (int i = 0; i < VALID_STATUSES.length; i++) {
            if (VALID_STATUSES[i].equalsIgnoreCase(status.trim())) {
                return true;
            }
        }
        return false;
    }
}
