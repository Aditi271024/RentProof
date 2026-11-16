package com.rentproof.property;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;

/**
 * PropertyService encapsulates business logic and rules for Property Management.
 * 
 * Responsibilities:
 * - Field validation (non-empty names, valid property types, positive room counts, positive rents)
 * - Pincode format validation (6-digit Indian PIN code)
 * - Ownership verification (ensuring a landlord can only edit or delete their own properties)
 * - Safe property addition, updating, and deletion
 * - Sorting properties using Comparable
 * 
 * Concepts used (Syllabus up to Lecture 42):
 * - Classes and Objects
 * - Encapsulation
 * - Exception Handling (IllegalArgumentException, SQLException)
 * - Basic Collections (ArrayList)
 * - Sorting with Collections.sort() and Comparable
 * - Regular Expressions
 */
public class PropertyService {

    private final PropertyDAO propertyDAO;

    // Indian 6-digit postal PIN code format
    private static final Pattern PINCODE_PATTERN = Pattern.compile("^[1-9][0-9]{5}$");

    // Standard valid property types
    private static final String[] VALID_TYPES = {"PG", "Flat", "Apartment", "House"};

    public PropertyService() {
        this.propertyDAO = new PropertyDAO();
    }

    public PropertyService(PropertyDAO propertyDAO) {
        this.propertyDAO = propertyDAO;
    }

    /**
     * Validates property input data according to business rules.
     * 
     * @param property Property object to validate
     * @return error message if invalid, or null if valid
     */
    public String validateProperty(Property property) {
        if (property == null) {
            return "Property details cannot be null.";
        }

        if (property.getPropertyName() == null || property.getPropertyName().trim().isEmpty()) {
            return "Property name is required.";
        }
        if (property.getPropertyName().trim().length() < 3) {
            return "Property name must be at least 3 characters.";
        }

        if (!isValidPropertyType(property.getPropertyType())) {
            return "Invalid property type. Must be one of: PG, Flat, Apartment, House.";
        }

        if (property.getAddress() == null || property.getAddress().trim().isEmpty()) {
            return "Address cannot be empty.";
        }

        if (property.getCity() == null || property.getCity().trim().isEmpty()) {
            return "City cannot be empty.";
        }

        if (property.getState() == null || property.getState().trim().isEmpty()) {
            return "State cannot be empty.";
        }

        if (property.getPincode() == null || !PINCODE_PATTERN.matcher(property.getPincode().trim()).matches()) {
            return "Invalid PIN code. Must be a valid 6-digit Indian postal code.";
        }

        if (property.getTotalRooms() < 1) {
            return "Total rooms must be at least 1.";
        }

        if (property.getMonthlyRent() < 0) {
            return "Monthly rent cannot be negative.";
        }

        return null; // All validation checks passed
    }

    /**
     * Helper to verify if property type is one of the allowed categories.
     */
    private boolean isValidPropertyType(String type) {
        if (type == null) {
            return false;
        }
        for (int i = 0; i < VALID_TYPES.length; i++) {
            if (VALID_TYPES[i].equalsIgnoreCase(type.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifies that the property exists and belongs to the specified landlord.
     * Security check: Prevents Landlord A from modifying or deleting Landlord B's properties.
     * 
     * @param propertyId property ID to check
     * @param landlordId requesting landlord ID
     * @return true if owned by this landlord, false otherwise
     */
    public boolean checkPropertyOwnership(int propertyId, int landlordId) {
        Property property = propertyDAO.getPropertyById(propertyId);
        if (property == null) {
            return false;
        }
        return property.getLandlordId() == landlordId;
    }

    /**
     * Adds a new property after performing business validation.
     * 
     * @param property property to insert
     * @param landlordId ID of the logged-in landlord
     * @return generated property_id
     * @throws IllegalArgumentException on validation failure
     * @throws SQLException on database failure
     */
    public int addProperty(Property property, int landlordId) throws SQLException {
        if (property == null) {
            throw new IllegalArgumentException("Property object cannot be null.");
        }

        property.setLandlordId(landlordId);

        String validationError = validateProperty(property);
        if (validationError != null) {
            throw new IllegalArgumentException(validationError);
        }

        int propertyId = propertyDAO.addProperty(property);
        if (propertyId > 0) {
            System.out.println("[PropertyService] Successfully added property '" + property.getPropertyName() + "' (ID: " + propertyId + ")");
            return propertyId;
        } else {
            throw new SQLException("Failed to add property to database.");
        }
    }

    /**
     * Updates an existing property with ownership and field validation.
     * 
     * @param property updated property object
     * @param landlordId requesting landlord
     * @return true if updated, false otherwise
     * @throws IllegalArgumentException on invalid input or unauthorized access
     */
    public boolean updateProperty(Property property, int landlordId) {
        if (property == null) {
            throw new IllegalArgumentException("Property cannot be null.");
        }

        // Verify ownership
        if (!checkPropertyOwnership(property.getPropertyId(), landlordId)) {
            throw new IllegalArgumentException("Unauthorized: You do not own property ID " + property.getPropertyId());
        }

        property.setLandlordId(landlordId);

        String validationError = validateProperty(property);
        if (validationError != null) {
            throw new IllegalArgumentException(validationError);
        }

        return propertyDAO.updateProperty(property);
    }

    /**
     * Deletes a property with ownership verification and dependency checks.
     * 
     * @param propertyId property ID to delete
     * @param landlordId requesting landlord ID
     * @return true if deleted, false otherwise
     */
    public boolean deleteProperty(int propertyId, int landlordId) {
        if (!checkPropertyOwnership(propertyId, landlordId)) {
            System.err.println("[PropertyService] Unauthorized delete attempt for property ID " + propertyId);
            return false;
        }

        return propertyDAO.deleteProperty(propertyId);
    }

    /**
     * Fetches all properties owned by the given landlord, sorted alphabetically by name.
     * Demonstrates syllabus Collections.sort() using Comparable.
     * 
     * @param landlordId owner ID
     * @return sorted list of properties
     */
    public ArrayList<Property> getPropertiesByLandlord(int landlordId) {
        ArrayList<Property> list = propertyDAO.getPropertiesByLandlordId(landlordId);
        // Uses Property.compareTo() natural order
        Collections.sort(list);
        return list;
    }

    /**
     * Fetches a single property by ID with ownership verification.
     * 
     * @param propertyId target property ID
     * @param landlordId requesting landlord ID
     * @return Property if found and authorized, or null
     */
    public Property getProperty(int propertyId, int landlordId) {
        Property property = propertyDAO.getPropertyById(propertyId);
        if (property != null && property.getLandlordId() == landlordId) {
            return property;
        }
        return null;
    }
}
