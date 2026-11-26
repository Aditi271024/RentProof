package com.rentproof.property;

import java.sql.Timestamp;

/**
 * Property entity model representing a rental building/unit owned by a
 * landlord.
 * 
 * Concepts used (Syllabus up to Lecture 42):
 * - Classes and Objects
 * - Encapsulation (private fields, public getters and setters)
 * - Constructors (Default, Overloaded, Parameterized)
 * - Comparable interface (sorting properties alphabetically by name)
 * - Wrapper classes and primitive types (int, double)
 * - toString() override
 */
public class Property implements Comparable<Property> {

    private int propertyId;
    private int landlordId;
    private String propertyName;
    private String propertyType; // 'PG', 'Flat', 'Apartment', 'House'
    private String address;
    private String city;
    private String state;
    private String pincode;
    private int totalRooms;
    private double monthlyRent;
    private String description;
    private Timestamp createdAt;

    // Default Constructor
    public Property() {
    }

    // Parameterized Constructor without propertyId (used before DB persistence)
    public Property(int landlordId, String propertyName, String propertyType, String address,
            String city, String state, String pincode, int totalRooms,
            double monthlyRent, String description) {
        this.landlordId = landlordId;
        this.propertyName = propertyName;
        this.propertyType = propertyType;
        this.address = address;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.totalRooms = totalRooms;
        this.monthlyRent = monthlyRent;
        this.description = description;
    }

    // Full Parameterized Constructor (used for mapping existing database records)
    public Property(int propertyId, int landlordId, String propertyName, String propertyType,
            String address, String city, String state, String pincode,
            int totalRooms, double monthlyRent, String description, Timestamp createdAt) {
        this.propertyId = propertyId;
        this.landlordId = landlordId;
        this.propertyName = propertyName;
        this.propertyType = propertyType;
        this.address = address;
        this.city = city;
        this.state = state;
        this.pincode = pincode;
        this.totalRooms = totalRooms;
        this.monthlyRent = monthlyRent;
        this.description = description;
        this.createdAt = createdAt;
    }

    // --- Getters and Setters (Encapsulation) ---

    public int getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(int propertyId) {
        this.propertyId = propertyId;
    }

    public int getLandlordId() {
        return landlordId;
    }

    public void setLandlordId(int landlordId) {
        this.landlordId = landlordId;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(String propertyType) {
        this.propertyType = propertyType;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public int getTotalRooms() {
        return totalRooms;
    }

    public void setTotalRooms(int totalRooms) {
        this.totalRooms = totalRooms;
    }

    public double getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(double monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Comparable implementation: natural ordering by property name
     * (case-insensitive).
     * Fulfills Java syllabus requirement for Comparable interface.
     */
    @Override
    public int compareTo(Property other) {
        if (other == null || other.getPropertyName() == null) {
            return 1;
        }
        if (this.propertyName == null) {
            return -1;
        }
        return this.propertyName.compareToIgnoreCase(other.getPropertyName());
    }

    @Override
    public String toString() {
        return "Property{" +
                "propertyId=" + propertyId +
                ", landlordId=" + landlordId +
                ", propertyName='" + propertyName + '\'' +
                ", propertyType='" + propertyType + '\'' +
                ", city='" + city + '\'' +
                ", totalRooms=" + totalRooms +
                ", monthlyRent=" + monthlyRent +
                '}';
    }
}
