package com.keystone;

import com.keystone.dto.WorkOrderRequest;
import com.keystone.dto.WorkOrderResponse;
import com.keystone.model.*;
import com.keystone.repository.*;
import com.keystone.service.WorkOrderPartService;
import com.keystone.service.WorkOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests transactional parts stock logic:
 * - Stock decrements atomically when part is used
 * - Stock cannot go negative (throws)
 * - Using parts on unassigned work order is rejected
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PartsStockTest {

    @Autowired WorkOrderService     workOrderService;
    @Autowired WorkOrderPartService workOrderPartService;
    @Autowired CustomerRepository   customerRepository;
    @Autowired SiteRepository       siteRepository;
    @Autowired UserRepository       userRepository;
    @Autowired PartRepository       partRepository;

    private Customer customer;
    private Site     site;
    private User     technician;
    private Part     part;

    @BeforeEach
    void setUp() {
        customer = customerRepository.save(new Customer("Parts Corp", "parts@test.com"));
        site     = siteRepository.save(new Site("Site A", "1 St", "City", "ST", "12345", customer));
        technician = userRepository.save(
                new User("Tech", "tech@parts.com", "hash", Role.TECHNICIAN));

        part = new Part();
        part.setName("Widget");
        part.setStockQuantity(10);
        part.setUnitPrice(500);
        part = partRepository.save(part);

        authenticateAs(technician, "TECHNICIAN");
    }

    @Test
    void addPart_decrementsStock() {
        Long workOrderId = assignedWorkOrder();
        workOrderPartService.addPart(workOrderId, part.getId(), 3);

        Part updated = partRepository.findById(part.getId()).orElseThrow();
        assertThat(updated.getStockQuantity()).isEqualTo(7);
    }

    @Test
    void addPart_stockCannotGoNegative() {
        Long workOrderId = assignedWorkOrder();
        assertThatThrownBy(() ->
                workOrderPartService.addPart(workOrderId, part.getId(), 999))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void addPart_toUnassignedWorkOrder_isRejected() {
        // Create but do NOT assign
        authenticateAs(technician, "MANAGER");
        WorkOrderResponse wo = createWorkOrder(null);
        authenticateAs(technician, "TECHNICIAN");

        assertThatThrownBy(() ->
                workOrderPartService.addPart(wo.id(), part.getId(), 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not assigned");
    }

    @Test
    void addPart_exactStock_succeeds() {
        Long workOrderId = assignedWorkOrder();
        workOrderPartService.addPart(workOrderId, part.getId(), 10);

        Part updated = partRepository.findById(part.getId()).orElseThrow();
        assertThat(updated.getStockQuantity()).isEqualTo(0);
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------

    private Long assignedWorkOrder() {
        authenticateAs(technician, "MANAGER");
        WorkOrderResponse wo = createWorkOrder(technician.getId());
        authenticateAs(technician, "TECHNICIAN");
        return wo.id();
    }

    private WorkOrderResponse createWorkOrder(Long assigneeId) {
        WorkOrderRequest req = new WorkOrderRequest(
                "Parts Test WO", "desc", Priority.HIGH,
                null, customer.getId(), site.getId(), assigneeId);
        return workOrderService.createWorkOrder(req);
    }

    private void authenticateAs(User user, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
