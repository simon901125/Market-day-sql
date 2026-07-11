package com.example.demo.dto.log;

public class StatusLogEntry {

    private Long requestLogId;
    private String targetType;
    private Long targetId;
    private String statusField;
    private String newStatus;

    public StatusLogEntry() {
    }

    public StatusLogEntry(Long requestLogId, String targetType, Long targetId, String statusField, String newStatus) {
        this.requestLogId = requestLogId;
        this.targetType = targetType;
        this.targetId = targetId;
        this.statusField = statusField;
        this.newStatus = newStatus;
    }

    public Long getRequestLogId() {
        return requestLogId;
    }

    public void setRequestLogId(Long requestLogId) {
        this.requestLogId = requestLogId;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getStatusField() {
        return statusField;
    }

    public void setStatusField(String statusField) {
        this.statusField = statusField;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }
}
