package com.cinephile.model;

import java.sql.Timestamp;

public class AuditLogEntry {
    private long auditId;
    private long bookingId;
    private String actionType;
    private String oldStatus;
    private String newStatus;
    private String details;
    private Timestamp loggedAt;

    public AuditLogEntry(long auditId, long bookingId, String actionType,
                         String oldStatus, String newStatus, String details, Timestamp loggedAt) {
        this.auditId = auditId;
        this.bookingId = bookingId;
        this.actionType = actionType;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.details = details;
        this.loggedAt = loggedAt;
    }

    public long getAuditId() { return auditId; }
    public long getBookingId() { return bookingId; }
    public String getActionType() { return actionType; }
    public String getOldStatus() { return oldStatus; }
    public String getNewStatus() { return newStatus; }
    public String getDetails() { return details; }
    public Timestamp getLoggedAt() { return loggedAt; }
}
