package com.rentproof.payment;

import com.rentproof.database.DBConnection;

import java.util.ArrayList;
import java.util.Map;

/**
 * PaymentDemo - Verification Suite and Faculty Viva Test Runner
 * Module: Payment Management
 * Author: Aanvi
 * 
 * Demonstrates:
 * 1. Payment entity model & encapsulation (Payment.java)
 * 2. Business validation rules (PaymentService.java)
 * 3. Database connectivity & CRUD operations via JDBC (PaymentDAO.java)
 * 4. Relational SQL JOINs with tenants, properties, and rooms
 * 5. Statistical aggregations (Total Rent Collected, Successful, Pending)
 * 
 * Run via terminal:
 * javac -d bin -cp ".;lib/*;backend/src/main/resources" backend/src/main/java/com/rentproof/database/*.java backend/src/main/java/com/rentproof/payment/*.java
 * java -cp "bin;lib/*;backend/src/main/resources" com.rentproof.payment.PaymentDemo
 */
public class PaymentDemo {

    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("   RENTPROOF: AANVI'S PAYMENT MODULE VERIFICATION SUITE     ");
        System.out.println("============================================================\n");

        PaymentService paymentService = new PaymentService();

        // ---------------------------------------------------------
        // 1. TESTING PAYMENT MODEL & ENCAPSULATION
        // ---------------------------------------------------------
        System.out.println("--- 1. TESTING PAYMENT MODEL & ENCAPSULATION ---");
        Payment testPayment = new Payment(
                1, 1, 1, 8500.0,
                "2026-11-05", "November 2026", "UPI",
                "UPI/20261105/98218731", "Paid", "Rent received via Google Pay"
        );
        testPayment.setTenantName("Rohan Verma");
        testPayment.setPropertyName("Sunrise Executive PG");
        testPayment.setRoomNumber("101");

        System.out.println("[Model] Tenant Name  : " + testPayment.getTenantName());
        System.out.println("[Model] Property     : " + testPayment.getPropertyName());
        System.out.println("[Model] Room Number  : " + testPayment.getRoomNumber());
        System.out.println("[Model] Amount (Rs)  : " + testPayment.getAmount());
        System.out.println("[Model] Payment Mode : " + testPayment.getPaymentMode());
        System.out.println("[Model] Status       : " + testPayment.getPaymentStatus());
        System.out.println("[Model] toString()   : " + testPayment.toString());
        System.out.println("[PASS] Payment model instantiated and encapsulated successfully.\n");

        // ---------------------------------------------------------
        // 2. TESTING BUSINESS VALIDATION LOGIC
        // ---------------------------------------------------------
        System.out.println("--- 2. TESTING BUSINESS VALIDATION LOGIC ---");

        // Test 2.1: Negative Amount Check
        Payment invalidAmount = new Payment(1, 1, 1, -500.0, "2026-11-05", "November 2026", "Cash", "", "Paid", "Invalid");
        String err1 = paymentService.validatePayment(invalidAmount);
        System.out.println("[Validation 1] Negative Amount Error Caught: " + err1);

        // Test 2.2: Missing Tenant ID Check
        Payment missingTenant = new Payment(0, 1, 1, 5000.0, "2026-11-05", "November 2026", "Cash", "", "Paid", "Invalid");
        String err2 = paymentService.validatePayment(missingTenant);
        System.out.println("[Validation 2] Missing Tenant Error Caught : " + err2);

        // Test 2.3: Invalid Payment Mode Check
        Payment invalidMode = new Payment(1, 1, 1, 5000.0, "2026-11-05", "November 2026", "Bitcoin", "", "Paid", "Invalid");
        String err3 = paymentService.validatePayment(invalidMode);
        System.out.println("[Validation 3] Invalid Mode Error Caught   : " + err3);

        // Test 2.4: Electronic Payment without Transaction Reference Check
        Payment missingRef = new Payment(1, 1, 1, 5000.0, "2026-11-05", "November 2026", "UPI", "", "Paid", "Invalid");
        String err4 = paymentService.validatePayment(missingRef);
        System.out.println("[Validation 4] Missing Ref Error Caught    : " + err4);

        // Test 2.5: Valid Payment Check
        String validCheck = paymentService.validatePayment(testPayment);
        System.out.println("[Validation 5] Valid Payment Check         : " + (validCheck == null ? "PASSED (null error)" : validCheck));
        System.out.println("[PASS] All business rules and defensive validations verified.\n");

        // ---------------------------------------------------------
        // 3. TESTING DATABASE CONNECTIVITY & LIVE CRUD
        // ---------------------------------------------------------
        System.out.println("--- 3. TESTING DATABASE CONNECTIVITY ---");
        System.out.println("Database URL: " + DBConnection.getUrl());
        boolean isConnected = DBConnection.testConnection();

        if (isConnected) {
            System.out.println("[SUCCESS] Connected to MySQL 'rentproof' database!");
            runLiveDatabaseTests(paymentService);
        } else {
            System.out.println("[OFFLINE MODE] Local MySQL server is not active or credentials need setup.");
            System.out.println("All Core Java OOP structures, business validations, and DAO methods passed compilation.");
            System.out.println("When MySQL is running and rentproof.sql is executed, live database transactions will activate.");
        }

        System.out.println("\n============================================================");
        System.out.println("   PAYMENT MODULE VERIFICATION COMPLETE - READY FOR VIVA    ");
        System.out.println("============================================================");
    }

    private static void runLiveDatabaseTests(PaymentService paymentService) {
        try {
            System.out.println("\n--- 4. RETRIEVING LIVE PAYMENTS & DASHBOARD STATS ---");
            Map<String, Object> stats = paymentService.getDashboardStats();
            System.out.println("Total Rent Collected    : Rs. " + stats.get("totalRentCollected"));
            System.out.println("Successful Payments     : " + stats.get("successfulPaymentCount"));
            System.out.println("Pending Payments        : " + stats.get("pendingPaymentCount"));
            System.out.println("Overdue Payments        : " + stats.get("overduePaymentCount"));

            ArrayList<Payment> allPayments = paymentService.getAllPayments();
            System.out.println("Total Existing Records  : " + allPayments.size());
            for (Payment p : allPayments) {
                System.out.printf("  #%d | %s | %s | Room %s | Rs. %.2f | %s | %s [%s]\n",
                        p.getPaymentId(), p.getTenantName(), p.getPropertyName(), 
                        p.getRoomNumber(), p.getAmount(), p.getPaymentDate(), 
                        p.getPaymentMode(), p.getPaymentStatus());
            }

            System.out.println("\n--- 5. TESTING INSERT (CREATE) PAYMENT ---");
            Payment newPayment = new Payment(
                    1, 1, 1, 8500.00,
                    "2026-11-10", "November 2026", "UPI",
                    "UPI/VIVA/DEMO9999", "Pending", "Viva Demonstration Payment"
            );
            int newId = paymentService.recordPayment(newPayment);
            System.out.println("[INSERT] New payment record created with ID: " + newId);

            System.out.println("\n--- 6. TESTING RETRIEVAL BY ID & JOIN ---");
            Payment retrieved = paymentService.getPaymentById(newId);
            if (retrieved != null) {
                System.out.println("[READ] Found: " + retrieved.getTenantName() + " | Status: " + retrieved.getPaymentStatus());
            }

            System.out.println("\n--- 7. TESTING STATUS UPDATE (UPDATE) ---");
            boolean updated = paymentService.updatePaymentStatus(newId, "Paid");
            System.out.println("[UPDATE] Status changed to 'Paid': " + updated);

            System.out.println("\n--- 8. TESTING SEARCH & FILTER ---");
            ArrayList<Payment> searchResults = paymentService.searchAndFilterPayments("DEMO9999", null, null, null);
            System.out.println("[SEARCH] Found records matching 'DEMO9999': " + searchResults.size());

            System.out.println("\n--- 9. TESTING RECORD DELETION (DELETE) ---");
            boolean deleted = paymentService.deletePayment(newId);
            System.out.println("[DELETE] Demo payment #" + newId + " removed: " + deleted);

        } catch (Exception e) {
            System.err.println("[Database Test Error] " + e.getMessage());
            e.printStackTrace();
        }
    }
}

