package com.keystone.controller;

import com.keystone.model.Site;
import com.keystone.service.SiteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SiteController {

    private final SiteService siteService;

    public SiteController(SiteService siteService) {
        this.siteService = siteService;
    }

    // =========================
    // GET ALL SITES
    // =========================
    @GetMapping("/sites")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER')")
    public List<Site> getAllSites() {
        return siteService.getAllSites();
    }

    // =========================
    // GET SITE BY ID
    // =========================
    @GetMapping("/sites/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER')")
    public ResponseEntity<Site> getSiteById(
            @PathVariable Long id) {

        return siteService.getSiteById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // =========================
    // GET CUSTOMER SITES
    // =========================
    @GetMapping("/customers/{customerId}/sites")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER')")
    public List<Site> getSitesByCustomer(
            @PathVariable Long customerId) {

        return siteService.getSitesByCustomerId(customerId);
    }

    // =========================
    // CREATE SITE
    // =========================
    @PostMapping("/customers/{customerId}/sites")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER')")
    public ResponseEntity<Site> createSite(
            @PathVariable Long customerId,
            @Valid @RequestBody Site site) {

        return siteService.createSite(customerId, site)
                .map(createdSite ->
                        ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(createdSite)
                )
                .orElse(ResponseEntity.notFound().build());
    }

    // =========================
    // UPDATE SITE
    // =========================
    @PutMapping("/sites/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER')")
    public ResponseEntity<Site> updateSite(
            @PathVariable Long id,
            @Valid @RequestBody Site updatedSite) {

        return siteService.updateSite(id, updatedSite)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // =========================
    // DELETE SITE
    // =========================
    @DeleteMapping("/sites/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'DISPATCHER')")
    public ResponseEntity<Void> deleteSite(
            @PathVariable Long id) {

        if (!siteService.deleteSite(id)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}