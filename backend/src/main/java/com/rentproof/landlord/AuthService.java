package com.rentproof.landlord;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * AuthService handles business logic for landlord authentication.
 * 
 * Responsibilities:
 * - Registration validation (email format, 10-digit phone, password length & confirmation)
 * - Cryptographic password hashing (SHA-256)
 * - User login verification against database
 * - Protection against plaintext password storage
 * 
 * Concepts used (Syllabus up to Lecture 42):
 * - Classes and Objects
 * - Encapsulation
 * - Strings and Wrapper classes
 * - Regular Expressions (Pattern matching)
 * - Exception Handling (custom error reporting, try-catch)
 * - Static helper methods
 */
public class AuthService {

    private final LandlordDAO landlordDAO;

    // Email regex pattern conforming to standard email specifications
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    // Indian 10-digit mobile number regex pattern
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[6-9]\\d{9}$"
    );

    // Constructor initializing the DAO dependency
    public AuthService() {
        this.landlordDAO = new LandlordDAO();
    }

    // Overloaded constructor allowing DAO injection for testing
    public AuthService(LandlordDAO landlordDAO) {
        this.landlordDAO = landlordDAO;
    }

    /**
     * Validates landlord registration input fields.
     * 
     * @param fullName landlord's full name
     * @param email landlord's email address
     * @param phone 10-digit contact number
     * @param plainPassword user-chosen password
     * @param confirmPassword re-typed password for verification
     * @return validation error message if invalid, or null if all checks pass
     */
    public String validateRegistration(String fullName, String email, String phone, 
                                       String plainPassword, String confirmPassword) {
        if (fullName == null || fullName.trim().length() < 2) {
            return "Full Name must be at least 2 characters long.";
        }

        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return "Invalid email address format (e.g. landlord@rentproof.com).";
        }

        if (phone == null || !PHONE_PATTERN.matcher(phone.trim()).matches()) {
            return "Phone number must be a valid 10-digit Indian mobile number starting with 6-9.";
        }

        if (plainPassword == null || plainPassword.length() < 6) {
            return "Password must be at least 6 characters long.";
        }

        if (confirmPassword == null || !plainPassword.equals(confirmPassword)) {
            return "Passwords do not match.";
        }

        // Check if email already exists in database
        Landlord existing = landlordDAO.getLandlordByEmail(email);
        if (existing != null) {
            return "An account with email '" + email + "' already exists. Please login instead.";
        }

        return null; // Valid
    }

    /**
     * Validates landlord login input fields.
     * 
     * @param email login email
     * @param plainPassword login password
     * @return error message if invalid, or null if valid
     */
    public String validateLogin(String email, String plainPassword) {
        if (email == null || email.trim().isEmpty()) {
            return "Email is required.";
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return "Invalid email format.";
        }
        if (plainPassword == null || plainPassword.isEmpty()) {
            return "Password is required.";
        }
        return null;
    }

    /**
     * Registers a new Landlord account with encrypted password storage.
     * 
     * @return registered Landlord object with generated ID
     * @throws IllegalArgumentException if input validation fails
     * @throws SQLException if database insertion encounters an error
     */
    public Landlord register(String fullName, String email, String phone, 
                             String plainPassword, String confirmPassword) throws SQLException {
        // Step 1: Validate inputs
        String validationError = validateRegistration(fullName, email, phone, plainPassword, confirmPassword);
        if (validationError != null) {
            throw new IllegalArgumentException(validationError);
        }

        // Step 2: Hash the password securely using SHA-256
        String passwordHash = hashPassword(plainPassword);

        // Step 3: Instantiate Landlord model
        Landlord newLandlord = new Landlord(
                fullName.trim(),
                email.toLowerCase().trim(),
                phone.trim(),
                passwordHash
        );

        // Step 4: Persist through DAO
        int generatedId = landlordDAO.registerLandlord(newLandlord);
        if (generatedId > 0) {
            newLandlord.setLandlordId(generatedId);
            System.out.println("[AuthService] Registered new landlord successfully with ID: " + generatedId);
            return newLandlord;
        } else {
            throw new SQLException("Failed to save landlord record to database.");
        }
    }

    /**
     * Authenticates a landlord by verifying email and hashed password.
     * 
     * @param email landlord's email
     * @param plainPassword entered plaintext password
     * @return authenticated Landlord object if credentials match, or null
     */
    public Landlord login(String email, String plainPassword) {
        String validationError = validateLogin(email, plainPassword);
        if (validationError != null) {
            System.err.println("[AuthService] Login validation failed: " + validationError);
            return null;
        }

        Landlord landlord = landlordDAO.getLandlordByEmail(email);
        if (landlord == null) {
            System.err.println("[AuthService] Login failed: No account registered for " + email);
            return null;
        }

        // Hash the incoming password attempt and compare with stored hash
        String inputHash = hashPassword(plainPassword);
        if (inputHash.equals(landlord.getPasswordHash())) {
            System.out.println("[AuthService] Landlord authenticated successfully: " + landlord.getFullName());
            return landlord;
        } else {
            System.err.println("[AuthService] Login failed: Incorrect password for " + email);
            return null;
        }
    }

    /**
     * Hashes a plaintext password using standard SHA-256 cryptographic digest.
     * Built using Core Java standard library (java.security.MessageDigest) - No third-party jars.
     * 
     * Viva Explanation:
     * 1. MessageDigest.getInstance("SHA-256") obtains the algorithm implementation.
     * 2. password.getBytes(StandardCharsets.UTF_8) converts the password string into a byte array.
     * 3. digest() produces a 256-bit (32-byte) hash.
     * 4. Each byte is converted into a 2-digit hexadecimal representation, creating a fixed 64-character hash string.
     * 
     * @param plainPassword plaintext string
     * @return 64-character hexadecimal SHA-256 hash string
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(plainPassword.getBytes(StandardCharsets.UTF_8));

            // Convert byte array to hexadecimal string
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < hashBytes.length; i++) {
                byte b = hashBytes[i];
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0'); // Ensure 2 characters per byte
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm is not supported by current JVM runtime.", e);
        }
    }
}
