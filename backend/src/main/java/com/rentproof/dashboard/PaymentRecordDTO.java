package com.rentproof.dashboard;

/**
 * PaymentRecordDTO - Lightweight representation of payment transactions for dashboard display.
 */
public class PaymentRecordDTO {

    private int paymentId;
    private String tenantName;
    private String propertyName;
    private double amount;
    private String paymentDate;
    private String paymentMonth;
    private String paymentStatus;

    public PaymentRecordDTO() {
    }

    public PaymentRecordDTO(int paymentId, String tenantName, String propertyName,
                            double amount, String paymentDate, String paymentMonth, String paymentStatus) {
        this.paymentId = paymentId;
        this.tenantName = tenantName;
        this.propertyName = propertyName;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.paymentMonth = paymentMonth;
        this.paymentStatus = paymentStatus;
    }

    public int getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(int paymentId) {
        this.paymentId = paymentId;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(String paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getPaymentMonth() {
        return paymentMonth;
    }

    public void setPaymentMonth(String paymentMonth) {
        this.paymentMonth = paymentMonth;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    @Override
    public String toString() {
        return "PaymentRecordDTO{" +
                "paymentId=" + paymentId +
                ", tenantName='" + tenantName + '\'' +
                ", propertyName='" + propertyName + '\'' +
                ", amount=" + amount +
                ", paymentDate='" + paymentDate + '\'' +
                ", paymentStatus='" + paymentStatus + '\'' +
                '}';
    }
}
