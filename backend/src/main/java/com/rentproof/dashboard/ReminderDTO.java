package com.rentproof.dashboard;

/**
 * ReminderDTO - Represents an upcoming landlord action reminder.
 */
public class ReminderDTO {

    private int reminderId;
    private String title;
    private String relatedTo;
    private String dueDate;
    private String reminderType;
    private String reminderStatus;

    public ReminderDTO() {
    }

    public ReminderDTO(int reminderId, String title, String relatedTo,
                       String dueDate, String reminderType, String reminderStatus) {
        this.reminderId = reminderId;
        this.title = title;
        this.relatedTo = relatedTo;
        this.dueDate = dueDate;
        this.reminderType = reminderType;
        this.reminderStatus = reminderStatus;
    }

    public int getReminderId() {
        return reminderId;
    }

    public void setReminderId(int reminderId) {
        this.reminderId = reminderId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRelatedTo() {
        return relatedTo;
    }

    public void setRelatedTo(String relatedTo) {
        this.relatedTo = relatedTo;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
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

    @Override
    public String toString() {
        return "ReminderDTO{" +
                "reminderId=" + reminderId +
                ", title='" + title + '\'' +
                ", relatedTo='" + relatedTo + '\'' +
                ", dueDate='" + dueDate + '\'' +
                ", reminderStatus='" + reminderStatus + '\'' +
                '}';
    }
}
