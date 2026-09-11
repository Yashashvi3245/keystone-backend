package com.keystone.controller;

import com.keystone.model.User;
import com.keystone.service.UserService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // =========================
    // GET ALL USERS
    // MANAGER / DISPATCHER ONLY
    // =========================

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER')")
    public ResponseEntity<List<User>> getAllUsers() {

        return ResponseEntity.ok(
                userService.getAllUsers()
        );
    }

    // =========================
    // GET USER BY ID
    // MANAGER / DISPATCHER ONLY
    // =========================

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER')")
    public ResponseEntity<User> getUserById(
            @PathVariable Long id) {

        return userService.getUserById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity
                                .notFound()
                                .build()
                );
    }

    // =========================
    // GET CURRENT LOGGED-IN USER
    // ALL AUTHENTICATED USERS
    // =========================

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCurrentUser(
            Authentication authentication) {

        if (authentication == null
                || authentication.getName() == null) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("User is not authenticated");
        }

        return userService
                .getUserByEmail(
                        authentication.getName()
                )
                .map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity
                                .status(
                                        HttpStatus.NOT_FOUND
                                )
                                .body(null)
                );
    }

    // =========================
    // CREATE USER
    // MANAGER ONLY
    // =========================

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<User> createUser(
            @RequestBody User user) {

        return ResponseEntity.ok(
                userService.saveUser(user)
        );
    }

    // =========================
    // UPDATE USER
    // MANAGER ONLY
    // =========================

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<User> updateUser(
            @PathVariable Long id,
            @RequestBody User user) {

        return userService
                .updateUser(id, user)
                .map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity
                                .notFound()
                                .build()
                );
    }

    // =========================
    // DELETE USER
    // MANAGER ONLY
    // =========================

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id) {

        try {

            userService.deleteUser(id);

            return ResponseEntity
                    .noContent()
                    .build();

        } catch (RuntimeException e) {

            return ResponseEntity
                    .notFound()
                    .build();
        }
    }
}
