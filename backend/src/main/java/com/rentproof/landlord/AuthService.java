package com.rentproof.landlord;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * AuthService handles business logic for landlord authentication.
 * 
 * Responsibilities:
 * - Registration validation (email format, 10-digit phone, password length & confirmation)
 * - Salted PBKDF2 password hashing (HMAC-SHA-256)
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
    private static final String DUPLICATE_ACCOUNT_MESSAGE =
            "An account with this email already exists. Please login instead.";

    public static final class DuplicateAccountException extends IllegalArgumentException {
        public DuplicateAccountException() {
            super(DUPLICATE_ACCOUNT_MESSAGE);
        }
    }

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
            return DUPLICATE_ACCOUNT_MESSAGE;
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
     * Registers a new landlord account with salted password-hash storage.
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
            if (DUPLICATE_ACCOUNT_MESSAGE.equals(validationError)) {
                throw new DuplicateAccountException();
            }
            throw new IllegalArgumentException(validationError);
        }

        // Step 2: Generate an independent salt and derive a PBKDF2 hash
        String salt = generateSalt();
        String passwordHash = hashPassword(plainPassword, salt, PBKDF2_ITERATIONS);

        // Step 3: Instantiate Landlord model
        Landlord newLandlord = new Landlord(
                fullName.trim(),
                email.trim().toLowerCase(Locale.ROOT),
                phone.trim(),
                passwordHash
        );
        newLandlord.setPasswordSalt(salt);
        newLandlord.setPasswordIterations(PBKDF2_ITERATIONS);

        // Step 4: Persist through DAO
        int generatedId = landlordDAO.registerLandlord(newLandlord);
        if (generatedId > 0) {
            newLandlord.setLandlordId(generatedId);
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
            return null;
        }

        Landlord landlord = landlordDAO.getLandlordByEmail(email);
        if (landlord == null) {
            return null;
        }

        boolean matches = verifyPassword(plainPassword, landlord.getPasswordHash(),
                landlord.getPasswordSalt(), landlord.getPasswordIterations());

        if (matches) {
            // Transparently upgrade legacy SHA-256 hash to salted PBKDF2 upon successful login
            if (landlord.getPasswordSalt() == null || landlord.getPasswordSalt().trim().isEmpty()) {
                String salt = generateSalt();
                String hash = hashPassword(plainPassword, salt, PBKDF2_ITERATIONS);
                landlordDAO.updatePasswordHash(landlord.getLandlordId(), hash, salt, PBKDF2_ITERATIONS);
            }
            return landlord;
        } else {
            return null;
        }
    }

    /**
     * Verifies a plaintext password against a stored hash and salt.
     * Supports both modern salted PBKDF2 hashes and legacy SHA-256 hashes.
     * 
     * @param plainPassword plaintext candidate password
     * @param storedHash stored password hash
     * @param saltBase64 stored Base64 salt (null or empty for legacy SHA-256)
     * @param iterations PBKDF2 iteration count (or 0 for default)
     * @return true if credentials match, false otherwise
     */
    public static boolean verifyPassword(String plainPassword, String storedHash, String saltBase64, int iterations) {
        if (plainPassword == null || storedHash == null) {
            return false;
        }
        if (saltBase64 == null || saltBase64.trim().isEmpty()) {
            String legacy = hashLegacySha256(plainPassword);
            return constantTimeHexEquals(legacy, storedHash);
        }
        int iter = iterations > 0 ? iterations : PBKDF2_ITERATIONS;
        try {
            String computedHash = hashPassword(plainPassword, saltBase64, iter);
            return constantTimeHexEquals(computedHash, storedHash);
        } catch (Exception e) {
            return false;
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
