package models;

import java.time.LocalDateTime;

/**
 * Represents a message that has been reported by a user.
 * Reported messages are visible to the admin via CLI.
 */
public class ReportedMessage {

    private String id;
    private String messageId;    // the reported message
    private String reporterId;   // user who submitted the report
    private String reason;       // optional reason text
    private LocalDateTime reportedAt;

    public ReportedMessage() {}

    public ReportedMessage(String id, String messageId, String reporterId, String reason) {
        this.id = id;
        this.messageId = messageId;
        this.reporterId = reporterId;
        this.reason = reason;
        this.reportedAt = LocalDateTime.now();
    }

    // Getters
    public String getId()                   { return id; }
    public String getMessageId()            { return messageId; }
    public String getReporterId()           { return reporterId; }
    public String getReason()               { return reason; }
    public LocalDateTime getReportedAt()    { return reportedAt; }

    // Setters
    public void setId(String id)                    { this.id = id; }
    public void setMessageId(String messageId)      { this.messageId = messageId; }
    public void setReporterId(String reporterId)    { this.reporterId = reporterId; }
    public void setReason(String reason)            { this.reason = reason; }
    public void setReportedAt(LocalDateTime t)      { this.reportedAt = t; }

    @Override
    public String toString() {
        return "ReportedMessage{id='" + id + "', messageId='" + messageId + "', reporterId='" + reporterId + "'}";
    }
}
