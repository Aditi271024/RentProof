package com.rentproof.property;

import java.sql.Timestamp;

/**
 * Room Model Class representing a room record in the 'rooms' table.
 * 
 * Part of Property & Room Management.
 * 
 * Concepts used (Syllabus up to Lecture 42):
 * - OOP: Encapsulation (private fields, public getters and setters)
 * - Constructor Overloading
 * - toString method override
 */
public class Room implements Comparable<Room> {

    private int roomId;
    private int propertyId;
    private String roomNumber;
    private double monthlyRent;
    private String roomStatus; // "Vacant", "Occupied", "Maintenance"
    private Timestamp createdAt;

    // Optional descriptive fields from joins
    private String propertyName;

    /**
     * Default constructor.
     */
    public Room() {
        this.roomStatus = "Vacant";
    }

    /**
     * Constructor for adding a new room (without ID or timestamp).
     */
    public Room(int propertyId, String roomNumber, double monthlyRent, String roomStatus) {
        this.propertyId = propertyId;
        this.roomNumber = roomNumber;
        this.monthlyRent = monthlyRent;
        this.roomStatus = (roomStatus != null && !roomStatus.trim().isEmpty()) ? roomStatus : "Vacant";
    }

    /**
     * Full constructor for mapping existing database records.
     */
    public Room(int roomId, int propertyId, String roomNumber, double monthlyRent, String roomStatus, Timestamp createdAt) {
        this.roomId = roomId;
        this.propertyId = propertyId;
        this.roomNumber = roomNumber;
        this.monthlyRent = monthlyRent;
        this.roomStatus = roomStatus;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public int getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(int propertyId) {
        this.propertyId = propertyId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public double getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(double monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    public String getRoomStatus() {
        return roomStatus;
    }

    public void setRoomStatus(String roomStatus) {
        this.roomStatus = roomStatus;
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

    @Override
    public int compareTo(Room other) {
        if (other == null) return 1;
        return this.roomNumber.compareToIgnoreCase(other.roomNumber);
    }

    @Override
    public String toString() {
        return "Room{" +
                "roomId=" + roomId +
                ", propertyId=" + propertyId +
                ", roomNumber='" + roomNumber + '\'' +
                ", monthlyRent=" + monthlyRent +
                ", roomStatus='" + roomStatus + '\'' +
                '}';
    }
}
