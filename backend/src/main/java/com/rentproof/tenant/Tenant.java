package com.rentproof.tenant;

import java.sql.Date;
import java.sql.Timestamp;

/**
 * Tenant entity model representing a tenant registered in the RentProof system.
 * Part of Shweta's Tenant Management and Property Assignment module.
 * 
 * Concepts used (Syllabus up to Lecture 42):
 * - Classes and Objects (OOP encapsulation)
 * - Constructors (Default, Overloaded, and Parameterized)
 * - Comparable interface for natural sorting by tenant name
 * - Wrapper classes and primitive types (int, double)
 * - Java SQL types (Date, Timestamp)
 * - Overridden toString() method
 */
public class Tenant implements Comparable<Tenant> {

    // Primary Database Fields (mapped to 'tenants' table)
    private int tenantId;
    private int propertyId;
    private int roomId;
    private String fullName;
    private String email;
    private String phone;
    private String emergencyContact;
    private String permanentAddress;
    private String idProofNumber;
    private Date moveInDate;
    private Date moveOutDate;
    private double monthlyRent;
    private double securityDeposit;
    private String tenantStatus; // 'Active', 'Moved Out', 'Pending'
    private Timestamp createdAt;

    // Relational Display Fields (populated from joined 'properties' & 'rooms'
    // tables)
    private String propertyName;
    private String roomNumber;

    /**
     * Default No-Argument Constructor.
     */
    public Tenant() {
        this.tenantStatus = "Active";
        this.securityDeposit = 0.0;
        this.monthlyRent = 0.0;
    }

    /**
     * Parameterized Constructor without tenantId (used when registering a new
     * tenant before DB insert).
     */
    public Tenant(int propertyId, int roomId, String fullName, String email, String phone,
            String emergencyContact, String permanentAddress, Date moveInDate,
            double monthlyRent, double securityDeposit, String tenantStatus) {
        this.propertyId = propertyId;
        this.roomId = roomId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.emergencyContact = emergencyContact;
        this.permanentAddress = permanentAddress;
        this.moveInDate = moveInDate;
        this.monthlyRent = monthlyRent;
        this.securityDeposit = securityDeposit;
        this.tenantStatus = (tenantStatus != null && !tenantStatus.trim().isEmpty()) ? tenantStatus : "Active";
    }

    /**
     * Full Parameterized Constructor (used when mapping existing database rows).
     */
    public Tenant(int tenantId, int propertyId, int roomId, String fullName, String email,
            String phone, String emergencyContact, String permanentAddress, String idProofNumber,
            Date moveInDate, Date moveOutDate, double monthlyRent, double securityDeposit,
            String tenantStatus, Timestamp createdAt, String propertyName, String roomNumber) {
        this.tenantId = tenantId;
        this.propertyId = propertyId;
        this.roomId = roomId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.emergencyContact = emergencyContact;
        this.permanentAddress = permanentAddress;
        this.idProofNumber = idProofNumber;
        this.moveInDate = moveInDate;
        this.moveOutDate = moveOutDate;
        this.monthlyRent = monthlyRent;
        this.securityDeposit = securityDeposit;
        this.tenantStatus = tenantStatus;
        this.createdAt = createdAt;
        this.propertyName = propertyName;
        this.roomNumber = roomNumber;
    }

    // ==========================================
    // GETTERS AND SETTERS (Encapsulation)
    // ==========================================

    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    public int getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(int propertyId) {
        this.propertyId = propertyId;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public String getPermanentAddress() {
        return permanentAddress;
    }

    public void setPermanentAddress(String permanentAddress) {
        this.permanentAddress = permanentAddress;
    }

    public String getIdProofNumber() {
        return idProofNumber;
    }

    public void setIdProofNumber(String idProofNumber) {
        this.idProofNumber = idProofNumber;
    }

    public Date getMoveInDate() {
        return moveInDate;
    }

    public void setMoveInDate(Date moveInDate) {
        this.moveInDate = moveInDate;
    }

    public Date getMoveOutDate() {
        return moveOutDate;
    }

    public void setMoveOutDate(Date moveOutDate) {
        this.moveOutDate = moveOutDate;
    }

    public double getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(double monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    public double getSecurityDeposit() {
        return securityDeposit;
    }

    public void setSecurityDeposit(double securityDeposit) {
        this.securityDeposit = securityDeposit;
    }

    public String getTenantStatus() {
        return tenantStatus;
    }

    public void setTenantStatus(String tenantStatus) {
        this.tenantStatus = tenantStatus;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    // ==========================================
    // COMPARABLE INTERFACE IMPLEMENTATION
    // Natural ordering: Alphabetical by Full Name
    // ==========================================
    @Override
    public int compareTo(Tenant other) {
        if (other == null) {
            return 1;
        }
        if (this.fullName == null && other.fullName == null) {
            return 0;
        }
        if (this.fullName == null) {
            return -1;
        }
        if (other.fullName == null) {
            return 1;
        }
        return this.fullName.compareToIgnoreCase(other.fullName);
    }

    // ==========================================
    // TO STRING METHOD FOR LOGGING & VIVA
    // ==========================================
    @Override
    public String toString() {
        return "Tenant [" +
                "ID=" + tenantId +
                ", Name='" + fullName + '\'' +
                ", Phone='" + phone + '\'' +
                ", Email='" + email + '\'' +
                ", Property='" + propertyName + "' (ID: " + propertyId + ")" +
                ", Room='" + roomNumber + "' (ID: " + roomId + ")" +
                ", Rent=₹" + monthlyRent +
                ", Status='" + tenantStatus + '\'' +
                ", MoveIn=" + moveInDate +
                ']';
    }
}
