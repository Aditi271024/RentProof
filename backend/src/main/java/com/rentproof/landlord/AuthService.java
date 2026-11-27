package com.rentproof.landlord;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
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
    public static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    public static final int PBKDF2_ITERATIONS = 120000;
    public static final int HASH_BYTES = 32;
    public static final int SALT_BYTES = 16;

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

        // Step 2: Generate an independent salt and derive a PBKDF2 hash
        String salt = generateSalt();
        String passwordHash = hashPassword(plainPassword, salt, PBKDF2_ITERATIONS);

        // Step 3: Instantiate Landlord model
        Landlord newLandlord = new Landlord(
                fullName.trim(),
                email.toLowerCase().trim(),
                phone.trim(),
                passwordHash
        );
        newLandlord.setPasswordSalt(salt);
        newLandlord.setPasswordIterations(PBKDF2_ITERATIONS);

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

        boolean matches;
        if (landlord.getPasswordSalt() == null || landlord.getPasswordSalt().trim().isEmpty()) {
            matches = MessageDigest.isEqual(
                    hashLegacySha256(plainPassword).getBytes(StandardCharsets.US_ASCII),
                    landlord.getPasswordHash().getBytes(StandardCharsets.US_ASCII));
            if (matches) {
                String salt = generateSalt();
                String hash = hashPassword(plainPassword, salt, PBKDF2_ITERATIONS);
                landlordDAO.updatePasswordHash(landlord.getLandlordId(), hash, salt, PBKDF2_ITERATIONS);
            }
        } else {
            String inputHash = hashPassword(plainPassword, landlord.getPasswordSalt(),
                    landlord.getPasswordIterations() > 0
                            ? landlord.getPasswordIterations()
                            : PBKDF2_ITERATIONS);
            matches = constantTimeHexEquals(inputHash, landlord.getPasswordHash());
        }

        if (matches) {
            System.out.println("[AuthService] Landlord authenticated successfully: " + landlord.getFullName());
            return landlord;
        } else {
            System.err.println("[AuthService] Login failed: Incorrect password for " + email);
            return null;
        }
    }

    public static String generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Backward-compatible helper used by verification tools.
     * New registrations and logins use the salted overload below.
     */
    public static String hashPassword(String plainPassword) {
        String salt = generateSalt();
        return hashPassword(plainPassword, salt, PBKDF2_ITERATIONS);
    }

    public static String hashPassword(String plainPassword, String saltBase64, int iterations) {
        if (plainPassword == null || saltBase64 == null) {
            throw new IllegalArgumentException("Password and salt are required.");
        }
        try {
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            PBEKeySpec spec = new PBEKeySpec(
                    plainPassword.toCharArray(),
                    salt,
                    iterations,
                    HASH_BYTES * 8);
            try {
                byte[] hash = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).getEncoded();
                return Base64.getEncoder().encodeToString(hash);
            } finally {
                spec.clearPassword();
            }
        } catch (InvalidKeySpecException | IllegalArgumentException e) {
            throw new RuntimeException("Password hashing failed.", e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(PBKDF2_ALGORITHM + " is unavailable.", e);
        }
    }

    public static String hashLegacySha256(String plainPassword) {
        if (plainPassword == null) {
            return null;
        }
        try {
            byte[] hashBytes = MessageDigest.getInstance("SHA-256")
                    .digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte value : hashBytes) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 is unavailable.", e);
        }
    }

    public static boolean constantTimeHexEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        byte[] leftBytes = left.getBytes(StandardCharsets.US_ASCII);
        byte[] rightBytes = right.getBytes(StandardCharsets.US_ASCII);
        return leftBytes.length == rightBytes.length
                && MessageDigest.isEqual(leftBytes, rightBytes);
    }
}
