package com.keystone.repository;

import com.keystone.model.Site;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SiteRepository extends JpaRepository<Site, Long> {

    @Override
    @EntityGraph(attributePaths = {"customer"})
    List<Site> findAll();

    @Override
    @EntityGraph(attributePaths = {"customer"})
    Optional<Site> findById(Long id);

    @EntityGraph(attributePaths = {"customer"})
    List<Site> findByCustomerId(Long customerId);
}