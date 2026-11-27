package com.rentproof.dashboard;

import java.util.ArrayList;
import java.util.Map;

/**
 * DashboardService - Business Logic Layer for Dashboard Metrics & Analytics
 * Module: Dashboard Module
 * Developer: Gungun (BTech CSE Project)
 * 
 * Responsibilities:
 * - Decouples presentation and HTTP layer from direct SQL queries
 * - Applies business aggregations and data validation
 * - Computes real-time occupancy and collection indicators
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
     * Retrieves aggregated system metrics for the landlord.
     */
    public DashboardStats getDashboardStats(int landlordId) {
        int targetId = landlordId > 0 ? landlordId : 1;
        return dashboardDAO.getDashboardStats(targetId);
    }

    /**
     * Retrieves recent activity audit trail.
     */
    public ArrayList<Map<String, String>> getRecentActivities(int landlordId, int limit) {
        int targetId = landlordId > 0 ? landlordId : 1;
        return dashboardDAO.getRecentActivities(targetId, limit > 0 ? limit : 8);
    }

    /**
     * Retrieves actionable urgent reminders for the dashboard.
     */
    public ArrayList<Map<String, Object>> getUrgentReminders(int landlordId, int limit) {
        int targetId = landlordId > 0 ? landlordId : 1;
        return dashboardDAO.getUrgentReminders(targetId, limit > 0 ? limit : 5);
    }
}
