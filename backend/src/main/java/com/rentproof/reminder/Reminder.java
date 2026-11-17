package com.rentproof.reminder;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;

/**
 * Reminder entity model representing a reminder or notification created by a landlord.
 * 
 * Module: Rent Reminder Management Module
 * Developer: Gungun (BTech CSE)
 * 
 * Concepts used (Syllabus up to Lecture 42):
 * - Classes and Objects
 * - Encapsulation (private fields, public getters and setters)
 * - Constructors (Default, Overloaded, Parameterized)
 * - Comparable interface (sorting reminders by due date)
 * - Wrapper classes (Integer for nullable foreign key tenant_id)
 * - Date and Timestamp handling
 * - Overridden toString() for debugging and logging
 */
public class Reminder implements Comparable<Reminder> {

    private int reminderId;
    private int landlordId;
    private Integer tenantId;       // Nullable for general/maintenance reminders
    private String tenantName;      // Display helper when joined with tenants table
    private String title;
    private String message;
    private Date dueDate;
    private String reminderType;    // 'Rent Due', 'Maintenance', 'General', 'Agreement Expiry'
    private String reminderStatus;  // 'Pending', 'Completed', 'Sent'
    private Timestamp createdAt;

    /**
     * Default Constructor.
     * Initializes an empty Reminder object.
     */
    public Reminder() {
    }

    /**
     * Parameterized Constructor without reminderId.
     * Used when creating a new reminder before inserting into the database.
     * 
     * @param landlordId     landlord identifier
     * @param tenantId       tenant identifier (can be null for general reminders)
     * @param title          title of the reminder
     * @param message        body text of the reminder
     * @param dueDate        scheduled due date (java.sql.Date)
     * @param reminderType   category ('Rent Due', 'Maintenance', 'General')
     * @param reminderStatus status ('Pending', 'Completed', 'Sent')
     */
    public Reminder(int landlordId, Integer tenantId, String title, String message, 
                    Date dueDate, String reminderType, String reminderStatus) {
        this.landlordId = landlordId;
        this.tenantId = tenantId;
        this.title = title;
        this.message = message;
        this.dueDate = dueDate;
        this.reminderType = reminderType;
        this.reminderStatus = reminderStatus;
    }

    /**
     * Full Parameterized Constructor (without tenantName).
     * Used when mapping existing records directly from the database table.
     */
    public Reminder(int reminderId, int landlordId, Integer tenantId, String title, 
                    String message, Date dueDate, String reminderType, 
                    String reminderStatus, Timestamp createdAt) {
        this.reminderId = reminderId;
        this.landlordId = landlordId;
        this.tenantId = tenantId;
        this.title = title;
        this.message = message;
        this.dueDate = dueDate;
        this.reminderType = reminderType;
        this.reminderStatus = reminderStatus;
        this.createdAt = createdAt;
    }

    /**
     * Full Parameterized Constructor with tenantName.
     * Used when mapping database JOIN queries between reminders and tenants.
     */
    public Reminder(int reminderId, int landlordId, Integer tenantId, String tenantName, 
                    String title, String message, Date dueDate, String reminderType, 
                    String reminderStatus, Timestamp createdAt) {
        this.reminderId = reminderId;
        this.landlordId = landlordId;
        this.tenantId = tenantId;
        this.tenantName = tenantName;
        this.title = title;
        this.message = message;
        this.dueDate = dueDate;
        this.reminderType = reminderType;
        this.reminderStatus = reminderStatus;
        this.createdAt = createdAt;
    }

    // =========================================================================
    // Getters and Setters (Encapsulation)
    // =========================================================================

    public int getReminderId() {
        return reminderId;
    }

    public void setReminderId(int reminderId) {
        this.reminderId = reminderId;
    }

    public int getLandlordId() {
        return landlordId;
    }

    public void setLandlordId(int landlordId) {
        this.landlordId = landlordId;
    }

    public Integer getTenantId() {
        return tenantId;
    }

    public void setTenantId(Integer tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public String getReminderType() {
        return reminderType;
    }

    public void setReminderType(String reminderType) {
        this.reminderType = reminderType;
    }

    public String getReminderStatus() {
        return reminderStatus;
    }

    public void setReminderStatus(String reminderStatus) {
        this.reminderStatus = reminderStatus;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    // =========================================================================
    // Helper Business Methods
    // =========================================================================

    /**
     * Checks if the reminder is currently pending action.
     * 
     * @return true if reminderStatus equals "Pending" (case-insensitive)
     */
    public boolean isPending() {
        return "Pending".equalsIgnoreCase(this.reminderStatus);
    }

    /**
     * Checks if the reminder is marked as completed.
     * 
     * @return true if reminderStatus equals "Completed" or "Resolved"
     */
    public boolean isCompleted() {
        return "Completed".equalsIgnoreCase(this.reminderStatus) || 
               "Resolved".equalsIgnoreCase(this.reminderStatus);
    }

    /**
     * Checks if the reminder is overdue (due date is strictly before today and not completed).
     * 
     * @return true if due date has passed and reminder is not completed
     */
    public boolean isOverdue() {
        if (dueDate == null || isCompleted()) {
            return false;
        }
        LocalDate today = LocalDate.now();
        LocalDate due = dueDate.toLocalDate();
        return due.isBefore(today);
    }

    /**
     * Checks if the reminder is upcoming within a certain window of days (e.g. next 7 days).
     * 
     * @param days ahead window in days
     * @return true if due date is between today and today + days
     */
    public boolean isUpcoming(int days) {
        if (dueDate == null || isCompleted()) {
            return false;
        }
        LocalDate today = LocalDate.now();
        LocalDate due = dueDate.toLocalDate();
        LocalDate limit = today.plusDays(days);
        return (!due.isBefore(today)) && (!due.isAfter(limit));
    }

    // =========================================================================
    // Comparable Implementation (Sorting by Due Date Ascending)
    // =========================================================================

    /**
     * Compares reminders primarily by due date in chronological order (earliest first).
     * If due dates are identical, breaks ties by reminderId.
     */
    @Override
    public int compareTo(Reminder other) {
        if (other == null) {
            return 1;
        }
        if (this.dueDate == null && other.dueDate == null) {
            return Integer.compare(this.reminderId, other.reminderId);
        }
        if (this.dueDate == null) {
            return 1;
        }
        if (other.dueDate == null) {
            return -1;
        }
        int dateCompare = this.dueDate.compareTo(other.dueDate);
        if (dateCompare != 0) {
            return dateCompare;
        }
        return Integer.compare(this.reminderId, other.reminderId);
    }

    // =========================================================================
    // toString Override
    // =========================================================================

    @Override
    public String toString() {
        return "Reminder {" +
                "id=" + reminderId +
                ", landlordId=" + landlordId +
                ", tenantId=" + (tenantId != null ? tenantId : "N/A") +
                ", tenantName='" + (tenantName != null ? tenantName : "All Tenants / General") + '\'' +
                ", title='" + title + '\'' +
                ", dueDate=" + dueDate +
                ", type='" + reminderType + '\'' +
                ", status='" + reminderStatus + '\'' +
                ", overdue=" + isOverdue() +
                '}';
    }
}
