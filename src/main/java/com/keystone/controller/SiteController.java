package com.keystone.controller;

import com.keystone.model.Site;
import com.keystone.service.SiteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SiteController {

    private final SiteService siteService;

    public SiteController(SiteService siteService) {
        this.siteService = siteService;
    }

    // GET all sites
    @GetMapping("/sites")
    public List<Site> getAllSites() {
        return siteService.getAllSites();
    }

    // GET site by ID
    @GetMapping("/sites/{id}")
    public ResponseEntity<Site> getSiteById(@PathVariable Long id) {
        return siteService.getSiteById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET all sites for a customer
    @GetMapping("/customers/{customerId}/sites")
    public List<Site> getSitesByCustomer(
            @PathVariable Long customerId) {

        return siteService.getSitesByCustomerId(customerId);
    }

    // CREATE site for a customer
    @PostMapping("/customers/{customerId}/sites")
    public ResponseEntity<Site> createSite(
            @PathVariable Long customerId,
            @RequestBody Site site) {

        return siteService.createSite(customerId, site)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // UPDATE site
    @PutMapping("/sites/{id}")
    public ResponseEntity<Site> updateSite(
            @PathVariable Long id,
            @RequestBody Site updatedSite) {

        return siteService.updateSite(id, updatedSite)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE site
    @DeleteMapping("/sites/{id}")
    public ResponseEntity<Void> deleteSite(@PathVariable Long id) {

        if (!siteService.deleteSite(id)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}