package com.keystone.controller;

import com.keystone.service.AuthenticationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(
            AuthenticationService authenticationService) {

        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request) {

        String token = authenticationService.login(
                request.email(),
                request.password()
        );

        return ResponseEntity.ok(
                new LoginResponse(token)
        );
    }

    // TEMPORARY: reset test user password
    @GetMapping("/reset-test-password")
    public ResponseEntity<String> resetTestPassword() {

        authenticationService.resetTestPassword();

        return ResponseEntity.ok(
                "Test password reset successfully"
        );
    }

    public record LoginRequest(
            String email,
            String password
    ) {}

    public record LoginResponse(
            String token
    ) {}
}