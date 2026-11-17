package com.rentproof.reminder;

import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * ReminderService encapsulates business logic and rules for Rent Reminder Management.
 * 
 * Module: Rent Reminder Management Module
 * Developer: Gungun (BTech CSE)
 * 
 * Responsibilities:
 * - Input validation & required field checking
 * - Due-date validation and timeline classification (Upcoming vs Overdue)
 * - Reminder status transitions and validation
 * - Business queries (Upcoming rent dues, Overdue alerts, Maintenance reminders)
 * - Statistical computations for landlord reminder dashboards
 * - Decoupling business logic from database operations (delegates to ReminderDAO)
 * 
 * Concepts used (Syllabus up to Lecture 42):
 * - Classes and Objects
 * - Encapsulation
 * - Exception Handling (IllegalArgumentException, SQLException)
 * - Basic Collections (ArrayList, Map, Collections.sort)
 * - Method Overloading & Constructors
 * - Date and Time API (LocalDate)
 */
public class ReminderService {

    private final ReminderDAO reminderDAO;

    // Allowed reminder types
    public static final String TYPE_RENT_DUE = "Rent Due";
    public static final String TYPE_MAINTENANCE = "Maintenance";
    public static final String TYPE_GENERAL = "General";
    public static final String TYPE_AGREEMENT_EXPIRY = "Agreement Expiry";

    private static final String[] VALID_TYPES = {
        TYPE_RENT_DUE,
        TYPE_MAINTENANCE,
        TYPE_GENERAL,
        TYPE_AGREEMENT_EXPIRY
    };

    // Allowed reminder statuses
    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_COMPLETED = "Completed";
    public static final String STATUS_SENT = "Sent";

    private static final String[] VALID_STATUSES = {
        STATUS_PENDING,
        STATUS_COMPLETED,
        STATUS_SENT
    };

    /**
     * Default constructor initializes its own ReminderDAO.
     */
    public ReminderService() {
        this.reminderDAO = new ReminderDAO();
    }

    /**
     * Overloaded constructor for dependency injection (useful in unit testing).
     * 
     * @param reminderDAO custom or mock ReminderDAO
     */
    public ReminderService(ReminderDAO reminderDAO) {
        this.reminderDAO = reminderDAO;
    }

    // =========================================================================
    // 1. Validation Logic
    // =========================================================================

    /**
     * Validates all input fields of a Reminder object according to business rules.
     * 
     * @param reminder Reminder object to validate
     * @return error message string if invalid, or null if all checks pass
     */
    public String validateReminder(Reminder reminder) {
        if (reminder == null) {
            return "Reminder details cannot be null.";
        }

        if (reminder.getLandlordId() <= 0) {
            return "Valid Landlord ID is required.";
        }

        // Title validation
        if (reminder.getTitle() == null || reminder.getTitle().trim().isEmpty()) {
            return "Reminder title is required.";
        }
        if (reminder.getTitle().trim().length() < 3) {
            return "Reminder title must be at least 3 characters long.";
        }
        if (reminder.getTitle().trim().length() > 150) {
            return "Reminder title cannot exceed 150 characters.";
        }

        // Message validation
        if (reminder.getMessage() == null || reminder.getMessage().trim().isEmpty()) {
            return "Reminder message is required.";
        }
        if (reminder.getMessage().trim().length() < 5) {
            return "Reminder message must be at least 5 characters long.";
        }

        // Due date validation
        if (reminder.getDueDate() == null) {
            return "Due date is required.";
        }

        // Type validation
        if (!isValidReminderType(reminder.getReminderType())) {
            return "Invalid reminder type. Allowed types: 'Rent Due', 'Maintenance', 'General', 'Agreement Expiry'.";
        }

        // Status validation
        if (!isValidReminderStatus(reminder.getReminderStatus())) {
            return "Invalid reminder status. Allowed statuses: 'Pending', 'Completed', 'Sent'.";
        }

        return null; // All validation checks passed successfully
    }

    /**
     * Helper to verify if reminder type is among allowed categories.
     */
    public boolean isValidReminderType(String type) {
        if (type == null) return false;
        for (String valid : VALID_TYPES) {
            if (valid.equalsIgnoreCase(type.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper to verify if reminder status is valid.
     */
    public boolean isValidReminderStatus(String status) {
        if (status == null) return false;
        for (String valid : VALID_STATUSES) {
            if (valid.equalsIgnoreCase(status.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parses an ISO date string (YYYY-MM-DD) into java.sql.Date with validation.
     * 
     * @param dateStr ISO date string
     * @return java.sql.Date if valid
     * @throws IllegalArgumentException if format is invalid
     */
    public Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Date string cannot be empty.");
        }
        try {
            LocalDate localDate = LocalDate.parse(dateStr.trim());
            return Date.valueOf(localDate);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Expected format: YYYY-MM-DD (e.g. 2026-12-05).", e);
        }
    }

    // =========================================================================
    // 2. CRUD Business Methods
    // =========================================================================

    /**
     * Creates a new reminder after validating all required fields and business rules.
     * 
     * @param reminder Reminder object to add
     * @return generated reminder_id
     * @throws IllegalArgumentException if validation fails
     * @throws SQLException             if database insertion fails
     */
    public int createReminder(Reminder reminder) throws SQLException {
        String validationError = validateReminder(reminder);
        if (validationError != null) {
            throw new IllegalArgumentException("Validation Error: " + validationError);
        }

        return reminderDAO.addReminder(reminder);
    }

    /**
     * Retrieves all reminders across the system.
     */
    public ArrayList<Reminder> getAllReminders() {
        return reminderDAO.getAllReminders();
    }

    /**
     * Retrieves all reminders for a specific landlord, sorted by due date.
     */
    public ArrayList<Reminder> getRemindersByLandlord(int landlordId) {
        if (landlordId <= 0) {
            return new ArrayList<>();
        }
        return reminderDAO.getRemindersByLandlord(landlordId);
    }

    /**
     * Finds a reminder by its primary key ID.
     */
    public Reminder getReminderById(int reminderId) {
        if (reminderId <= 0) {
            return null;
        }
        return reminderDAO.getReminderById(reminderId);
    }

    /**
     * Retrieves reminders for a specific tenant.
     */
    public ArrayList<Reminder> getRemindersByTenant(int tenantId) {
        if (tenantId <= 0) {
            return new ArrayList<>();
        }
        return reminderDAO.getRemindersByTenant(tenantId);
    }

    /**
     * Retrieves all pending reminders for a landlord.
     */
    public ArrayList<Reminder> getPendingReminders(int landlordId) {
        return reminderDAO.getPendingReminders(landlordId);
    }

    /**
     * Updates an existing reminder after validating input data.
     */
    public boolean updateReminder(Reminder reminder) throws SQLException {
        if (reminder == null || reminder.getReminderId() <= 0) {
            throw new IllegalArgumentException("Reminder ID must be greater than 0 for update.");
        }
        String validationError = validateReminder(reminder);
        if (validationError != null) {
            throw new IllegalArgumentException("Validation Error: " + validationError);
        }
        return reminderDAO.updateReminder(reminder);
    }

    /**
     * Deletes a reminder by ID.
     */
    public boolean deleteReminder(int reminderId) throws SQLException {
        if (reminderId <= 0) {
            throw new IllegalArgumentException("Invalid reminder ID for deletion.");
        }
        return reminderDAO.deleteReminder(reminderId);
    }

    /**
     * Searches reminders by keyword.
     */
    public ArrayList<Reminder> searchReminders(int landlordId, String keyword) {
        return reminderDAO.searchReminders(landlordId, keyword);
    }

    // =========================================================================
    // 3. Status Management Business Logic
    // =========================================================================

    /**
     * Updates the status of a reminder.
     * 
     * @param reminderId reminder identifier
     * @param newStatus  new status string
     * @return true if updated
     * @throws IllegalArgumentException on invalid status
     * @throws SQLException             on database error
     */
    public boolean updateStatus(int reminderId, String newStatus) throws SQLException {
        if (reminderId <= 0) {
            throw new IllegalArgumentException("Invalid reminder ID.");
        }
        if (!isValidReminderStatus(newStatus)) {
            throw new IllegalArgumentException("Invalid reminder status: " + newStatus);
        }
        return reminderDAO.updateReminderStatus(reminderId, newStatus.trim());
    }

    /**
     * Marks a reminder as Completed.
     */
    public boolean markAsCompleted(int reminderId) throws SQLException {
        return updateStatus(reminderId, STATUS_COMPLETED);
    }

    /**
     * Marks a reminder as Sent.
     */
    public boolean markAsSent(int reminderId) throws SQLException {
        return updateStatus(reminderId, STATUS_SENT);
    }

    /**
     * Marks a reminder as Pending.
     */
    public boolean markAsPending(int reminderId) throws SQLException {
        return updateStatus(reminderId, STATUS_PENDING);
    }

    // =========================================================================
    // 4. Timeline & Categorization Filters
    // =========================================================================

    /**
     * Filters upcoming reminders for a landlord within a given number of days (default 7 days).
     * Upcoming = status is not completed AND due date is between today and today + days.
     * 
     * @param landlordId landlord ID
     * @param withinDays number of days ahead
     * @return ArrayList of upcoming Reminder objects
     */
    public ArrayList<Reminder> getUpcomingReminders(int landlordId, int withinDays) {
        ArrayList<Reminder> all = getRemindersByLandlord(landlordId);
        ArrayList<Reminder> upcoming = new ArrayList<>();
        int days = withinDays > 0 ? withinDays : 7;

        for (Reminder r : all) {
            if (r.isUpcoming(days)) {
                upcoming.add(r);
            }
        }
        return upcoming;
    }

    /**
     * Filters overdue reminders for a landlord.
     * Overdue = status is not completed AND due date is strictly before today.
     * 
     * @param landlordId landlord ID
     * @return ArrayList of overdue Reminder objects
     */
    public ArrayList<Reminder> getOverdueReminders(int landlordId) {
        ArrayList<Reminder> all = getRemindersByLandlord(landlordId);
        ArrayList<Reminder> overdue = new ArrayList<>();

        for (Reminder r : all) {
            if (r.isOverdue()) {
                overdue.add(r);
            }
        }
        return overdue;
    }

    /**
     * Filters completed reminders for a landlord.
     */
    public ArrayList<Reminder> getCompletedReminders(int landlordId) {
        ArrayList<Reminder> all = getRemindersByLandlord(landlordId);
        ArrayList<Reminder> completed = new ArrayList<>();

        for (Reminder r : all) {
            if (r.isCompleted()) {
                completed.add(r);
            }
        }
        return completed;
    }

    /**
     * Filters maintenance reminders where applicable.
     */
    public ArrayList<Reminder> getMaintenanceReminders(int landlordId) {
        ArrayList<Reminder> all = getRemindersByLandlord(landlordId);
        ArrayList<Reminder> maintenance = new ArrayList<>();

        for (Reminder r : all) {
            if (TYPE_MAINTENANCE.equalsIgnoreCase(r.getReminderType())) {
                maintenance.add(r);
            }
        }
        return maintenance;
    }

    /**
     * Computes real-time reminder metrics for dashboard summary cards.
     * 
     * @param landlordId landlord ID
     * @return Map containing: total, pending, completed, upcoming, overdue
     */
    public Map<String, Integer> calculateReminderStats(int landlordId) {
        ArrayList<Reminder> all = getRemindersByLandlord(landlordId);
        int total = all.size();
        int pending = 0;
        int completed = 0;
        int upcoming = 0;
        int overdue = 0;

        for (Reminder r : all) {
            if (r.isCompleted()) {
                completed++;
            } else {
                pending++;
                if (r.isOverdue()) {
                    overdue++;
                } else if (r.isUpcoming(7)) {
                    upcoming++;
                }
            }
        }

        Map<String, Integer> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("pending", pending);
        stats.put("completed", completed);
        stats.put("upcoming", upcoming);
        stats.put("overdue", overdue);
        return stats;
    }

    /**
     * Sorts a list of reminders chronologically using the Comparable interface.
     */
    public void sortRemindersByDueDate(ArrayList<Reminder> list) {
        if (list != null) {
            Collections.sort(list);
        }
    }

    /**
     * Loads active tenants from the database for populating tenant selection options.
     */
    public ArrayList<ReminderDAO.TenantSummary> getActiveTenants(int landlordId) {
        return reminderDAO.getActiveTenants(landlordId);
    }
}
