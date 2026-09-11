package com.keystone;

import com.keystone.dto.WorkOrderRequest;
import com.keystone.dto.WorkOrderResponse;
import com.keystone.model.*;
import com.keystone.repository.*;
import com.keystone.service.WorkOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Verifies server-side data isolation:
 * - CUSTOMER only sees their own organisation's work orders
 * - TECHNICIAN pagination only returns their assigned work orders
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SecurityIsolationTest {

    @Autowired WorkOrderService   workOrderService;
    @Autowired CustomerRepository customerRepository;
    @Autowired SiteRepository     siteRepository;
    @Autowired UserRepository     userRepository;

    private Customer customer1, customer2;
    private Site     site1, site2;
    private User     manager;
    private User     tech1, tech2;
    private User     custUser1, custUser2;

    @BeforeEach
    void setUp() {
        customer1 = customerRepository.save(new Customer("Alpha Corp", "alpha@test.com"));
        customer2 = customerRepository.save(new Customer("Beta Corp",  "beta@test.com"));

        site1 = siteRepository.save(new Site("Alpha HQ", "1 St", "A City", "AL", "10001", customer1));
        site2 = siteRepository.save(new Site("Beta HQ",  "2 St", "B City", "BL", "20002", customer2));

        manager = userRepository.save(new User("Mgr", "mgr@iso.com", "hash", Role.MANAGER));

        tech1 = userRepository.save(new User("Tech1", "tech1@iso.com", "hash", Role.TECHNICIAN));
        tech2 = userRepository.save(new User("Tech2", "tech2@iso.com", "hash", Role.TECHNICIAN));

        custUser1 = new User("CustUser1", "cust1@iso.com", "hash", Role.CUSTOMER);
        custUser1.setCustomer(customer1);
        custUser1 = userRepository.save(custUser1);

        custUser2 = new User("CustUser2", "cust2@iso.com", "hash", Role.CUSTOMER);
        custUser2.setCustomer(customer2);
        custUser2 = userRepository.save(custUser2);

        // Create work orders: one for each customer
        authenticateAs(manager, "MANAGER");
        WorkOrderRequest req1 = new WorkOrderRequest(
                "Alpha WO", "desc", Priority.MEDIUM, null,
                customer1.getId(), site1.getId(), null);
        WorkOrderRequest req2 = new WorkOrderRequest(
                "Beta WO", "desc", Priority.LOW, null,
                customer2.getId(), site2.getId(), null);
        workOrderService.createWorkOrder(req1);
        workOrderService.createWorkOrder(req2);
    }

    // -------------------------------------------------------
    // CUSTOMER isolation: each customer sees only their own WOs
    // -------------------------------------------------------

    @Test
    void customer1_seesOnlyOwnWorkOrders() {
        Page<WorkOrderResponse> page = workOrderService.searchCustomerWorkOrders(
                customer1.getId(), null, null, null, PageRequest.of(0, 20));

        assertThat(page.getContent())
                .allMatch(wo -> wo.customerId().equals(customer1.getId()))
                .isNotEmpty();
    }

    @Test
    void customer2_seesOnlyOwnWorkOrders() {
        Page<WorkOrderResponse> page = workOrderService.searchCustomerWorkOrders(
                customer2.getId(), null, null, null, PageRequest.of(0, 20));

        assertThat(page.getContent())
                .allMatch(wo -> wo.customerId().equals(customer2.getId()))
                .isNotEmpty();
    }

    @Test
    void customer1_cannotSeeCustomer2_workOrders() {
        Page<WorkOrderResponse> page = workOrderService.searchCustomerWorkOrders(
                customer1.getId(), null, null, null, PageRequest.of(0, 20));

        assertThat(page.getContent())
                .noneMatch(wo -> wo.customerId().equals(customer2.getId()));
    }

    // -------------------------------------------------------
    // TECHNICIAN isolation: each technician sees only assigned WOs
    // -------------------------------------------------------

    @Test
    void technician_seesOnlyAssignedWorkOrders() {
        // Assign the Alpha WO to tech1
        authenticateAs(manager, "MANAGER");
        Page<WorkOrderResponse> allWOs = workOrderService.searchWorkOrders(
                null, null, null, PageRequest.of(0, 20));
        Long alphaId = allWOs.getContent().stream()
                .filter(wo -> wo.title().equals("Alpha WO"))
                .findFirst().orElseThrow().id();

        workOrderService.assignWorkOrder(alphaId, tech1.getId());

        // Tech1 should see 1 work order
        Page<WorkOrderResponse> tech1WOs = workOrderService.searchTechnicianWorkOrders(
                tech1.getId(), null, null, null, PageRequest.of(0, 20));
        assertThat(tech1WOs.getContent()).hasSize(1);
        assertThat(tech1WOs.getContent().get(0).assigneeId()).isEqualTo(tech1.getId());

        // Tech2 should see 0 work orders
        Page<WorkOrderResponse> tech2WOs = workOrderService.searchTechnicianWorkOrders(
                tech2.getId(), null, null, null, PageRequest.of(0, 20));
        assertThat(tech2WOs.getContent()).isEmpty();
    }

    @Test
    void technician_paginationMetadataIsCorrect() {
        // Assign both WOs to tech1
        authenticateAs(manager, "MANAGER");
        Page<WorkOrderResponse> allWOs = workOrderService.searchWorkOrders(
                null, null, null, PageRequest.of(0, 20));

        allWOs.getContent().forEach(wo ->
                workOrderService.assignWorkOrder(wo.id(), tech1.getId()));

        // Request page size 1 — metadata must reflect only tech1's 2 WOs
        Page<WorkOrderResponse> page = workOrderService.searchTechnicianWorkOrders(
                tech1.getId(), null, null, null, PageRequest.of(0, 1));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(1);
    }

    // -------------------------------------------------------
    // Helper
    // -------------------------------------------------------

    private void authenticateAs(User user, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
