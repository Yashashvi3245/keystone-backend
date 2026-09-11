package com.keystone.controller;

import com.keystone.model.User;
import com.keystone.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Login endpoint — returns a JWT Bearer token")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Authenticate with email + password. Returns JWT token, role, and user ID.")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {

        AuthenticationService.LoginResult result =
                authenticationService.login(request.email(), request.password());

        return ResponseEntity.ok(new LoginResponse(
                result.token(),
                result.role(),
                result.userId(),
                result.name(),
                result.customerId()
        ));
    }

    public record LoginRequest(String email, String password) {}

    public record LoginResponse(
            String token,
            String role,
            Long   userId,
            String name,
            Long   customerId
    ) {}
}
