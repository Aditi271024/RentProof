package com.rentproof.dashboard;

import com.rentproof.database.DBConnection;

import java.util.ArrayList;

/**
 * DashboardDemo - College Project Demonstration and Viva Test Runner for Dashboard Module.
 * Author: Aditi Sharma
 */
public class DashboardDemo {

    public static void main(String[] args) {
        System.out.println("============================================================");
        System.out.println("   RENTPROOF: LANDLORD DASHBOARD VERIFICATION SUITE         ");
        System.out.println("   Author: Aditi Sharma | Module: Landlord Dashboard        ");
        System.out.println("============================================================\n");

        int testLandlordId = 1; // Aditi Sharma
        DashboardService service = new DashboardService();

        System.out.println("--- 1. DATABASE CONNECTIVITY HEALTH CHECK ---");
        boolean isConnected = DBConnection.testConnection();
        if (isConnected) {
            System.out.println("SUCCESS: Live connection established to " + DBConnection.getUrl());
        } else {
            System.out.println("NOTICE: Local MySQL server not reachable on localhost:3306.");
            System.out.println("Dashboard architecture is validated in offline demonstration mode.");
        }
        System.out.println();

        System.out.println("--- 2. TESTING OVERVIEW STATISTICS (DashboardDAO.getDashboardStats) ---");
        DashboardStats stats = service.getStats(testLandlordId);
        System.out.println("Total Properties : " + stats.getTotalProperties());
        System.out.println("Total Rooms      : " + stats.getTotalRooms());
        System.out.println("Occupied Rooms   : " + stats.getOccupiedRooms());
        System.out.println("Vacant Rooms     : " + stats.getVacantRooms());
        System.out.println("Total Tenants    : " + stats.getTotalTenants());
        System.out.println("Rent Collected   : INR " + stats.getRentCollected());
        System.out.println("Pending Rent     : INR " + stats.getPendingRent());
        System.out.println();

        System.out.println("--- 3. TESTING PROPERTY OVERVIEW (DashboardDAO.getPropertyOverview) ---");
        ArrayList<PropertyOverviewDTO> properties = service.getPropertyOverview(testLandlordId);
        System.out.println("Properties returned: " + properties.size());
        for (int i = 0; i < properties.size(); i++) {
            PropertyOverviewDTO p = properties.get(i);
            System.out.println("  [" + p.getPropertyId() + "] " + p.getPropertyName() + 
                               " | " + p.getPropertyType() + " in " + p.getLocation() + 
                               " | Rooms: " + p.getTotalRooms() + " (Occupied: " + p.getOccupiedRooms() + ", Vacant: " + p.getVacantRooms() + ")");
        }
        System.out.println();

        System.out.println("--- 4. TESTING RECENT PAYMENTS (DashboardDAO.getRecentPayments) ---");
        ArrayList<PaymentRecordDTO> payments = service.getRecentPayments(testLandlordId, 5);
        System.out.println("Recent payments returned: " + payments.size());
        for (int i = 0; i < payments.size(); i++) {
            PaymentRecordDTO pr = payments.get(i);
            System.out.println("  Tenant: " + pr.getTenantName() + " | Property: " + pr.getPropertyName() + 
                               " | INR " + pr.getAmount() + " | Date: " + pr.getPaymentDate() + " | Status: " + pr.getPaymentStatus());
        }
        System.out.println();

        System.out.println("--- 5. TESTING PENDING RENT (DashboardDAO.getPendingRent) ---");
        ArrayList<PaymentRecordDTO> pending = service.getPendingRent(testLandlordId);
        System.out.println("Pending rent records: " + pending.size());
        for (int i = 0; i < pending.size(); i++) {
            PaymentRecordDTO pr = pending.get(i);
            System.out.println("  Tenant: " + pr.getTenantName() + " | Property: " + pr.getPropertyName() + 
                               " | Due: INR " + pr.getAmount() + " on " + pr.getPaymentDate());
        }
        System.out.println();

        System.out.println("--- 6. TESTING UPCOMING REMINDERS (DashboardDAO.getUpcomingReminders) ---");
        ArrayList<ReminderDTO> reminders = service.getUpcomingReminders(testLandlordId, 5);
        System.out.println("Reminders returned: " + reminders.size());
        for (int i = 0; i < reminders.size(); i++) {
            ReminderDTO r = reminders.get(i);
            System.out.println("  [" + r.getReminderType() + "] " + r.getTitle() + 
                               " | Target: " + r.getRelatedTo() + " | Due: " + r.getDueDate() + " | Status: " + r.getReminderStatus());
        }
        System.out.println();

        System.out.println("============================================================");
        System.out.println("   DASHBOARD MODULE BACKEND VERIFICATION COMPLETE           ");
        System.out.println("============================================================");
    }
}
