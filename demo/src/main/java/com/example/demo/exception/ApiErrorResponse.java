package com.example.demo.exception;

import java.util.Date;
import java.util.Map;

public class ApiErrorResponse {
    private Date timestamp;
    private int status;
    private String error;
    private String message;
    private Map<String, String> details;

    public ApiErrorResponse(int status, String error, String message) {
        this.timestamp = new Date();
        this.status = status;
        this.error = error;
        this.message = message;
    }

    public ApiErrorResponse(int status, String error, String message, Map<String, String> details) {
        this.timestamp = new Date();
        this.status = status;
        this.error = error;
        this.message = message;
        this.details = details;
    }

    // Getters and Setters
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Map<String, String> getDetails() { return details; }
    public void setDetails(Map<String, String> details) { this.details = details; }
}
