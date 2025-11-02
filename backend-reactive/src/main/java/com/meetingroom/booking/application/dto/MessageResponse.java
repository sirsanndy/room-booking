package com.meetingroom.booking.application.dto;

public class MessageResponse {
    private String message;
    
    public MessageResponse() {
    }
    
    public MessageResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
