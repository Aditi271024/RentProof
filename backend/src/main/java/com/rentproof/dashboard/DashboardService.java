package com.rentproof.dashboard;

import java.util.ArrayList;

/**
 * DashboardService - Business Logic and Service Layer for Dashboard.
 * 
 * Strict compliance with Core Java SE syllabus:
 * - Encapsulation
 * - Defensive copying and null handling
 * - Clean method contracts
 */
public class DashboardService {

    private final DashboardDAO dashboardDAO;

    public DashboardService() {
        this.dashboardDAO = new DashboardDAO();
    }

    public DashboardService(DashboardDAO dashboardDAO) {
        this.dashboardDAO = dashboardDAO;
    }

    /**
     * Retrieves aggregated portfolio statistics for a landlord.
     */
    public DashboardStats getStats(int landlordId) {
        if (landlordId <= 0) {
            landlordId = 1; // Default to demo landlord (Aditi Sharma)
        }
        return dashboardDAO.getDashboardStats(landlordId);
    }

    /**
     * Retrieves property room breakdown list.
     */
    public ArrayList<PropertyOverviewDTO> getPropertyOverview(int landlordId) {
        if (landlordId <= 0) {
            landlordId = 1;
        }
        return dashboardDAO.getPropertyOverview(landlordId);
    }

    /**
     * Retrieves recent payments list.
     */
    public ArrayList<PaymentRecordDTO> getRecentPayments(int landlordId, int limit) {
        if (landlordId <= 0) {
            landlordId = 1;
        }
        return dashboardDAO.getRecentPayments(landlordId, limit);
    }

    /**
     * Retrieves pending rent list.
     */
    public ArrayList<PaymentRecordDTO> getPendingRent(int landlordId) {
        if (landlordId <= 0) {
            landlordId = 1;
        }
        return dashboardDAO.getPendingRent(landlordId);
    }

    /**
     * Retrieves upcoming reminders list.
     */
    public ArrayList<ReminderDTO> getUpcomingReminders(int landlordId, int limit) {
        if (landlordId <= 0) {
            landlordId = 1;
        }
        return dashboardDAO.getUpcomingReminders(landlordId, limit);
    }
}
