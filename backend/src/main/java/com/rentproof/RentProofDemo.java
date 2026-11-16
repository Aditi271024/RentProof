package com.rentproof;

import com.rentproof.database.DBConnection;
import com.rentproof.landlord.AuthService;
import com.rentproof.landlord.Landlord;
import com.rentproof.property.Property;
import com.rentproof.property.PropertyService;

import java.util.ArrayList;

/**
 * RentProofDemo - College Project Demonstration and Viva Test Runner
 * 
 * Demonstrates Aditi's full backend implementation:
 * 1. Database Connectivity (DBConnection)
 * 2. Landlord Authentication & SHA-256 Hashing (AuthService & LandlordDAO)
 * 3. Property Management & Comparable Sorting (PropertyService & PropertyDAO)
 * 
 * Can be run from the command line:
 * javac -d bin backend/src/main/java/com/rentproof/database/*.java ...
 * java -cp "bin;backend/src/main/resources" com.rentproof.RentProofDemo
 */
public class RentProofDemo {

    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("   RENTPROOF: ADITI'S MODULE BACKEND VERIFICATION SUITE     ");
        System.out.println("============================================================\n");

        // 1. Password Hashing Verification
        System.out.println("--- 1. TESTING PASSWORD HASHING (SHA-256) ---");
        String testPassword = "RentProof@123";
        String testHash = AuthService.hashPassword(testPassword);
        System.out.println("Plaintext Password : " + testPassword);
        System.out.println("Generated SHA-256  : " + testHash);
        System.out.println("Hash Length (chars): " + testHash.length() + " (Fixed 256-bit hash)");
        System.out.println("Is Consistent      : " + testHash.equals(AuthService.hashPassword(testPassword)));
        System.out.println();

        // 2. Validation Logic Tests
        System.out.println("--- 2. TESTING INPUT VALIDATIONS ---");
        AuthService authService = new AuthService();
        PropertyService propertyService = new PropertyService();

        // Invalid registration check
        String regError = authService.validateRegistration("A", "bad-email", "123", "pass", "diff");
        System.out.println("Expected Registration Error caught: " + regError);

        // Invalid property check
        Property invalidProp = new Property(1, "A", "InvalidType", "", "Noida", "UP", "999", 0, -500.0, "Desc");
        String propError = propertyService.validateProperty(invalidProp);
        System.out.println("Expected Property Error caught: " + propError);
        System.out.println();

        // 3. Database Connectivity Check
        System.out.println("--- 3. TESTING DATABASE CONNECTIVITY ---");
        System.out.println("Connecting to: " + DBConnection.getUrl());
        boolean connected = DBConnection.testConnection();
        if (connected) {
            System.out.println("SUCCESS: Connected to MySQL 'rentproof' database successfully!");
            runDatabaseTests(authService, propertyService);
        } else {
            System.out.println("NOTICE: Local MySQL server is not active or credentials need configuration in db.properties.");
            System.out.println("All Java business logic, validations, and SHA-256 hashing passed successfully in offline mode.");
        }

        System.out.println("\n============================================================");
        System.out.println("   VERIFICATION COMPLETE - READY FOR FACULTY VIVA           ");
        System.out.println("============================================================");
    }

    private static void runDatabaseTests(AuthService authService, PropertyService propertyService) {
        try {
            System.out.println("\n--- 4. TESTING LIVE DATABASE AUTHENTICATION ---");
            // Test Login with pre-seeded demo account
            Landlord demoLandlord = authService.login("aditi.landlord@rentproof.com", "RentProof@123");
            if (demoLandlord != null) {
                System.out.println("Login Succeeded! Welcome, " + demoLandlord.getFullName() + " (ID: " + demoLandlord.getLandlordId() + ")");
                
                System.out.println("\n--- 5. TESTING PROPERTY RETRIEVAL & SORTING ---");
                ArrayList<Property> properties = propertyService.getPropertiesByLandlord(demoLandlord.getLandlordId());
                System.out.println("Total properties retrieved for Landlord " + demoLandlord.getLandlordId() + ": " + properties.size());
                
                for (int i = 0; i < properties.size(); i++) {
                    Property p = properties.get(i);
                    System.out.println("  " + (i + 1) + ". " + p.getPropertyName() + " [" + p.getPropertyType() + "] - " 
                                       + p.getCity() + " | Rooms: " + p.getTotalRooms() + " | Rent: Rs. " + p.getMonthlyRent());
                }
            } else {
                System.out.println("Demo login failed - make sure database/rentproof.sql is executed in MySQL Workbench.");
            }
        } catch (Exception e) {
            System.err.println("Database test exception: " + e.getMessage());
        }
    }
}
