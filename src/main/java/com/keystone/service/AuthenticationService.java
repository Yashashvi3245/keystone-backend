package com.keystone.service;

import com.keystone.model.User;
import com.keystone.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthenticationService(
            UserService userService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // LOGIN
    public String login(String email, String password) {

        User user = userService.getUserByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Invalid email or password"
                        )
                );

        if (!passwordEncoder.matches(
                password,
                user.getPassword())) {

            throw new RuntimeException(
                    "Invalid email or password"
            );
        }

        return jwtService.generateToken(
                user.getEmail()
        );
    }

    // TEMPORARY TEST PASSWORD RESET
    public void resetTestPassword() {

        User user = userService
                .getUserByEmail("testuser@example.com")
                .orElseThrow(() ->
                        new RuntimeException(
                                "Test user not found"
                        )
                );

        user.setPassword(
                passwordEncoder.encode("Test@123")
        );

        userService.saveExistingUser(user);
    }
}