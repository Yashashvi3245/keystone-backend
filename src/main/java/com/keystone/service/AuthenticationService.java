package com.keystone.service;

import com.keystone.model.User;
import com.keystone.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final UserService     userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService      jwtService;

    public AuthenticationService(UserService userService,
                                  PasswordEncoder passwordEncoder,
                                  JwtService jwtService) {
        this.userService     = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService      = jwtService;
    }

    /**
     * Authenticates user and returns a rich LoginResult containing
     * the JWT token, role, userId, name, and customerId (if CUSTOMER role).
     */
    public LoginResult login(String email, String password) {

        User user = userService.getUserByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getEmail());

        Long customerId = (user.getCustomer() != null)
                ? user.getCustomer().getId()
                : null;

        return new LoginResult(
                token,
                user.getRole().name(),
                user.getId(),
                user.getName(),
                customerId
        );
    }

    /**
     * Immutable value returned on successful login.
     */
    public record LoginResult(
            String token,
            String role,
            Long   userId,
            String name,
            Long   customerId
    ) {}
}
