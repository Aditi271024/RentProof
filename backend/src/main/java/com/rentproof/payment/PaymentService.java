package com.rentproof.payment;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PaymentService handles all business logic, validation rules, and operations for Payment & Rent Management.
 * Module: Payment & Rent Management
 * Author: Aanvi
 * 
 * Concepts used (Syllabus up to Lecture 42):
 * - Classes, Objects and Encapsulation
 * - Constructors and Dependency Injection (Passing DAO to Service)
 * - Exception Handling (IllegalArgumentException, SQLException)
 * - Collections Framework (ArrayList, Map, Collections.sort)
 * - Strict Date & Calendar Parsing (java.time.LocalDate)
 * - Duplicate Detection & Defensive Programming
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
     * Checks data integrity, positive amounts, valid calendar dates, and allowed statuses.
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

        if (payment.getAmount() <= 0.0) {
            return "Payment amount must be greater than zero.";
        }

        if (payment.getAmount() > 10000000.0) {
            return "Payment amount exceeds allowable transaction threshold (Rs. 1 Crore).";
        }

        // Validate payment date format and calendar existence
        if (payment.getPaymentDate() == null || payment.getPaymentDate().trim().isEmpty()) {
            return "Payment date is required.";
        }

        try {
            LocalDate parsedDate = LocalDate.parse(payment.getPaymentDate().trim(), DateTimeFormatter.ISO_LOCAL_DATE);
            int year = parsedDate.getYear();
            if (year < 2000 || year > 2050) {
                return "Payment date year must be between 2000 and 2050.";
            }
        } catch (DateTimeParseException e) {
            return "Payment date is invalid. Please provide a valid calendar date in YYYY-MM-DD format.";
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
     * Records a new rent payment after validating all business rules and verifying
     * against unintended duplicate transactions or duplicate paid rent.
     * 
     * @param payment the payment to record
     * @return generated payment_id from MySQL
     * @throws IllegalArgumentException if validation fails or duplicate entry detected
     * @throws SQLException if database write fails
     */
    public int recordPayment(Payment payment) throws IllegalArgumentException, SQLException {
        // 1. Basic Field Validation
        String validationError = validatePayment(payment);
        if (validationError != null) {
            throw new IllegalArgumentException(validationError);
        }

        // 2. Duplicate Transaction Reference Prevention (Step 3 Requirement)
        if (payment.getTransactionRef() != null && !payment.getTransactionRef().trim().isEmpty()) {
            String ref = payment.getTransactionRef().trim();
            // Don't treat auto-generated cash prefixes as duplicate collisions
            if (!ref.toUpperCase().startsWith("CASH-REC-")) {
                if (paymentDAO.isDuplicateTransactionRef(ref, payment.getPaymentId())) {
                    throw new IllegalArgumentException("Duplicate payment detected: Transaction reference '" + ref + "' already exists in the system.");
                }
            }
        }

        // 3. Duplicate Paid Rent Prevention for the same tenant and month (Step 3 Requirement)
        if ("Paid".equalsIgnoreCase(payment.getPaymentStatus())) {
            if (paymentDAO.hasPaidForMonth(payment.getTenantId(), payment.getPaymentMonth(), payment.getPaymentId())) {
                throw new IllegalArgumentException("Duplicate payment error: Rent for " + payment.getPaymentMonth() + " has already been paid for this tenant.");
            }
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
     * Retrieves all payments made by a given tenant.
     */
    public ArrayList<Payment> getPaymentsByTenant(int tenantId) {
        if (tenantId <= 0) {
            return new ArrayList<>();
        }
        return paymentDAO.getPaymentsByTenant(tenantId);
    }

    /**
     * Updates an existing payment after validation and duplicate checking.
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

        // Check duplicate reference (excluding current payment ID)
        if (payment.getTransactionRef() != null && !payment.getTransactionRef().trim().isEmpty()) {
            String ref = payment.getTransactionRef().trim();
            if (!ref.toUpperCase().startsWith("CASH-REC-")) {
                if (paymentDAO.isDuplicateTransactionRef(ref, payment.getPaymentId())) {
                    throw new IllegalArgumentException("Duplicate transaction reference '" + ref + "' belongs to another payment.");
                }
            }
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

        // If marking 'Paid', ensure tenant doesn't already have another 'Paid' entry for that month
        if ("Paid".equalsIgnoreCase(newStatus)) {
            Payment existing = paymentDAO.getPaymentById(paymentId);
            if (existing != null) {
                if (paymentDAO.hasPaidForMonth(existing.getTenantId(), existing.getPaymentMonth(), paymentId)) {
                    throw new IllegalArgumentException("Cannot mark Paid: Another paid record exists for " + existing.getPaymentMonth() + ".");
                }
            }
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
     * Supports multi-landlord isolation when landlordId > 0, or global stats when landlordId <= 0.
     * 
     * Returns:
     * - totalRentCollected (double)
     * - successfulPaymentCount (int)
     * - pendingPaymentCount (int)
     * - overduePaymentCount (int)
     * - overdueAmount (double)
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

    /** Overload for standalone or unauthenticated dashboard stats */
    public Map<String, Object> getDashboardStats() {
        return getDashboardStats(0);
    }

    // =========================================================================
    // RENT MANAGEMENT BUSINESS LOGIC (Step 2 Requirement)
    // =========================================================================

    /**
     * Retrieves tenant rent status and dues overview for a specific month.
     * Calculates total expected rent, total collected, and pending balance.
     */
    public Map<String, Object> getRentOverviewReport(String billingMonth) {
        if (billingMonth == null || billingMonth.trim().isEmpty()) {
            billingMonth = "November 2026";
        }

        ArrayList<Map<String, Object>> tenants = paymentDAO.getTenantRentOverview(billingMonth);

        double totalExpectedRent = 0.0;
        double totalCollectedRent = 0.0;
        double totalPendingRent = 0.0;
        int paidCount = 0;
        int pendingCount = 0;
        int overdueCount = 0;

        for (Map<String, Object> t : tenants) {
            double rent = ((Number) t.getOrDefault("monthlyRent", 0.0)).doubleValue();
            totalExpectedRent += rent;

            String status = (String) t.getOrDefault("paymentStatus", "Pending");
            if ("Paid".equalsIgnoreCase(status)) {
                totalCollectedRent += rent;
                paidCount++;
            } else if ("Overdue".equalsIgnoreCase(status)) {
                totalPendingRent += rent;
                overdueCount++;
            } else {
                totalPendingRent += rent;
                pendingCount++;
            }
        }

        Map<String, Object> report = new HashMap<>();
        report.put("billingMonth", billingMonth);
        report.put("tenants", tenants);
        report.put("totalExpectedRent", totalExpectedRent);
        report.put("totalCollectedRent", totalCollectedRent);
        report.put("totalPendingRent", totalPendingRent);
        report.put("paidCount", paidCount);
        report.put("pendingCount", pendingCount);
        report.put("overdueCount", overdueCount);

        return report;
    }

    /**
     * Updates the persistent monthly rent for a room in MySQL.
     * 
     * @param roomId room ID
     * @param newMonthlyRent new rent amount
     * @return true if updated successfully
     */
    public boolean updateRoomRent(int roomId, double newMonthlyRent) throws SQLException {
        if (roomId <= 0) {
            throw new IllegalArgumentException("Invalid room ID.");
        }
        if (newMonthlyRent <= 0) {
            throw new IllegalArgumentException("Monthly rent must be greater than zero.");
        }
        return paymentDAO.updateRoomRent(roomId, newMonthlyRent);
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
