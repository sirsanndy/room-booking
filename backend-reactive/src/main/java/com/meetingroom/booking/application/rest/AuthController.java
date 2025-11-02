package com.meetingroom.booking.application.rest;

import com.meetingroom.booking.application.dto.JwtResponse;
import com.meetingroom.booking.application.dto.LoginRequest;
import com.meetingroom.booking.application.dto.MessageResponse;
import com.meetingroom.booking.application.dto.SignupRequest;
import com.meetingroom.booking.domain.port.in.AuthUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Authentication", description = "Authentication endpoints for user registration and login (Reactive)")
public class AuthController {
    private static final Logger LOG = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthUseCase authUseCase;
    
    public AuthController(AuthUseCase authUseCase) {
        this.authUseCase = authUseCase;
    }
    
    @PostMapping("/signup")
    @Operation(summary = "Register new user", description = "Register a new user account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User registered successfully",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Bad request - Username or email already exists",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class)))
    })
    public Mono<ResponseEntity<MessageResponse>> registerUser(@Valid @RequestBody SignupRequest signupRequest) {
        LOG.info("Registering user request: {}", signupRequest.getUsername());
        
        return authUseCase.registerUser(
                signupRequest.getUsername(),
                signupRequest.getPassword(),
                signupRequest.getEmail(),
                signupRequest.getFullName()
            )
            .map(user -> ResponseEntity.ok(new MessageResponse("User registered successfully")))
            .onErrorResume(error -> {
                LOG.error("Error registering user: {}", error.getMessage());
                return Mono.just(ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse(error.getMessage())));
            });
    }
    
    @PostMapping("/signin")
    @Operation(summary = "User login", description = "Authenticate user and get JWT token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = JwtResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid credentials",
            content = @Content(mediaType = "application/json"))
    })
    public Mono<ResponseEntity<JwtResponse>> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        LOG.info("Authenticating user request: {}", loginRequest.getUsername());
        
        return authUseCase.authenticateUser(loginRequest.getUsername(), loginRequest.getPassword())
            .flatMap(token -> 
                authUseCase.getUserByUsername(loginRequest.getUsername())
                    .map(user -> {
                        JwtResponse response = new JwtResponse(
                            token,
                            user.getId(),
                            user.getUsername(),
                            user.getEmail(),
                            user.getRoles()
                        );
                        return ResponseEntity.ok(response);
                    })
            )
            .onErrorResume(error -> {
                LOG.error("Authentication error: {}", error.getMessage());
                return Mono.just(ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(null));
            });
    }
}
