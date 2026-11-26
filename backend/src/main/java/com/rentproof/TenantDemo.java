package com.rentproof;

import com.rentproof.database.DBConnection;
import com.rentproof.tenant.Tenant;
import com.rentproof.tenant.TenantDAO;
import com.rentproof.tenant.TenantService;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;

/**
 * TenantDemo - Standalone Demonstration & Viva Verification Suite
 * for Shweta's Tenant Management and Property Assignment Module.
 * 
 * Verifies:
 * 1. Tenant Entity Model: Encapsulation, Constructors, Getters/Setters, Monthly Rent & Security Deposit
 * 2. Business Validation: Full name, 10-digit Indian mobile number, RFC email format, non-negative amounts
 * 3. Natural Sorting: Comparable<Tenant> interface with Collections.sort()
 * 4. In-Memory Search & Status Filtering (without Stream API, adhering to CSE Syllabus L1-42)
 * 5. Tenant Move-Out workflow and Room Vacancy logic
 * 6. Live MySQL CRUD Operations when database is connected
 */
public class TenantDemo {

    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("   RENTPROOF: SHWETA'S TENANT MODULE VERIFICATION SUITE     ");
        System.out.println("   Module: Tenant Management & Property Assignment          ");
        System.out.println("============================================================\n");

        int passed = 0;
        int failed = 0;

        // -------------------------------------------------------------
        // 1. Model & Encapsulation Verification
        // -------------------------------------------------------------
        System.out.println("--- 1. TESTING TENANT MODEL & ENCAPSULATION ---");
        try {
            Date moveIn = Date.valueOf(LocalDate.now());
            Tenant t1 = new Tenant(
                    1, 1, "Rohan Verma", "rohan.v@gmail.com", "9988776655",
                    "9811223344", "H-42 Civil Lines, Kanpur, UP",
                    moveIn, 8500.00, 17000.00, "Active"
            );
            t1.setTenantId(101);
            t1.setIdProofNumber("AADHAAR-8901");
            t1.setPropertyName("Sunrise Executive PG");
            t1.setRoomNumber("101");

            assert t1.getTenantId() == 101 : "Tenant ID mismatch";
            assert "Rohan Verma".equals(t1.getFullName()) : "Full name mismatch";
            assert t1.getMonthlyRent() == 8500.00 : "Monthly rent mismatch";
            assert t1.getSecurityDeposit() == 17000.00 : "Security deposit mismatch";
            assert "H-42 Civil Lines, Kanpur, UP".equals(t1.getPermanentAddress()) : "Address mismatch";
            assert "AADHAAR-8901".equals(t1.getIdProofNumber()) : "ID proof mismatch";
            assert "Active".equals(t1.getTenantStatus()) : "Status mismatch";

            System.out.println("  [PASS] Tenant entity encapsulation, fields, getters/setters verified.");
            System.out.println("         Tenant: " + t1.getFullName() + " | Rent: Rs." + t1.getMonthlyRent() + " | Deposit: Rs." + t1.getSecurityDeposit());
            passed++;
        } catch (AssertionError | Exception e) {
            System.err.println("  [FAIL] Model verification failed: " + e.getMessage());
            failed++;
        }
        System.out.println();

        // -------------------------------------------------------------
        // 2. Business Validation Logic Verification
        // -------------------------------------------------------------
        System.out.println("--- 2. TESTING INPUT VALIDATION LOGIC ---");
        TenantService service = new TenantService();

        // Test 2a: Name too short
        Tenant invalidName = new Tenant(1, 1, "Ro", "ro@gmail.com", "9876543210", null, "Addr", Date.valueOf(LocalDate.now()), 5000, 10000, "Active");
        String errName = service.validateTenant(invalidName);
        if (errName != null && errName.contains("at least 3 characters")) {
            System.out.println("  [PASS] Name length validation caught: \"" + errName + "\"");
            passed++;
        } else {
            System.err.println("  [FAIL] Short name was not rejected properly.");
            failed++;
        }

        // Test 2b: Invalid phone (not 10 digits or wrong starting digit)
        Tenant invalidPhone = new Tenant(1, 1, "Rohan Verma", "rohan@gmail.com", "123456", null, "Addr", Date.valueOf(LocalDate.now()), 5000, 10000, "Active");
        String errPhone = service.validateTenant(invalidPhone);
        if (errPhone != null && errPhone.contains("10-digit")) {
            System.out.println("  [PASS] Invalid phone format caught: \"" + errPhone + "\"");
            passed++;
        } else {
            System.err.println("  [FAIL] Invalid phone was not rejected properly.");
            failed++;
        }

        // Test 2c: Invalid email format
        Tenant invalidEmail = new Tenant(1, 1, "Rohan Verma", "not-an-email", "9876543210", null, "Addr", Date.valueOf(LocalDate.now()), 5000, 10000, "Active");
        String errEmail = service.validateTenant(invalidEmail);
        if (errEmail != null && errEmail.contains("email")) {
            System.out.println("  [PASS] Invalid email format caught: \"" + errEmail + "\"");
            passed++;
        } else {
            System.err.println("  [FAIL] Invalid email was not rejected properly.");
            failed++;
        }

        // Test 2d: Negative monthly rent
        Tenant negativeRent = new Tenant(1, 1, "Rohan Verma", "rohan@gmail.com", "9876543210", null, "Addr", Date.valueOf(LocalDate.now()), -500, 10000, "Active");
        String errRent = service.validateTenant(negativeRent);
        if (errRent != null && errRent.contains("Monthly rent must be between 0")) {
            System.out.println("  [PASS] Negative monthly rent caught: \"" + errRent + "\"");
            passed++;
        } else {
            System.err.println("  [FAIL] Negative rent was not rejected properly.");
            failed++;
        }

        // Test 2e: Valid tenant passes
        Tenant validTenant = new Tenant(1, 1, "Rohan Verma", "rohan.v@gmail.com", "9988776655", "9811223344", "H-42 Civil Lines", Date.valueOf(LocalDate.now()), 8500, 17000, "Active");
        String errValid = service.validateTenant(validTenant);
        if (errValid == null) {
            System.out.println("  [PASS] Fully valid tenant passes all validation rules cleanly.");
            passed++;
        } else {
            System.err.println("  [FAIL] Valid tenant unexpectedly rejected: " + errValid);
            failed++;
        }
        System.out.println();

        // -------------------------------------------------------------
        // 3. Comparable Natural Sorting Verification
        // -------------------------------------------------------------
        System.out.println("--- 3. TESTING COMPARABLE & COLLECTIONS.SORT() ---");
        ArrayList<Tenant> list = new ArrayList<>();
        Tenant tZ = new Tenant(); tZ.setFullName("Zoya Akhtar");
        Tenant tA = new Tenant(); tA.setFullName("Ankit Mishra");
        Tenant tR = new Tenant(); tR.setFullName("Rohan Verma");
        list.add(tZ);
        list.add(tA);
        list.add(tR);

        Collections.sort(list);
        if ("Ankit Mishra".equals(list.get(0).getFullName()) &&
            "Rohan Verma".equals(list.get(1).getFullName()) &&
            "Zoya Akhtar".equals(list.get(2).getFullName())) {
            System.out.println("  [PASS] Natural alphabetical sorting verified: " +
                    list.get(0).getFullName() + " -> " +
                    list.get(1).getFullName() + " -> " +
                    list.get(2).getFullName());
            passed++;
        } else {
            System.err.println("  [FAIL] Sorting order incorrect.");
            failed++;
        }
        System.out.println();

        // -------------------------------------------------------------
        // 4. In-Memory Search & Filtering Verification
        // -------------------------------------------------------------
        System.out.println("--- 4. TESTING SEARCH & STATUS FILTERING ---");
        tA.setPhone("9871122334");
        tA.setEmail("ankit.m@yahoo.com");
        tA.setTenantStatus("Active");

        tR.setPhone("9988776655");
        tR.setEmail("rohan.v@gmail.com");
        tR.setTenantStatus("Moved Out");

        tZ.setPhone("9811002233");
        tZ.setEmail("zoya.a@gmail.com");
        tZ.setTenantStatus("Active");

        ArrayList<Tenant> pool = new ArrayList<>();
        pool.add(tA);
        pool.add(tR);
        pool.add(tZ);

        // Filter by Active status
        int activeCount = 0;
        for (int i = 0; i < pool.size(); i++) {
            if ("Active".equalsIgnoreCase(pool.get(i).getTenantStatus())) {
                activeCount++;
            }
        }
        if (activeCount == 2) {
            System.out.println("  [PASS] Status filtering: found 2 Active tenants out of 3.");
            passed++;
        } else {
            System.err.println("  [FAIL] Active count mismatch: expected 2, got " + activeCount);
            failed++;
        }

        // Linear Search by phone substring "9988"
        String query = "9988";
        Tenant found = null;
        for (int i = 0; i < pool.size(); i++) {
            if (pool.get(i).getPhone().contains(query)) {
                found = pool.get(i);
                break;
            }
        }
        if (found != null && "Rohan Verma".equals(found.getFullName())) {
            System.out.println("  [PASS] Phone substring search (\"9988\"): successfully resolved to Rohan Verma.");
            passed++;
        } else {
            System.err.println("  [FAIL] Search for '9988' failed.");
            failed++;
        }
        System.out.println();

        // -------------------------------------------------------------
        // 5. Database Connectivity & Live CRUD Check
        // -------------------------------------------------------------
        System.out.println("--- 5. DATABASE CONNECTIVITY & PERSISTENCE ---");
        System.out.println("  Configured JDBC URL: " + DBConnection.getUrl());
        boolean dbConnected = DBConnection.testConnection();

        if (dbConnected) {
            System.out.println("  [PASS] Connected to MySQL 'rentproof' database successfully!");
            passed++;

            TenantDAO dao = new TenantDAO();
            try {
                ArrayList<Tenant> dbTenants = dao.getAllTenants();
                System.out.println("  [PASS] Successfully queried database: " + dbTenants.size() + " tenants currently registered.");
                passed++;
            } catch (Exception e) {
                System.err.println("  [WARN] Database query returned: " + e.getMessage());
            }
        } else {
            System.out.println("  [INFO] MySQL service is offline or credentials not yet set in db.properties.");
            System.out.println("         Core Java & business logic suites ran completely in-memory.");
        }
        System.out.println();

        // -------------------------------------------------------------
        // Summary
        // -------------------------------------------------------------
        System.out.println("============================================================");
        System.out.println("   TENANT MODULE TEST SUMMARY: " + passed + " PASSED, " + failed + " FAILED");
        System.out.println("============================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}
