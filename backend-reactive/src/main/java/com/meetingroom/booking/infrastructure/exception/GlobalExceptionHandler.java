package com.meetingroom.booking.infrastructure.exception;

import com.meetingroom.booking.application.dto.MessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<MessageResponse>> handleIllegalArgumentException(IllegalArgumentException ex) {
        LOG.error("Illegal argument: {}", ex.getMessage());
        return Mono.just(ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new MessageResponse(ex.getMessage())));
    }
    
    @ExceptionHandler(RuntimeException.class)
    public Mono<ResponseEntity<MessageResponse>> handleRuntimeException(RuntimeException ex) {
        LOG.error("Runtime exception: {}", ex.getMessage());
        return Mono.just(ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new MessageResponse(ex.getMessage())));
    }
    
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<MessageResponse>> handleValidationException(WebExchangeBindException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        
        LOG.error("Validation error: {}", errors);
        return Mono.just(ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new MessageResponse("Validation failed: " + errors)));
    }
    
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<MessageResponse>> handleGenericException(Exception ex) {
        LOG.error("Unexpected error: {}", ex.getMessage(), ex);
        return Mono.just(ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new MessageResponse("An unexpected error occurred")));
    }
}
