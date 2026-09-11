package com.keystone.controller;

import com.keystone.model.Site;
import com.keystone.model.User;
import com.keystone.repository.UserRepository;
import com.keystone.service.SiteService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SiteController {

    private final SiteService siteService;
    private final UserRepository userRepository;

    public SiteController(
            SiteService siteService,
            UserRepository userRepository) {

        this.siteService = siteService;
        this.userRepository = userRepository;
    }

    // =========================
    // GET ALL SITES
    // MANAGER / DISPATCHER ONLY
    // =========================

    @GetMapping("/sites")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER')")
    public ResponseEntity<List<Site>> getAllSites() {

        return ResponseEntity.ok(
                siteService.getAllSites()
        );
    }

    // =========================
    // GET SITE BY ID
    // MANAGER / DISPATCHER
    // OR CUSTOMER'S OWN SITE
    // =========================

    @GetMapping("/sites/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getSiteById(
            @PathVariable Long id,
            Authentication authentication) {

        if (authentication == null
                || authentication.getName() == null) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("User is not authenticated");
        }

        User currentUser =
                userRepository
                        .findByEmail(authentication.getName())
                        .orElse(null);

        if (currentUser == null) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("User not found");
        }

        // Manager and Dispatcher can view any site
        if (currentUser.getRole().name().equals("MANAGER")
                || currentUser.getRole().name().equals("DISPATCHER")) {

            return siteService
                    .getSiteById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }

        // Customer can view only their own organization's site
        if (currentUser.getRole().name().equals("CUSTOMER")) {

            if (currentUser.getCustomer() == null) {

                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(
                                "Customer account is not linked to an organization"
                        );
            }

            return siteService
                    .getSiteById(id)
                    .map(site -> {

                        if (site.getCustomer() == null) {

                            return ResponseEntity
                                    .status(HttpStatus.FORBIDDEN)
                                    .body(
                                            "Site is not linked to a customer"
                                    );
                        }

                        if (!site.getCustomer()
                                .getId()
                                .equals(
                                        currentUser
                                                .getCustomer()
                                                .getId()
                                )) {

                            return ResponseEntity
                                    .status(HttpStatus.FORBIDDEN)
                                    .body(
                                            "You can only access sites belonging to your organization"
                                    );
                        }

                        return ResponseEntity.ok(site);
                    })
                    .orElse(ResponseEntity.notFound().build());
        }

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(
                        "You do not have permission to access this site"
                );
    }

    // =========================
    // GET CUSTOMER SITES
    // MANAGER / DISPATCHER
    // OR CUSTOMER'S OWN SITES
    // =========================

    @GetMapping("/customers/{customerId}/sites")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getSitesByCustomer(
            @PathVariable Long customerId,
            Authentication authentication) {

        if (authentication == null
                || authentication.getName() == null) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("User is not authenticated");
        }

        User currentUser =
                userRepository
                        .findByEmail(authentication.getName())
                        .orElse(null);

        if (currentUser == null) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("User not found");
        }

        // Manager and Dispatcher can view any customer's sites
        if (currentUser.getRole().name().equals("MANAGER")
                || currentUser.getRole().name().equals("DISPATCHER")) {

            return ResponseEntity.ok(
                    siteService.getSitesByCustomerId(
                            customerId
                    )
            );
        }

        // Customer can view only their own organization's sites
        if (currentUser.getRole().name().equals("CUSTOMER")) {

            if (currentUser.getCustomer() == null) {

                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(
                                "Customer account is not linked to an organization"
                        );
            }

            if (!currentUser.getCustomer()
                    .getId()
                    .equals(customerId)) {

                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(
                                "You can only access sites belonging to your organization"
                        );
            }

            return ResponseEntity.ok(
                    siteService.getSitesByCustomerId(
                            customerId
                    )
            );
        }

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(
                        "You do not have permission to access these sites"
                );
    }

    // =========================
    // CREATE SITE
    // MANAGER / DISPATCHER ONLY
    // =========================

    @PostMapping("/customers/{customerId}/sites")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER')")
    public ResponseEntity<Site> createSite(
            @PathVariable Long customerId,
            @Valid @RequestBody Site site) {

        return siteService
                .createSite(customerId, site)
                .map(createdSite ->
                        ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(createdSite)
                )
                .orElse(
                        ResponseEntity
                                .notFound()
                                .build()
                );
    }

    // =========================
    // UPDATE SITE
    // MANAGER / DISPATCHER ONLY
    // =========================

    @PutMapping("/sites/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER')")
    public ResponseEntity<Site> updateSite(
            @PathVariable Long id,
            @Valid @RequestBody Site updatedSite) {

        return siteService
                .updateSite(id, updatedSite)
                .map(ResponseEntity::ok)
                .orElse(
                        ResponseEntity
                                .notFound()
                                .build()
                );
    }

    // =========================
    // DELETE SITE
    // MANAGER / DISPATCHER ONLY
    // =========================

    @DeleteMapping("/sites/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER')")
    public ResponseEntity<Void> deleteSite(
            @PathVariable Long id) {

        if (!siteService.deleteSite(id)) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        return ResponseEntity
                .noContent()
                .build();
    }
}
