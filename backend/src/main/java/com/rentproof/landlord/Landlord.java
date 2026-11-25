package com.rentproof.landlord;

import java.sql.Timestamp;

/**
 * Landlord entity model representing a registered property owner in RentProof.
 * 
 * Concepts used (Syllabus up to Lecture 42):
 * - Classes and Objects
 * - Encapsulation (private fields, public getters and setters)
 * - Constructors (Default, Parameterized, Overloaded)
 * - this keyword
 * - String manipulation
 * - toString() override
 */
public class Landlord {

    private int landlordId;
    private String fullName;
    private String email;
    private String phone;
    private String passwordHash;
    private String passwordSalt;
    private int passwordIterations;
    private Timestamp createdAt;

    // Default Constructor
    public Landlord() {
    }

    // Parameterized Constructor without landlordId (for registration before DB assigns ID)
    public Landlord(String fullName, String email, String phone, String passwordHash) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
    }

    // Full Parameterized Constructor (for mapping existing DB rows)
    public Landlord(int landlordId, String fullName, String email, String phone, String passwordHash, Timestamp createdAt) {
        this.landlordId = landlordId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    // --- Getters and Setters (Encapsulation) ---

    public int getLandlordId() {
        return landlordId;
    }

    public void setLandlordId(int landlordId) {
        this.landlordId = landlordId;
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    public int getPasswordIterations() {
        return passwordIterations;
    }

    public void setPasswordIterations(int passwordIterations) {
        this.passwordIterations = passwordIterations;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Landlord{" +
                "landlordId=" + landlordId +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
