package com.rentproof.dashboard;

/**
 * DashboardStats - Aggregated System Metrics Data Transfer Object (DTO)
 * Module: Dashboard Module
 * Developer: Gungun (BTech CSE Project)
 * 
 * Aggregates live metrics from MySQL across:
 * - Properties & Rooms (Aditi)
 * - Tenants (Shweta)
 * - Payments & Revenue (Aanvi)
 * - Reminders & Maintenance Alerts (Gungun)
 * 
 * Concepts used:
 * - Encapsulation (Private fields, public getters and setters)
 * - Parameterized and Default Constructors
 * - JSON serialization utility method for Core Java HttpServer without third-party libraries
 */
public class DashboardStats {

    // Properties & Inventory
    private int totalProperties;
    private int totalTenants;
    private int totalRooms;
    private int occupiedRooms;
    private int vacantRooms;
    private int maintenanceRooms;
    private double occupancyRate;

    // Financials & Payments
    private int totalPayments;
    private double totalRentCollected;
    private int completedPayments;
    private int pendingPayments;
    private int overduePayments;

    // Reminders & Alerts
    private int totalReminders;
    private int pendingReminders;
    private int upcomingReminders;
    private int overdueReminders;
    private int completedReminders;

    public DashboardStats() {
    }

    public DashboardStats(int totalProperties, int totalTenants, int totalRooms, 
                          int occupiedRooms, int vacantRooms, int maintenanceRooms, 
                          double occupancyRate, int totalPayments, double totalRentCollected, 
                          int completedPayments, int pendingPayments, int overduePayments, 
                          int totalReminders, int pendingReminders, int upcomingReminders, 
                          int overdueReminders, int completedReminders) {
        this.totalProperties = totalProperties;
        this.totalTenants = totalTenants;
        this.totalRooms = totalRooms;
        this.occupiedRooms = occupiedRooms;
        this.vacantRooms = vacantRooms;
        this.maintenanceRooms = maintenanceRooms;
        this.occupancyRate = occupancyRate;
        this.totalPayments = totalPayments;
        this.totalRentCollected = totalRentCollected;
        this.completedPayments = completedPayments;
        this.pendingPayments = pendingPayments;
        this.overduePayments = overduePayments;
        this.totalReminders = totalReminders;
        this.pendingReminders = pendingReminders;
        this.upcomingReminders = upcomingReminders;
        this.overdueReminders = overdueReminders;
        this.completedReminders = completedReminders;
    }

    // Getters and Setters
    public int getTotalProperties() { return totalProperties; }
    public void setTotalProperties(int totalProperties) { this.totalProperties = totalProperties; }

    public int getTotalTenants() { return totalTenants; }
    public void setTotalTenants(int totalTenants) { this.totalTenants = totalTenants; }

    public int getTotalRooms() { return totalRooms; }
    public void setTotalRooms(int totalRooms) { this.totalRooms = totalRooms; }

    public int getOccupiedRooms() { return occupiedRooms; }
    public void setOccupiedRooms(int occupiedRooms) { this.occupiedRooms = occupiedRooms; }

    public int getVacantRooms() { return vacantRooms; }
    public void setVacantRooms(int vacantRooms) { this.vacantRooms = vacantRooms; }

    public int getMaintenanceRooms() { return maintenanceRooms; }
    public void setMaintenanceRooms(int maintenanceRooms) { this.maintenanceRooms = maintenanceRooms; }

    public double getOccupancyRate() { return occupancyRate; }
    public void setOccupancyRate(double occupancyRate) { this.occupancyRate = occupancyRate; }

    public int getTotalPayments() { return totalPayments; }
    public void setTotalPayments(int totalPayments) { this.totalPayments = totalPayments; }

    public double getTotalRentCollected() { return totalRentCollected; }
    public void setTotalRentCollected(double totalRentCollected) { this.totalRentCollected = totalRentCollected; }

    public int getCompletedPayments() { return completedPayments; }
    public void setCompletedPayments(int completedPayments) { this.completedPayments = completedPayments; }

    public int getPendingPayments() { return pendingPayments; }
    public void setPendingPayments(int pendingPayments) { this.pendingPayments = pendingPayments; }

    public int getOverduePayments() { return overduePayments; }
    public void setOverduePayments(int overduePayments) { this.overduePayments = overduePayments; }

    public int getTotalReminders() { return totalReminders; }
    public void setTotalReminders(int totalReminders) { this.totalReminders = totalReminders; }

    public int getPendingReminders() { return pendingReminders; }
    public void setPendingReminders(int pendingReminders) { this.pendingReminders = pendingReminders; }

    public int getUpcomingReminders() { return upcomingReminders; }
    public void setUpcomingReminders(int upcomingReminders) { this.upcomingReminders = upcomingReminders; }

    public int getOverdueReminders() { return overdueReminders; }
    public void setOverdueReminders(int overdueReminders) { this.overdueReminders = overdueReminders; }

    public int getCompletedReminders() { return completedReminders; }
    public void setCompletedReminders(int completedReminders) { this.completedReminders = completedReminders; }

    /**
     * Converts stats object to valid JSON string without external libraries.
     */
    public String toJson() {
        return String.format(
            "{" +
            "\"totalProperties\":%d," +
            "\"totalTenants\":%d," +
            "\"totalRooms\":%d," +
            "\"occupiedRooms\":%d," +
            "\"vacantRooms\":%d," +
            "\"maintenanceRooms\":%d," +
            "\"occupancyRate\":%.1f," +
            "\"totalPayments\":%d," +
            "\"totalRentCollected\":%.2f," +
            "\"completedPayments\":%d," +
            "\"pendingPayments\":%d," +
            "\"overduePayments\":%d," +
            "\"totalReminders\":%d," +
            "\"pendingReminders\":%d," +
            "\"upcomingReminders\":%d," +
            "\"overdueReminders\":%d," +
            "\"completedReminders\":%d" +
            "}",
            totalProperties, totalTenants, totalRooms, occupiedRooms, vacantRooms, maintenanceRooms,
            occupancyRate, totalPayments, totalRentCollected, completedPayments, pendingPayments, overduePayments,
            totalReminders, pendingReminders, upcomingReminders, overdueReminders, completedReminders
        );
    }

    @Override
    public String toString() {
        return "DashboardStats{" +
                "properties=" + totalProperties +
                ", tenants=" + totalTenants +
                ", rooms=" + totalRooms + " (occ: " + occupiedRooms + ", vac: " + vacantRooms + ")" +
                ", rentCollected=" + totalRentCollected +
                ", payments=" + totalPayments +
                ", reminders=" + totalReminders + " (pending: " + pendingReminders + ", overdue: " + overdueReminders + ")" +
                '}';
    }
}
