package com.rentproof.dashboard;

/**
 * DashboardStats - Data Transfer Object representing high-level portfolio statistics.
 * 
 * Strict compliance with Core Java SE (up to Lecture 42):
 * - Private encapsulated fields
 * - Public getters and setters
 * - Overloaded constructors
 * - toString() representation
 */
public class DashboardStats {

    private int totalProperties;
    private int totalRooms;
    private int occupiedRooms;
    private int vacantRooms;
    private int totalTenants;
    private double rentCollected;
    private double pendingRent;

    public DashboardStats() {
    }

    public DashboardStats(int totalProperties, int totalRooms, int occupiedRooms,
                          int vacantRooms, int totalTenants, double rentCollected, double pendingRent) {
        this.totalProperties = totalProperties;
        this.totalRooms = totalRooms;
        this.occupiedRooms = occupiedRooms;
        this.vacantRooms = vacantRooms;
        this.totalTenants = totalTenants;
        this.rentCollected = rentCollected;
        this.pendingRent = pendingRent;
    }

    public int getTotalProperties() {
        return totalProperties;
    }

    public void setTotalProperties(int totalProperties) {
        this.totalProperties = totalProperties;
    }

    public int getTotalRooms() {
        return totalRooms;
    }

    public void setTotalRooms(int totalRooms) {
        this.totalRooms = totalRooms;
    }

    public int getOccupiedRooms() {
        return occupiedRooms;
    }

    public void setOccupiedRooms(int occupiedRooms) {
        this.occupiedRooms = occupiedRooms;
    }

    public int getVacantRooms() {
        return vacantRooms;
    }

    public void setVacantRooms(int vacantRooms) {
        this.vacantRooms = vacantRooms;
    }

    public int getTotalTenants() {
        return totalTenants;
    }

    public void setTotalTenants(int totalTenants) {
        this.totalTenants = totalTenants;
    }

    public double getRentCollected() {
        return rentCollected;
    }

    public void setRentCollected(double rentCollected) {
        this.rentCollected = rentCollected;
    }

    public double getPendingRent() {
        return pendingRent;
    }

    public void setPendingRent(double pendingRent) {
        this.pendingRent = pendingRent;
    }

    @Override
    public String toString() {
        return "DashboardStats{" +
                "totalProperties=" + totalProperties +
                ", totalRooms=" + totalRooms +
                ", occupiedRooms=" + occupiedRooms +
                ", vacantRooms=" + vacantRooms +
                ", totalTenants=" + totalTenants +
                ", rentCollected=" + rentCollected +
                ", pendingRent=" + pendingRent +
                '}';
    }
}
