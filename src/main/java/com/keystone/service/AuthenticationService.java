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

    public String login(String email, String password) {

        User user = userService
                .getUserByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Invalid email or password"
                        )
                );

        // TEMPORARY DEBUG
        System.out.println("================================");
        System.out.println("EMAIL = " + user.getEmail());
        System.out.println("HASH = " + user.getPassword());

        boolean passwordMatch =
                passwordEncoder.matches(
                        password,
                        user.getPassword()
                );

        System.out.println(
                "PASSWORD MATCH = " + passwordMatch
        );
        System.out.println("================================");

        if (!passwordMatch) {
            throw new RuntimeException(
                    "Invalid email or password"
            );
        }

        return jwtService.generateToken(
                user.getEmail()
        );
    }
}