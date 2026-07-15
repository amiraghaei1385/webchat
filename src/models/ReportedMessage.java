package models;

import java.time.LocalDateTime;

// مدل ریپورت
public class ReportedMessage {

    private String id;
    private String idmessage;
    private String idreporter;
    private String reason;
    private LocalDateTime reportedat;

    public ReportedMessage() {
    }

    public ReportedMessage(String id, String idmessage, String idreporter, String reason) {
        this.id = id;
        this.idmessage = idmessage;
        this.idreporter = idreporter;
        this.reason = reason;
        this.reportedat = LocalDateTime.now();
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getMessageId() {
        return idmessage;
    }

    public String getReporterId() {
        return idreporter;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getReportedAt() {
        return reportedat;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setMessageId(String idmessage) {
        this.idmessage = idmessage;
    }

    public void setReporterId(String idreporter) {
        this.idreporter = idreporter;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setReportedAt(LocalDateTime t) {
        this.reportedat = t;
    }

    @Override
    public String toString() {
        return "ReportedMessage{id='" + id + "', messageId='" + idmessage + "', reporterId='" + idreporter + "'}";
    }
}