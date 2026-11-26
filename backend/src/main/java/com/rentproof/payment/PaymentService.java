package com.rentproof.payment;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * PaymentService handles all business logic, validation rules, and operations for Payment Management.
 * Module: Payment Management
 * Author: Aanvi
 * 
 * Concepts used (Syllabus up to Lecture 42):
 * - Classes, Objects and Encapsulation
 * - Constructors and Dependency Injection (Passing DAO to Service)
 * - Exception Handling (IllegalArgumentException, SQLException)
 * - Collections Framework (ArrayList, Map, Collections.sort)
 * - Regular Expressions and Pattern Matching
 * - Input Validation & Defensive Programming
 */
public class PaymentService {

    private final PaymentDAO paymentDAO;

    // Allowed Payment Modes in RentProof
    public static final List<String> ALLOWED_MODES = Arrays.asList(
            "Cash", "UPI", "Bank Transfer", "Cheque"
    );

    // Allowed Payment Statuses in RentProof
    public static final List<String> ALLOWED_STATUSES = Arrays.asList(
            "Paid", "Pending", "Overdue", "Failed"
    );

    // Date Pattern (YYYY-MM-DD)
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

    /**
     * Default Constructor initializing internal PaymentDAO
     */
    public PaymentService() {
        this.paymentDAO = new PaymentDAO();
    }

    /**
     * Overloaded Constructor allowing dependency injection (Useful for unit testing)
     */
    public PaymentService(PaymentDAO paymentDAO) {
        this.paymentDAO = paymentDAO;
    }

    /**
     * Validates payment input fields according to business requirements.
     * 
     * @param payment Payment object to validate
     * @return null if payment is valid, or a descriptive error message String if invalid
     */
    public String validatePayment(Payment payment) {
        if (payment == null) {
            return "Payment details cannot be null.";
        }

        if (payment.getTenantId() <= 0) {
            return "Please select a valid tenant.";
        }

        if (payment.getPropertyId() <= 0) {
            return "Please select a valid property.";
        }

        if (payment.getRoomId() <= 0) {
            return "Please select a valid room.";
        }

        if (!Double.isFinite(payment.getAmount()) || payment.getAmount() <= 0.0) {
            return "Payment amount must be greater than zero.";
        }

        if (payment.getAmount() > 10000000.0) {
            return "Payment amount exceeds allowable transaction threshold (Rs. 1 Crore).";
        }

        if (payment.getPaymentDate() == null || payment.getPaymentDate().trim().isEmpty()) {
            return "Payment date is required.";
        }

        if (!DATE_PATTERN.matcher(payment.getPaymentDate().trim()).matches()) {
            return "Payment date must follow the format YYYY-MM-DD (e.g. 2026-11-05).";
        }

        if (payment.getPaymentMonth() == null || payment.getPaymentMonth().trim().isEmpty()) {
            return "Payment month is required (e.g. 'November 2026').";
        }

        if (!isAllowedMode(payment.getPaymentMode())) {
            return "Invalid payment mode. Must be one of: " + String.join(", ", ALLOWED_MODES);
        }

        if (payment.getPaymentStatus() == null || !isAllowedStatus(payment.getPaymentStatus())) {
            return "Invalid payment status. Must be one of: " + String.join(", ", ALLOWED_STATUSES);
        }

        // Business rule: Electronic payments (UPI, Bank Transfer) marked 'Paid' should include transaction reference
        boolean isElectronic = "UPI".equalsIgnoreCase(payment.getPaymentMode()) ||
                               "Bank Transfer".equalsIgnoreCase(payment.getPaymentMode());
        if (isElectronic && "Paid".equalsIgnoreCase(payment.getPaymentStatus())) {
            if (payment.getTransactionRef() == null || payment.getTransactionRef().trim().isEmpty()) {
                return "Transaction reference is required for completed " + payment.getPaymentMode() + " payments.";
            }
        }

        return null; // All validation checks passed!
    }

    /**
     * Records a new rent payment after validating all business rules.
     * 
     * @param payment the payment to record
     * @return generated payment_id from MySQL
     * @throws IllegalArgumentException if validation fails
     * @throws SQLException if database write fails
     */
    public int recordPayment(Payment payment) throws IllegalArgumentException, SQLException {
        String validationError = validatePayment(payment);
        if (validationError != null) {
            throw new IllegalArgumentException(validationError);
        }
        return paymentDAO.addPayment(payment);
    }

    /**
     * Retrieves all payments ordered chronologically.
     */
    public ArrayList<Payment> getAllPayments() {
        ArrayList<Payment> list = paymentDAO.getAllPayments();
        Collections.sort(list); // Uses Payment.compareTo()
        return list;
    }

    /**
     * Retrieves a single payment record by ID.
     */
    public Payment getPaymentById(int paymentId) {
        if (paymentId <= 0) {
            return null;
        }
        return paymentDAO.getPaymentById(paymentId);
    }

    /**
     * Updates an existing payment after validation.
     * 
     * @param payment Payment object with modified fields
     * @return true if updated, false otherwise
     * @throws IllegalArgumentException if invalid
     * @throws SQLException if database error occurs
     */
    public boolean updatePayment(Payment payment) throws IllegalArgumentException, SQLException {
        if (payment == null || payment.getPaymentId() <= 0) {
            throw new IllegalArgumentException("Valid Payment ID is required for update.");
        }
        String validationError = validatePayment(payment);
        if (validationError != null) {
            throw new IllegalArgumentException(validationError);
        }
        return paymentDAO.updatePayment(payment);
    }

    /**
     * Updates only the status of an existing payment (e.g. from 'Pending' to 'Paid').
     * 
     * @param paymentId payment ID
     * @param newStatus new status ('Paid', 'Pending', 'Overdue', 'Failed')
     * @return true if updated
     * @throws IllegalArgumentException if invalid status
     * @throws SQLException if database error occurs
     */
    public boolean updatePaymentStatus(int paymentId, String newStatus) throws IllegalArgumentException, SQLException {
        if (paymentId <= 0) {
            throw new IllegalArgumentException("Invalid payment ID.");
        }
        if (!isAllowedStatus(newStatus)) {
            throw new IllegalArgumentException("Invalid status: " + newStatus + ". Allowed: " + ALLOWED_STATUSES);
        }
        Payment payment = paymentDAO.getPaymentById(paymentId);
        if (payment == null) {
            return false;
        }
        payment.setPaymentStatus(newStatus);
        String validationError = validatePayment(payment);
        if (validationError != null) {
            throw new IllegalArgumentException(validationError);
        }
        return paymentDAO.updatePaymentStatus(paymentId, newStatus);
    }

    /**
     * Deletes a payment record.
     * 
     * @param paymentId ID of the payment to remove
     * @return true if deleted
     * @throws IllegalArgumentException if invalid ID
     * @throws SQLException if database error occurs
     */
    public boolean deletePayment(int paymentId) throws IllegalArgumentException, SQLException {
        if (paymentId <= 0) {
            throw new IllegalArgumentException("Invalid payment ID.");
        }
        return paymentDAO.deletePayment(paymentId);
    }

    /**
     * Searches and filters payments according to user dashboard selections.
     */
    public ArrayList<Payment> searchAndFilterPayments(String keyword, Integer tenantId, String month, String status) {
        return paymentDAO.searchAndFilterPayments(keyword, tenantId, month, status);
    }

    /**
     * Retrieves recent payments for the dashboard preview.
     */
    public ArrayList<Payment> getRecentPayments(int limit) {
        if (limit <= 0) {
            limit = 5;
        }
        return paymentDAO.getRecentPayments(limit);
    }

    /**
     * Computes high-level financial and operational summary metrics for the Dashboard cards.
     * Returns:
     * - totalRentCollected (double)
     * - successfulPaymentCount (int)
     * - pendingPaymentCount (int)
     * - overduePaymentCount (int)
     */
    public Map<String, Object> getDashboardStats(int landlordId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRentCollected", paymentDAO.getTotalRentCollected(landlordId));
        stats.put("successfulPaymentCount", paymentDAO.getSuccessfulPaymentCount(landlordId));
        stats.put("pendingPaymentCount", paymentDAO.getPendingPaymentCount(landlordId));
        stats.put("overduePaymentCount", paymentDAO.getOverduePaymentCount(landlordId));
        stats.put("overdueAmount", paymentDAO.getOverdueAmount(landlordId));
        return stats;
    }

    /**
     * Retrieves active tenants from MySQL for the Add Payment dropdown.
     */
    public ArrayList<Map<String, Object>> getAvailableTenants() {
        return paymentDAO.getAvailableTenants();
    }

    /**
     * Retrieves properties from MySQL for the Add Payment dropdown.
     */
    public ArrayList<Map<String, Object>> getAvailableProperties() {
        return paymentDAO.getAvailableProperties();
    }

    /**
     * Retrieves rooms belonging to a property from MySQL for the room dropdown.
     */
    public ArrayList<Map<String, Object>> getRoomsByProperty(int propertyId) {
        if (propertyId <= 0) {
            return new ArrayList<>();
        }
        return paymentDAO.getRoomsByProperty(propertyId);
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private boolean isAllowedMode(String mode) {
        if (mode == null) return false;
        for (String allowed : ALLOWED_MODES) {
            if (allowed.equalsIgnoreCase(mode.trim())) {
                return true;
            }
        }
        return false;
    }

    private boolean isAllowedStatus(String status) {
        if (status == null) return false;
        for (String allowed : ALLOWED_STATUSES) {
            if (allowed.equalsIgnoreCase(status.trim())) {
                return true;
            }
        }
        return false;
    }
}

