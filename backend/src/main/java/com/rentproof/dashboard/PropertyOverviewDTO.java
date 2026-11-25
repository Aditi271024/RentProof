package com.rentproof.dashboard;

/**
 * PropertyOverviewDTO - Summarizes property room inventory and occupancy status.
 */
public class PropertyOverviewDTO {

    private int propertyId;
    private String propertyName;
    private String propertyType;
    private String location;
    private int totalRooms;
    private int occupiedRooms;
    private int vacantRooms;

    public PropertyOverviewDTO() {
    }

    public PropertyOverviewDTO(int propertyId, String propertyName, String propertyType,
                               String location, int totalRooms, int occupiedRooms, int vacantRooms) {
        this.propertyId = propertyId;
        this.propertyName = propertyName;
        this.propertyType = propertyType;
        this.location = location;
        this.totalRooms = totalRooms;
        this.occupiedRooms = occupiedRooms;
        this.vacantRooms = vacantRooms;
    }

    public int getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(int propertyId) {
        this.propertyId = propertyId;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
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

    @Override
    public String toString() {
        return "PropertyOverviewDTO{" +
                "propertyId=" + propertyId +
                ", propertyName='" + propertyName + '\'' +
                ", propertyType='" + propertyType + '\'' +
                ", location='" + location + '\'' +
                ", totalRooms=" + totalRooms +
                ", occupiedRooms=" + occupiedRooms +
                ", vacantRooms=" + vacantRooms +
                '}';
    }
}
