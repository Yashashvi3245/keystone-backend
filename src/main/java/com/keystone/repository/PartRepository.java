package com.keystone.repository;

import com.keystone.model.Part;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PartRepository extends JpaRepository<Part, Long> {

    Optional<Part> findByNameIgnoreCase(String name);
}