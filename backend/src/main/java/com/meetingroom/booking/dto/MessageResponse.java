package com.meetingroom.booking.dto;

public class MessageResponse {
    private static final long serialVersionUID = 1L;

    private String message;
    
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
