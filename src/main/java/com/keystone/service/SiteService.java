package com.keystone.service;

import com.keystone.model.Customer;
import com.keystone.model.Site;
import com.keystone.repository.CustomerRepository;
import com.keystone.repository.SiteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SiteService {

    private final SiteRepository siteRepository;
    private final CustomerRepository customerRepository;

    public SiteService(SiteRepository siteRepository,
                       CustomerRepository customerRepository) {
        this.siteRepository = siteRepository;
        this.customerRepository = customerRepository;
    }

    // Get all sites
    public List<Site> getAllSites() {
        return siteRepository.findAll();
    }

    // Get site by ID
    public Optional<Site> getSiteById(Long id) {
        return siteRepository.findById(id);
    }

    // Get all sites belonging to a customer
    public List<Site> getSitesByCustomerId(Long customerId) {
        return siteRepository.findByCustomerId(customerId);
    }

    // Create site for a customer
    public Optional<Site> createSite(Long customerId, Site site) {

        return customerRepository.findById(customerId)
                .map(customer -> {
                    site.setCustomer(customer);
                    return siteRepository.save(site);
                });
    }

    // Update site
    public Optional<Site> updateSite(Long id, Site updatedSite) {

        return siteRepository.findById(id)
                .map(existingSite -> {

                    existingSite.setName(updatedSite.getName());
                    existingSite.setAddress(updatedSite.getAddress());
                    existingSite.setCity(updatedSite.getCity());
                    existingSite.setState(updatedSite.getState());
                    existingSite.setPostalCode(updatedSite.getPostalCode());

                    return siteRepository.save(existingSite);
                });
    }

    // Delete site
    public boolean deleteSite(Long id) {

        if (!siteRepository.existsById(id)) {
            return false;
        }

        siteRepository.deleteById(id);
        return true;
    }
}